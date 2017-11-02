import React from 'react';
import _ from 'lodash';
import {
    Card,
    CardTitle,
    CardMenu,
    CardContent,
    FloatingActionList,
    Info,
} from 'ecc-gui-elements';
import MappingRule from './MappingRule/MappingRule';
import Navigation from '../Mixins/Navigation';
import {MAPPING_RULE_TYPE_DIRECT, MAPPING_RULE_TYPE_OBJECT} from '../helpers';
import { DragDropContext, Droppable, Draggable } from 'react-beautiful-dnd';
import className from 'classnames';
import hierarchicalMappingChannel from '../store';

const MappingsList = React.createClass({
    mixins: [Navigation],

    // define property types
    propTypes: {
        rules: React.PropTypes.array.isRequired,
    },

    getDefaultProps() {
        return {
            rules: [],
        };
    },
    reorder(list, startIndex, endIndex) {
        const result = Array.from(list);
        const [removed] = result.splice(startIndex, 1);
        result.splice(endIndex, 0, removed);

        return result;
    },
    onDragEnd(result) {
        // dropped outside the list
        if (!result.destination) {
            return;
        }
        const fromPos = result.source.index;
        const toPos = result.destination.index;
        // no actual movement
        if (fromPos === toPos) {
            return;
        }
        hierarchicalMappingChannel
            .request({topic: 'rule.orderRule', data: {
                reload: false,
                toPos,
                fromPos,
                parentId: this.props.currentRuleId,
                id: this.props.rules[result.source.index].id
            }});

        const items = this.reorder(
            this.state.items,
            result.source.index,
            result.destination.index
        );
        this.setState({
            items
        });
    },
    componentWillReceiveProps(nextProps) {

        if (_.isEqual(this.props, nextProps))
            return;

        const items = _.map(nextProps.rules, (rule, i) => {

            const errorInfo =
                _.get(rule, 'status[0].type', false) === 'error'
                    ? _.get(rule, 'status[0].message', false)
                    : false;

            return {
                id: i,
                key: rule.id,
                content: <li
                    className={
                        className(
                            'ecc-silk-mapping__ruleitem',
                            {
                                'ecc-silk-mapping__ruleitem--object': rule.type === 'object',
                                'ecc-silk-mapping__ruleitem--literal': rule.type !== 'object',
                                'ecc-silk-mapping__ruleitem--defect': errorInfo,
                            }
                        )
                    }
                >
                    <MappingRule
                    pos={i}
                    parentId={this.props.currentRuleId}
                    count={nextProps.rules.length}
                    key={`MappingRule_${rule.id}`}
                    {...rule}
                    /></li>,
            }
        });

        this.setState ({
            items
        });
    },
    shouldComponentUpdate(nextProps, nextState) {
        return !_.isEqual(this.props, nextProps)

    },
    reorder(list, startIndex, endIndex) {
        const result = Array.from(list);
        const [removed] = result.splice(startIndex, 1);
        result.splice(endIndex, 0, removed);
        return result;
    },
    // template rendering
    render() {
        const getItemStyle = (draggableStyle, isDragging) => ({
            // some basic styles to make the items look a bit nicer
            userSelect: 'none',
            background: isDragging ? 'white' : 'inherit',
            boxShadow: isDragging ? "0px 3px 4px silver" : "inherit",
            opacity: isDragging ? '1' : '1',
            zIndex: isDragging ? '1' : 'inherit',
            // styles we need to apply on draggables
            ...draggableStyle,
        });



        const {rules} = this.props;

        const listTitle = (
            <CardTitle>
                <div className="mdl-card__title-text">
                    Mapping rules {`(${rules.length})`}
                </div>
            </CardTitle>
        );

        const listItems = _.isEmpty(rules) ? (
            <CardContent>
                <Info vertSpacing border>
                    No existing mapping rules.
                </Info>
                {/* TODO: we should provide options like adding rules or suggestions here,
                         even a help text would be a good support for the user.
                         */}
            </CardContent>
        ) : (
            <div>
                    <DragDropContext onDragEnd={this.onDragEnd}>
                    <Droppable droppableId="droppable">
                        {(provided, snapshot) => (
                            <div
                                ref={provided.innerRef}
                            >
                                <ol className="mdl-list">
                                    {this.state.items.map(item => (
                                        <Draggable key={item.id} draggableId={item.id}>
                                            {(provided, snapshot) => (
                                                <div>
                                                    <div
                                                        ref={provided.innerRef}
                                                        style={getItemStyle(
                                                            provided.draggableStyle,
                                                            snapshot.isDragging
                                                        )}
                                                        {...provided.dragHandleProps}
                                                    >
                                                        {item.content}
                                                    </div>
                                                    {provided.placeholder}
                                                </div>
                                            )}
                                        </Draggable>
                                    ))}
                                    {provided.placeholder}
                                </ol>
                            </div>
                        )}

                    </Droppable>
                    </DragDropContext>
            </div>

        );

        const listActions = (
            <FloatingActionList
                fabSize="large"
                fixed
                iconName="add"
                actions={[
                    {
                        icon: 'insert_drive_file',
                        label: 'Add value mapping',
                        handler: () => {
                            this.handleCreate({
                                type: MAPPING_RULE_TYPE_DIRECT,
                            });
                        },
                    },
                    {
                        icon: 'folder',
                        label: 'Add object mapping',
                        handler: () => {
                            this.handleCreate({
                                type: MAPPING_RULE_TYPE_OBJECT,
                            });
                        },
                    },
                    {
                        icon: 'lightbulb_outline',
                        label: 'Suggest mappings',
                        handler: this.handleShowSuggestions,
                    },
                ]}
            />
        );

        return (
            <div className="ecc-silk-mapping__ruleslist">
                <Card shadow={0}>
                    {listTitle}
                    {listItems}
                    {listActions}
                </Card>
            </div>
        );
    },
});

export default MappingsList;
