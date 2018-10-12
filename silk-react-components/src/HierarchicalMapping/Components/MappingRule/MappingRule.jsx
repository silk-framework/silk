/*
 An individual Mapping Rule Line
 */

import {DragDropContext, Droppable, Draggable} from 'react-beautiful-dnd';
import React from 'react';
import _ from 'lodash';

import {
    Button,
    ContextMenu,
    MenuItem,
    ConfirmationDialog,
    Spinner,
    DisruptiveButton,
    DismissiveButton,
} from '@eccenca/gui-elements';
import UseMessageBus from '../../UseMessageBusMixin';
import hierarchicalMappingChannel from '../../store';
import RuleValueEdit from './ValueMappingRule';
import RuleObjectEdit from './ObjectMappingRule';
import {RuleTypes, SourcePath, ThingName, ThingIcon} from './SharedComponents';
import {isObjectMappingRule, MAPPING_RULE_TYPE_OBJECT} from '../../helpers';
import Navigation from '../../Mixins/Navigation';
import className from 'classnames';

const MappingRule = React.createClass({
    mixins: [UseMessageBus, Navigation],

    // define property types
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        type: React.PropTypes.string, // mapping type
        typeRules: React.PropTypes.array,
        mappingTarget: React.PropTypes.object,
        // sourcePath: React.PropTypes.string, // it can be array or single string ...
        targetProperty: React.PropTypes.string,
        pattern: React.PropTypes.string,
        uriRule: React.PropTypes.object,
        parentId: React.PropTypes.string,
        pos: React.PropTypes.number.isRequired,
        count: React.PropTypes.number.isRequired,
        // provided,
        // snapshot,
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
        this.subscribe(
            hierarchicalMappingChannel.subject('rulesView.toggle'),
            ({expanded, id}) => {
                // only trigger state / render change if necessary
                if (
                    expanded !== this.state.expanded &&
                    this.props.type !== MAPPING_RULE_TYPE_OBJECT &&
                    (id === true || id === this.props.id)
                ) {
                    this.setState({expanded});
                }
            }
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.change'),
            this.onOpenEdit
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.close'),
            this.onCloseEdit
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.discardAll'),
            this.discardAll
        );
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

    // show / hide additional row details
    handleToggleExpand() {
        if (this.state.editing) {
            this.setState({
                askForDiscard: true,
            });
        } else this.setState({expanded: !this.state.expanded});
    },
    discardAll() {
        this.setState({
            editing: false,
        });
    },
    handleDiscardChanges() {
        this.setState({
            expanded: !this.state.expanded,
            askForDiscard: false,
        });
        hierarchicalMappingChannel
            .subject('ruleView.unchanged')
            .onNext({id: this.props.id});
    },
    handleCancelDiscard() {
        this.setState({
            askForDiscard: false,
        });
    },

    handleMoveElement({toPos, fromPos, parentId, id}, event) {
        if (fromPos === toPos) {
            return;
        }
        hierarchicalMappingChannel
            .subject('request.rule.orderRule')
            .onNext({toPos, fromPos, reload: true});
    },
    // template rendering
    render() {
        const getItemStyle = (draggableStyle, isDragging) => ({
            // some basic styles to make the items look a bit nicer
            userSelect: this.state.expanded ? 'inherit' : 'none',
            background: isDragging ? '#cbe7fb' : 'transparent',
            boxShadow: isDragging ? '0px 3px 4px silver' : 'inherit',
            opacity: isDragging ? '1' : '1',
            zIndex: isDragging ? '1' : 'inherit',
            // styles we need to apply on draggables
            ...draggableStyle,
        });

        const {
            id,
            type,
            parentId,
            sourcePath,
            sourcePaths,
            mappingTarget,
            rules,
            pos,
            count,
            metadata,
            errorInfo,
        } = this.props;

        const label = _.get(metadata, 'label', '');
        const loading = this.state.loading ? <Spinner /> : false;
        const discardView = this.state.askForDiscard ? (
            <ConfirmationDialog
                active
                modal
                title="Discard changes?"
                confirmButton={
                    <DisruptiveButton
                        disabled={false}
                        onClick={this.handleDiscardChanges}>
                        Discard
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton onClick={this.handleCancelDiscard}>
                        Cancel
                    </DismissiveButton>
                }>
                <p>You currently have unsaved changes.</p>
            </ConfirmationDialog>
        ) : (
            false
        );

        const mainAction = event => {
            if (type === MAPPING_RULE_TYPE_OBJECT) {
                this.handleNavigate(this.props.id, this.props.parentId, event);
            } else {
                this.handleToggleExpand({force: true});
            }
            event.stopPropagation();
        };
        const action = (
            <Button
                iconName={
                    type === MAPPING_RULE_TYPE_OBJECT
                        ? 'arrow_nextpage'
                        : this.state.expanded ? 'expand_less' : 'expand_more'
                }
                tooltip={
                    type === MAPPING_RULE_TYPE_OBJECT
                        ? 'Navigate to'
                        : undefined
                }
                onClick={mainAction}
            />
        );

        // TODO: enable real API structure

        const shortView = [
            <div
                key={'hl1'}
                className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__ruleitem-info-targetstructure">
                <ThingIcon
                    type={type}
                    status={_.get(this.props, 'status[0].type', false)}
                    message={_.get(this.props, 'status[0].message', false)}
                />
                {label || <ThingName id={mappingTarget.uri} />}
            </div>,
            <div
                key={'sl3'}
                className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-editinfo">
                <span className="hide-in-table">DataType:</span>{' '}
                <RuleTypes
                    rule={{
                        type,
                        mappingTarget,
                        rules,
                    }}
                />
            </div>,
            <div
                key={'sl2'}
                className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-sourcestructure">
                <span className="hide-in-table">from</span>{' '}
                <SourcePath
                    rule={{
                        type,
                        sourcePath: sourcePath || sourcePaths,
                    }}
                />
            </div>,
        ];

        const expandedView = this.state.expanded ? (
            isObjectMappingRule(type) ? (
                <RuleObjectEdit
                    {...this.props}
                    handleToggleExpand={this.handleToggleExpand}
                    type={type}
                    parentId={parentId}
                    edit={false}
                />
            ) : (
                <RuleValueEdit
                    {...this.props}
                    handleToggleExpand={this.handleToggleExpand}
                    type={type}
                    parentId={parentId}
                    edit={false}
                />
            )
        ) : (
            false
        );

        const reorderHandleButton = !this.state.expanded ? (
            <div className="ecc-silk-mapping__ruleitem-reorderhandler">
                <ContextMenu iconName="reorder" align="left" valign="top">
                    <MenuItem
                        onClick={this.handleMoveElement.bind(null, {
                            parentId,
                            fromPos: pos,
                            toPos: 0,
                            id,
                        })}>
                        Move to top
                    </MenuItem>
                    <MenuItem
                        onClick={this.handleMoveElement.bind(null, {
                            parentId,
                            fromPos: pos,
                            toPos: Math.max(0, pos - 1),
                            id,
                        })}>
                        Move up
                    </MenuItem>
                    <MenuItem
                        onClick={this.handleMoveElement.bind(null, {
                            parentId,
                            fromPos: pos,
                            toPos: Math.min(pos + 1, count - 1),
                            id,
                        })}>
                        Move down
                    </MenuItem>
                    <MenuItem
                        onClick={this.handleMoveElement.bind(null, {
                            parentId,
                            fromPos: pos,
                            toPos: count - 1,
                            id,
                        })}>
                        Move to bottom
                    </MenuItem>
                </ContextMenu>
            </div>
        ) : (
            false
        );

        return (
            <Draggable
                isDragDisabled={this.state.expanded}
                style={{width: '15'}}
                key={id}
                draggableId={id}>
                {(provided, snapshot) => (
                    <li
                        className={className('ecc-silk-mapping__ruleitem', {
                            'ecc-silk-mapping__ruleitem--object':
                                type === 'object',
                            'ecc-silk-mapping__ruleitem--literal':
                                type !== 'object',
                            'ecc-silk-mapping__ruleitem--defect': errorInfo,
                        })}>
                        <div
                            className={'ecc-silk-mapping__ruleitem--dnd'}
                            ref={provided.innerRef}
                            style={getItemStyle(
                                provided.draggableStyle,
                                snapshot.isDragging
                            )}
                            {...provided.dragHandleProps}>
                            {discardView}
                            {loading}
                            <div
                                className={className(
                                    'ecc-silk-mapping__ruleitem-summary',
                                    {
                                        'ecc-silk-mapping__ruleitem-summary--expanded': this
                                            .state.expanded,
                                    }
                                )}>
                                {reorderHandleButton}
                                <div
                                    className={'mdl-list__item clickable'}
                                    onClick={mainAction}>
                                    <div
                                        className={
                                            'mdl-list__item-primary-content'
                                        }>
                                        {shortView}
                                    </div>
                                    <div
                                        className="mdl-list__item-secondary-content"
                                        key="action">
                                        {action}
                                    </div>
                                </div>
                            </div>
                            {this.state.expanded ? (
                                <div className="ecc-silk-mapping__ruleitem-expanded">
                                    {expandedView}
                                </div>
                            ) : (
                                false
                            )}
                        </div>
                        {provided.placeholder}
                    </li>
                )}
            </Draggable>
        );
    },
});

export default MappingRule;
