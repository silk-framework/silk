/*
 An individual Mapping Rule Line
 */

import { Draggable } from 'react-beautiful-dnd';
import React from 'react';
import _ from 'lodash';

import {
    Button,
    ConfirmationDialog,
    ContextMenu,
    DismissiveButton,
    DisruptiveButton,
    MenuItem,
    ScrollingHOC,
    Spinner,
} from '@eccenca/gui-elements';
import RuleValueEdit from './ValueMappingRule';
import RuleObjectEdit from './ObjectMappingRule';
import { SourcePath} from '../../Components/SourcePath';
import RuleTypes from '../../elements/RuleTypes';
import { MAPPING_RULE_TYPE_OBJECT } from '../../utils/constants';
import className from 'classnames';
import { isObjectMappingRule, MESSAGES } from '../../utils/constants';
import EventEmitter from '../../utils/EventEmitter';
import PropTypes from 'prop-types';
import { getRuleLabel } from '../../utils/getRuleLabel';
import { ThingIcon } from '../../Components/ThingIcon';
import { URI } from 'ecc-utils';

export class MappingRule extends React.Component {
    // define property types
    static propTypes = {
        comment: PropTypes.string,
        id: PropTypes.string,
        type: PropTypes.string, // mapping type
        typeRules: PropTypes.array,
        mappingTarget: PropTypes.object,
        // sourcePath: PropTypes.string, // it can be array or single string ...
        targetProperty: PropTypes.string,
        pattern: PropTypes.string,
        uriRule: PropTypes.object,
        parentId: PropTypes.string,
        pos: PropTypes.number.isRequired,
        count: PropTypes.number.isRequired,
        onClickedRemove: PropTypes.func,
        // provided,
        // snapshot,
    };

    // initilize state
    constructor(props) {
        super(props);
        const pastedId = sessionStorage.getItem('pastedId');
        const isPasted = (pastedId !== null) && (pastedId === this.props.id);
        if (isPasted) {
            !sessionStorage.removeItem('pastedId');
        }
        let expanded = isPasted;
    
        const uriTemplate = new URI(window.location.href);
        if (uriTemplate.segment(-2) === 'rule') {
            expanded = uriTemplate.segment(-1) === this.props.id
        }
        
        this.state = {
            isPasted,
            expanded,
            editing: false,
            loading: false,
        };
        this.handleToggleRule = this.handleToggleRule.bind(this);
        this.onOpenEdit = this.onOpenEdit.bind(this);
        this.onCloseEdit = this.onCloseEdit.bind(this);
        this.handleToggleExpand = this.handleToggleExpand.bind(this);
        this.discardAll = this.discardAll.bind(this);
        this.handleDiscardChanges = this.handleDiscardChanges.bind(this);
        this.handleMoveElement = this.handleMoveElement.bind(this);
        this.handleNavigate = this.handleNavigate.bind(this);
    }

    componentDidMount() {
        // listen for event to expand / collapse mapping rule
        EventEmitter.on(MESSAGES.RULE_VIEW.TOGGLE, this.handleToggleRule);
        EventEmitter.on(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.CLOSE, this.onCloseEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.DISCARD_ALL, this.discardAll);
        if (this.state.isPasted) {
            this.props.scrollIntoView();
        }
    }

    componentWillUnmount() {
        EventEmitter.off(MESSAGES.RULE_VIEW.TOGGLE, this.handleToggleRule);
        EventEmitter.off(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.CLOSE, this.onCloseEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.DISCARD_ALL, this.discardAll);
    }

    handleToggleRule({ expanded, id }) {
        // only trigger state / render change if necessary
        if (
            expanded !== this.state.expanded &&
            this.props.type !== MAPPING_RULE_TYPE_OBJECT &&
            (id === true || id === this.props.id)
        ) {
            this.setState({ expanded });
        }
    };

    onOpenEdit(obj) {
        if (_.isEqual(this.props.id, obj.id)) {
            this.setState({
                editing: true,
            });
        }
    };

    onCloseEdit(obj) {
        if (_.isEqual(this.props.id, obj.id)) {
            this.setState({
                editing: false,
            });
        }
    };

    // show / hide additional row details
    handleToggleExpand () {
        if (this.state.editing) {
            this.props.onAskDiscardChanges(true);
        } else {
            this.setState({ expanded: !this.state.expanded });
        }
    };

    discardAll () {
        this.setState({
            editing: false,
        });
    };

