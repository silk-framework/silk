/*
 An individual Mapping Rule Line
 */

import React from 'react';
import _ from 'lodash';

import {
    Button,
    ConfirmationDialog,
    ContextMenu,
    DismissiveButton,
    DisruptiveButton,
    MenuItem,
    Spinner,
} from '@eccenca/gui-elements';
import ValueMappingRule from './ValueRule/ValueRule';
import ObjectRule from './ObjectRule/ObjectRule';
import { isObjectRule, isRootOrObjectRule, MAPPING_RULE_TYPE_OBJECT, MESSAGES } from '../../utils/constants';
import className from 'classnames';
import EventEmitter from '../../utils/EventEmitter';
import PropTypes from 'prop-types';
import MappingRuleRow from './MappingRuleRow';
import NavigateButton from '../../elements/buttons/NavigateButton';
import ExpandButton from '../../elements/buttons/ExpandButton';

export class MappingRule extends React.Component {
    // define property types
    static propTypes = {
        comment: PropTypes.string,
        id: PropTypes.string,
        type: PropTypes.string, // mapping type
        typeRules: PropTypes.array,
        mappingTarget: PropTypes.object,
        targetProperty: PropTypes.string,
        pattern: PropTypes.string,
        uriRule: PropTypes.object,
        parentId: PropTypes.string,
        pos: PropTypes.number.isRequired,
        count: PropTypes.number.isRequired,
        onClickedRemove: PropTypes.func,
        onOrderRules: PropTypes.func.isRequired,
        onExpand: PropTypes.func,
        // provided,
        // snapshot,
    };
    
    // initilize state
    constructor(props) {
        super(props);
        
        this.state = {
            editing: false,
            loading: false,
        };
        this.handleToggleRule = this.handleToggleRule.bind(this);
        this.onOpenEdit = this.onOpenEdit.bind(this);
        this.onCloseEdit = this.onCloseEdit.bind(this);
        this.handleToggleExpand = this.handleToggleExpand.bind(this);
        this.discardAll = this.discardAll.bind(this);
        this.handleNavigate = this.handleNavigate.bind(this);
    }
    
    componentDidMount() {
        // listen for event to expand / collapse mapping rule
        EventEmitter.on(MESSAGES.RULE_VIEW.TOGGLE, this.handleToggleRule);
        EventEmitter.on(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.CLOSE, this.onCloseEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.DISCARD_ALL, this.discardAll);
    }
    
    componentWillUnmount() {
        EventEmitter.off(MESSAGES.RULE_VIEW.TOGGLE, this.handleToggleRule);
        EventEmitter.off(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.CLOSE, this.onCloseEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.DISCARD_ALL, this.discardAll);
    }
    
    handleToggleRule({expanded, id}) {
        this.props.onExpand(expanded, id);
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
    
    handleMoveElement = ({toPos, fromPos}) => {
        if (fromPos === toPos) {
            return;
        }
        this.props.onOrderRules({toPos, fromPos});
    };
    
    // show / hide additional row details
    handleToggleExpand() {
        if (this.state.editing) {
            this.props.onAskDiscardChanges(true);
        } else {
            this.props.onExpand();
        }
    };
    
    discardAll() {
        this.setState({
            editing: false,
        });
    };
    
    // jumps to selected rule as new center of view
    handleNavigate(id, parent, event) {
        this.props.onRuleIdChange({newRuleId: id, parentId: parent});
        event.stopPropagation();
    };
    
    // template rendering
    render() {
        const getItemStyle = (draggableStyle, isDragging) => ({
            // some basic styles to make the items look a bit nicer
            userSelect: this.props.expanded ? 'inherit' : 'none',
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
        
        const srcPath = sourcePath || sourcePaths;
        
        const expandedView = this.props.expanded ? (
            isRootOrObjectRule(type) ? (
                <ObjectRule
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
                    type={type}
                />
            ) : (
                <ValueMappingRule
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
        const reorderHandleButton = !this.props.expanded ? (
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
            <li
                className={className('ecc-silk-mapping__ruleitem', {
                    'ecc-silk-mapping__ruleitem--object': type === 'object',
                    'ecc-silk-mapping__ruleitem--literal': type !== 'object',
                    'ecc-silk-mapping__ruleitem--defect': errorInfo,
                    'mdl-layout_item--background-flash': this.props.isPasted,
                })}
            >
                <div
                    className="ecc-silk-mapping__ruleitem--dnd"
                    ref={this.props.provided.innerRef}
                    style={getItemStyle(
                        this.props.provided.draggableStyle,
                        this.props.snapshot.isDragging
                    )}
                    {...this.props.provided.dragHandleProps}
                >
                    {this.state.loading ? <Spinner/> : false}
                    <div
                        className={className(
                            'ecc-silk-mapping__ruleitem-summary',
                            {
                                'ecc-silk-mapping__ruleitem-summary--expanded': this.props.expanded,
                            }
                        )}
                    >
                        {reorderHandleButton}
                        <div
                            data-test-id="row-click"
                            className="mdl-list__item clickable"
                            onClick={(ev) =>
                                isObjectRule(type)
                                    ? this.handleNavigate(this.props.id, this.props.parentId, ev)
                                    : this.handleToggleExpand({force: true})
                            }
                        >
                            <MappingRuleRow
                                status={this.props.status}
                                mappingTarget={mappingTarget}
                                metadata={metadata}
                                rules={rules}
                                sourcePath={srcPath}
                                type={type}
                            />
                            <div className="mdl-list__item-secondary-content" key="action">
                                {
                                    !isObjectRule(type) && <ExpandButton
                                        id={this.props.id}
                                        expanded={this.props.expanded}
                                        onExpand={ev => {
                                            ev.stopPropagation();
                                            this.handleToggleExpand({force: true});
                                        }}
                                    />
                                }
                                {
                                    isObjectRule(type) && <NavigateButton
                                        id={this.props.id}
                                        tooltip={'Navigate to'}
                                        onClick={ev => {
                                            ev.stopPropagation();
                                            this.handleNavigate(this.props.id, this.props.parentId, ev);
                                        }}
                                    />
                                }
                                
                            </div>
                        </div>
                    </div>
                    {this.props.expanded ? (
                        <div className="ecc-silk-mapping__ruleitem-expanded">
                            {expandedView}
                        </div>
                    ) : (
                        false
                    )}
                </div>
                {this.props.provided.placeholder}
            </li>
        );
    }
}

export default MappingRule;
