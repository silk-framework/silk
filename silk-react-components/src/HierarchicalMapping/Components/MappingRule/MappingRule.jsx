/*
 An individual Mapping Rule Line
 */

import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {Button, ContextMenu, MenuItem, ConfirmationDialog, Spinner, DisruptiveButton, DismissiveButton} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../../store';
import RuleValueEdit from './ValueMappingRule';
import RuleObjectEdit from './ObjectMappingRule';
import _ from 'lodash';
import {
    RuleTypes,
    SourcePath,
    ThingName,
    ThingIcon,
} from './SharedComponents';

const MappingRule = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        type: React.PropTypes.string, // mapping type
        typeRules: React.PropTypes.array,
        mappingTarget: React.PropTypes.object,
        //sourcePath: React.PropTypes.string, // it can be array or single string ...
        targetProperty: React.PropTypes.string,
        pattern: React.PropTypes.string,
        uriRule: React.PropTypes.object,
        parent: React.PropTypes.string,
        pos: React.PropTypes.number.isRequired,
        count: React.PropTypes.number.isRequired,

    },

    // initilize state
    getInitialState() {
        return {
            expanded: false,
            editing: false,
            askForDiscard: false,
            loading: false,
        };
    },
    componentDidMount() {
        // listen for event to expand / collapse mapping rule
        this.subscribe(hierarchicalMappingChannel.subject('rulesView.toggle'), ({expanded}) => {
            // only trigger state / render change if necessary
            if (expanded !== this.state.expanded && this.props.type !== 'object') {
                this.setState({expanded});
            }
        });
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.change'), this.onOpenEdit);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.close'), this.onCloseEdit);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.discardAll'), this.discardAll);
    },
    onOpenEdit(obj) {
        if (_.isEqual(this.props.id, obj.id)) {
            this.setState({
                editing: true,
            });
        }
    },
    onCloseEdit(obj) {
        if (_.isEqual(this.props.id, obj.id)) {
            this.setState({
                editing: false,
            });
        }
    },
    // jumps to selected rule as new center of view
    handleNavigate() {
        hierarchicalMappingChannel.subject('ruleId.change').onNext({newRuleId: this.props.id, parent: this.props.parent});
    },
    // show / hide additional row details
    handleToggleExpand() {
        if (this.state.editing) {
            this.setState({
                askForDiscard: true,
            });
        }
        else this.setState({expanded: !this.state.expanded});
    },
    discardAll() {
        this.setState({
            editing: false,
        });
    },
    handleDiscardChanges(){
        this.setState({
            expanded: !this.state.expanded,
            askForDiscard: false,
        });
        hierarchicalMappingChannel.subject('ruleView.unchanged').onNext({id: this.props.id});
    },
    handleCancelDiscard() {
        this.setState({
            askForDiscard: false,
        })
    },

    handleMoveElement(id, pos, parent, event){
        this.setState({
            loading: true,
        });
        console.log(event, id, pos, parent)
        event.stopPropagation();
        hierarchicalMappingChannel.request({topic: 'rule.orderRule', data: {id, pos, parent}})
            .subscribe(
                () => {
                    // FIXME: let know the user which element is gone!
                    this.setState({
                        loading: false,
                    });
                },
                (err) => {
                    // FIXME: let know the user what have happened!
                    this.setState({
                        loading: false,
                    });
                }
            );
    },
    // template rendering
    render () {
        const {
            id,
            type,
            parent,
            sourcePath,
            mappingTarget,
            rules,
            pos,
            count,
        } = this.props;

        const loading = this.state.loading ? <Spinner/> : false;
        const discardView = this.state.askForDiscard
            ? <ConfirmationDialog
                active={true}
                title="Discard changes"
                confirmButton={
                    <DisruptiveButton disabled={false} onClick={this.handleDiscardChanges}>
                        Continue
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton onClick={this.handleCancelDiscard}>
                        Cancel
                    </DismissiveButton>
                }>
                <p>By clicking on CONTINUE, all unsaved changes from the current formular will be destroy.</p>
                <p>Are you sure you want to close the form?</p>
            </ConfirmationDialog>
            : false;

        const mainAction = (event) => {
            if (type === 'object') {
                this.handleNavigate();
            } else {
                this.handleToggleExpand({force: true});
            }
            event.stopPropagation();
        };
        const action = (
            <Button
                iconName={type === 'object' ? 'arrow_nextpage' : (this.state.expanded ? 'expand_less' : 'expand_more')}
                tooltip={type === 'object' ? 'Navigate to' : undefined}
                onClick={mainAction}
            />
        );

        // TODO: enable real API structure
        const errorInfo = (_.get(this.props, 'status[0].type', false) == 'error') ? _.get(this.props, 'status[0].message', false) : false;

        const shortView = [
            <div key={'hl1'} className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__ruleitem-info-targetstructure">
                <ThingIcon
                    type={type}
                    tooltip={type + ' mapping'}
                    status={_.get(this.props, 'status[0].type', false)}
                    message={_.get(this.props, 'status[0].message', false)}
                />
                <ThingName id={mappingTarget.uri} />
            </div>,
            /*
                <div key={'sl1'} className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-mappingtype">
                    {type} mapping
                </div>,
            */
            <div key={'sl2'} className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-sourcestructure">
                <span className="hide-in-table">from</span>
                <SourcePath
                    rule={
                        {
                            type,
                            sourcePath,
                        }
                    }
                />
            </div>,
            <div key={'sl3'} className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-editinfo">
                <span className="hide-in-table">DataType:</span>
                <RuleTypes
                    rule={
                        {
                            type,
                            mappingTarget,
                            rules,
                        }
                    }
                />
            </div>,
        ];

        const expandedView = this.state.expanded ? (
            (type === 'object' || type === 'root') ? (
                <RuleObjectEdit
                    {...this.props}
                    handleToggleExpand={this.handleToggleExpand}
                    type={type}
                    parent={parent}
                    edit={false}
                />
            ) : (
                <RuleValueEdit
                    {...this.props}
                    handleToggleExpand={this.handleToggleExpand}
                    type={type}
                    parent={parent}
                    edit={false}
                />
            )
        ) : false;

        const reorderHandleButton = !this.state.expanded && __DEBUG__ ? (
            <div
                className="ecc-silk-mapping__ruleitem-reorderhandler"
            >
                <ContextMenu
                    iconName="reorder"
                    align='left'
                    valign='top'
                >
                    <MenuItem
                        onClick={this.handleMoveElement.bind(null, id, 0, parent)}
                    >
                        Move to top
                    </MenuItem>
                    <MenuItem
                        onClick={this.handleMoveElement.bind(null, id, Math.max(0, pos -1), parent)}
                    >
                        Move up
                    </MenuItem>
                    <MenuItem
                        onClick={this.handleMoveElement.bind(null, id, Math.min(pos + 1, count-1), parent)}
                    >
                        Move down
                    </MenuItem>
                    <MenuItem
                        onClick={this.handleMoveElement.bind(null, id, count - 1, parent)}
                    >
                        Move to bottom
                    </MenuItem>
                </ContextMenu>
            </div>
        ) : false;

        return (
            <li className={
                    "ecc-silk-mapping__ruleitem mdl-list__item " +
                    (type === 'object' ? 'ecc-silk-mapping__ruleitem--object' : 'ecc-silk-mapping__ruleitem--literal') +
                    (this.state.expanded ? ' ecc-silk-mapping__ruleitem--expanded' : ' ecc-silk-mapping__ruleitem--summary') +
                    (errorInfo ? ' ecc-silk-mapping__ruleitem--defect' : '')
                }
            >
                {discardView}
                {loading}
                {reorderHandleButton}
                <div
                    className={
                        'mdl-list__item-primary-content ecc-silk-mapping__ruleitem-content' +
                        (this.state.expanded ? '' : ' clickable')
                    }
                    onClick={this.state.expanded ? null : mainAction}
                >
                    {this.state.expanded ? expandedView : shortView}
                </div>
                <div className="mdl-list__item-secondary-content" key="action">
                    {action}
                </div>
            </li>
        );
    },
});

export default MappingRule;
