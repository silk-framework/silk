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
import UseMessageBus from '../UseMessageBusMixin';
import {DragDropContext, Droppable, Draggable} from 'react-beautiful-dnd';
import hierarchicalMappingChannel from '../store';

const MappingsList = React.createClass({
    mixins: [Navigation, UseMessageBus],
    // define property types
    propTypes: {
        rules: React.PropTypes.array.isRequired,
        // currentRuleId actually the current object mapping rule id we are viewing
    },
    getInitialState() {
        return {
            items: this.getItems(this.props.rules),
        };
    },
    componentDidMount() {
        // process reorder requests from single MappingRules
        this.subscribe(
            hierarchicalMappingChannel.subject('request.rule.orderRule'),
            this.orderRules
        );
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
    orderRules({fromPos, toPos, reload}) {
        const childrenRules = this.state.items.map(a => a.key);
        const oldItem = childrenRules[toPos];
        childrenRules[toPos] = childrenRules[fromPos];
        childrenRules[fromPos] = oldItem;
        hierarchicalMappingChannel.request({
            topic: 'rule.orderRule',
            data: {
                reload,
                childrenRules,
                fromPos,
                toPos,
                id: this.props.currentRuleId,
            },
        }).subscribe(
            () => {
                // reload mapping tree
                hierarchicalMappingChannel.subject('reload').onNext();
            },
        );
        // FIXME: this should be in success part of request in case of error but results in content flickering than
        // manage ordering local
        const items = this.reorder(
            this.state.items,
            fromPos,
            toPos,
        );
        this.setState({
            items,
        });
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
        const reload = false;
        this.orderRules({
            fromPos, toPos, reload
        })
    },
    getItems(rules) {
        return _.map(rules, (rule, i) => ({
            id: i,
            key: rule.id,
            props: {
                pos: i,
                parentId: this.props.currentRuleId,
                count: rules.length,
                key: `MappingRule_${rule.id}`,
                ...rule,
            },
            errorInfo:
                _.get(rule, 'status[0].type', false) === 'error'
                    ? _.get(rule, 'status[0].message', false)
                    : false,
        }));
    },
    onDragStart(result) {},
    componentWillReceiveProps(nextProps) {
        if (_.isEqual(this.props, nextProps)) return;

        this.setState({
            items: this.getItems(nextProps.rules),
        });
    },
    shouldComponentUpdate(nextProps, nextState) {
        return !_.isEqual(this.props, nextProps);
    },
    reorder(list, startIndex, endIndex) {
        const result = Array.from(list);
        const [removed] = result.splice(startIndex, 1);
        result.splice(endIndex, 0, removed);
        return result;
    },
    // template rendering
    render() {
        const {rules} = this.props;

        const listTitle = (
            <CardTitle>
                <div className="mdl-card__title-text">
                    Mapping rules {`(${rules.length})`}
                </div>
            </CardTitle>
        );

        const listItem = (index, item, provided, snapshot) => (
            <MappingRule {...item.props} provided snapshot />
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
            <DragDropContext
                onDragStart={this.onDragStart}
                onDragEnd={this.onDragEnd}>
                <Droppable droppableId="droppable">
                    {(provided, snapshot) => (
                        <ol className="mdl-list" ref={provided.innerRef}>
                            {_.map(this.state.items, (item, index) =>
                                listItem(index, item, provided, snapshot)
                            )}
                            {provided.placeholder}
                        </ol>
                    )}
                </Droppable>
            </DragDropContext>
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
