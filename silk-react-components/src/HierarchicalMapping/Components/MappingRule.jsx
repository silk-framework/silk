/*
 An individual Mapping Rule Line
 */

import React from 'react';
import UseMessageBus from '../UseMessageBusMixin';
import {Button, ContextMenu, MenuItem} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../store';
import RuleValueEdit from './RuleValueEditView';
import RuleObjectEdit from './RuleObjectEditView';

const MappingRule = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        type: React.PropTypes.string, // mapping type
        typeRules: React.PropTypes.array,
        mappingTarget: React.PropTypes.object,
        sourcePath: React.PropTypes.string,
        targetProperty: React.PropTypes.string,
        pattern: React.PropTypes.string,
        uriRule: React.PropTypes.object,
        parent: React.PropTypes.string,
        pos: React.PropTypes.number.isRequired,
        count: React.PropTypes.number.isRequired,
        expanded: React.PropTypes.bool.isRequired,
    },

    // initilize state
    getInitialState() {
        // listen for event to expand / collapse mapping rule
        this.subscribe(hierarchicalMappingChannel.subject('rulesView.toggle'), ({expanded}) => {
            // only trigger state / render change if necessary
            if (expanded !== this.state.expanded && !this.props.parent && this.props.type !== 'object') {
                this.setState({expanded});
            }
        });
        // listen to rule edit event

        return {
            expanded: this.props.expanded,
        };
    },
    // jumps to selected rule as new center of view
    handleNavigate() {
        hierarchicalMappingChannel.subject('ruleId.change').onNext({newRuleId: this.props.id, parent: this.props.parent});
    },
    // show / hide additional row details
    handleToggleExpand() {
        hierarchicalMappingChannel.subject('ruleView.toggle').onNext({id: this.props.id, expanded: !this.state.expanded});
        this.setState({expanded: !this.state.expanded});
    },
    handleMoveElement(id, pos, parent){
        return (event) => {
            event.stopPropagation();
            hierarchicalMappingChannel.request({topic: 'rule.orderRule', data: {id, pos, parent}})
                .subscribe(
                    () => {
                        // FIXME: let know the user which element is gone!

                    },
                    (err) => {
                        // FIXME: let know the user what have happened!

                    }
                );
        }
    },
    // template rendering
    render () {
        const {
            id,
            type,
            parent,
            sourcePath = false,
            mappingTarget,
            pos,
            count,
        } = this.props;

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

        const shortView = [
            <div key={'hl1'} className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__ruleitem-info-targetstructure">
                {mappingTarget.uri} {/* TODO: should be normalized and easy readable */}
            </div>,
            <div key={'sl1'} className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-mappingtype">
                {type} mapping
            </div>,
            <div key={'sl2'} className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-sourcestructure">
                <span className="hide-in-table">from</span> {sourcePath ? sourcePath : '(todo: complex overview)'}
            </div>,
            <div key={'sl3'} className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-editinfo">
                <span className="hide-in-table">by</span> (todo: author, date)
            </div>,
        ];

        const expandedView = this.state.expanded ? (
            (type === 'object' || type === 'root') ? (
                <RuleObjectEdit
                    {...this.props}
                    handleToggleExpand={this.handleToggleExpand}
                    type={type}
                    edit={false}
                />
            ) : (
                <RuleValueEdit
                    {...this.props}
                    handleToggleExpand={this.handleToggleExpand}
                    type={type}
                    edit={false}
                />
            )
        ) : false;

        const reorderHandleButton = !this.state.expanded ? (
            <div
                className="ecc-silk-mapping__ruleitem-reorderhandler"
            >
                <ContextMenu
                    iconName="reorder"
                    align='left'
                    valign='top'
                >
                    <MenuItem
                        onClick={this.handleMoveElement(id, 0, parent)}
                    >
                        Move to top
                    </MenuItem>
                    <MenuItem
                        onClick={this.handleMoveElement(id, Math.max(0, pos -1), parent)}
                    >
                        Move up
                    </MenuItem>
                    <MenuItem
                        onClick={this.handleMoveElement(id, Math.min(pos + 1, count-1), parent)}
                    >
                        Move down
                    </MenuItem>
                    <MenuItem
                        onClick={this.handleMoveElement(id, count - 1, parent)}
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
                    (this.state.expanded ? '' : ' clickable')
                }
                onClick={this.state.expanded ? null : mainAction}
                title={
                    this.state.expanded ? '' : 'Click to expand'
                }
            >
                {reorderHandleButton}
                <div
                    className={
                        'mdl-list__item-primary-content ecc-silk-mapping__ruleitem-content'
                    }
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