    handleDiscardChanges() {
        this.setState({
            expanded: !this.state.expanded,
        });
        this.props.onAskDiscardChanges(false);
        EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id: this.props.id });
    };

    handleMoveElement({ toPos, fromPos }) {
        if (fromPos === toPos) {
            return;
        }
        EventEmitter.emit(MESSAGES.RULE.REQUEST_ORDER, { toPos, fromPos, reload: true });
    };

    // jumps to selected rule as new center of view
    handleNavigate(id, parent, event) {
        this.props.onRuleIdChange({ newRuleId: id, parentId: parent });
        event.stopPropagation();
    };

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

        const mainAction = event => {
            if (type === MAPPING_RULE_TYPE_OBJECT) {
                this.handleNavigate(this.props.id, this.props.parentId, event);
            } else {
                this.handleToggleExpand({ force: true });
            }
            event.stopPropagation();
        };
        const action = (
            <Button
                className={`silk${this.props.id}`}
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

        const ruleLabelData = getRuleLabel({ label, uri: mappingTarget.uri });

        // TODO: enable real API structure
        const shortView = [
            <div
                key="hl1"
                className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__ruleitem-info-targetstructure"
            >
                <ThingIcon
                    type={type}
                    status={_.get(this.props, 'status[0].type', false)}
                    message={_.get(this.props, 'status[0].message', false)}
                />
                <div className="ecc-silk-mapping__ruleitem-label">
                    {ruleLabelData.displayLabel}
                </div>
                {ruleLabelData.uri && <div className="ecc-silk-mapping__ruleitem-extraline ecc-silk-mapping__ruleitem-url">{ruleLabelData.uri}</div>}
            </div>,
            <div
                key="sl3"
                className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-editinfo"
            >
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
                key="sl2"
                className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-sourcestructure"
            >
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
                    ruleData={{
                        ...this.props,
                        type
                    }}
                    handleToggleExpand={this.handleToggleExpand}
                    parentId={parentId}
                    edit={false}
                    handleCopy={this.props.handleCopy}
                    handleClone={this.props.handleClone}
                    onClickedRemove={this.props.onClickedRemove}
                />
            ) : (
                <RuleValueEdit
                    {...this.props}
                    handleToggleExpand={this.handleToggleExpand}
                    type={type}
                    parentId={parentId}
                    edit={false}
                    handleCopy={this.props.handleCopy}
                    handleClone={this.props.handleClone}
                    onClickedRemove={this.props.onClickedRemove}
                />
            )
        ) : (
            false
        );
        const reorderHandleButton = !this.state.expanded ? (
            <div className="ecc-silk-mapping__ruleitem-reorderhandler" key={id}>
                <ContextMenu iconName="reorder" align="left" valign="top">
                    <MenuItem
                        onClick={() => this.handleMoveElement({
                            parentId,
                            fromPos: pos,
                            toPos: 0,
                            id,
                        })}
                    >
                        Move to top
                    </MenuItem>
                    <MenuItem
                        onClick={() => this.handleMoveElement({
                            parentId,
                            fromPos: pos,
                            toPos: Math.max(0, pos - 1),
                            id,
                        })}
                    >
                        Move up
                    </MenuItem>
                    <MenuItem
                        onClick={() => this.handleMoveElement({
                            parentId,
                            fromPos: pos,
                            toPos: Math.min(pos + 1, count - 1),
                            id,
                        })}
                    >
                        Move down
                    </MenuItem>
                    <MenuItem
                        onClick={() => this.handleMoveElement({
                            parentId,
                            fromPos: pos,
                            toPos: count - 1,
                            id,
                        })}
                    >
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
                style={{ width: '15' }}
                key={id}
                draggableId={id}
            >
                {(provided, snapshot) => (
                    <li
                        className={className('ecc-silk-mapping__ruleitem', {
                            'ecc-silk-mapping__ruleitem--object': type === 'object',
                            'ecc-silk-mapping__ruleitem--literal': type !== 'object',
                            'ecc-silk-mapping__ruleitem--defect': errorInfo,
                            'mdl-layout_item--background-flash': this.state.isPasted,
                        })}
                    >
                        <div
                            className="ecc-silk-mapping__ruleitem--dnd"
                            ref={provided.innerRef}
                            style={getItemStyle(
                                provided.draggableStyle,
                                snapshot.isDragging
                            )}
                            {...provided.dragHandleProps}
                        >
                            {loading}
                            <div
                                className={className(
                                    'ecc-silk-mapping__ruleitem-summary',
                                    {
                                        'ecc-silk-mapping__ruleitem-summary--expanded': this
                                            .state.expanded,
                                    }
                                )}
                            >
                                {reorderHandleButton}
                                <div
                                    className="mdl-list__item clickable"
                                    onClick={mainAction}
                                >
                                    <div
                                        className="mdl-list__item-primary-content"
                                    >
                                        {shortView}
                                    </div>
                                    <div
                                        className="mdl-list__item-secondary-content"
                                        key="action"
                                    >
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
    }
}

export default ScrollingHOC(MappingRule);
