import React from 'react';
import _ from 'lodash';
import {
    Card,
    CardTitle,
    CardContent,
    FloatingActionList,
    Info,
} from '@eccenca/gui-elements';
import MappingRule from './MappingRule/MappingRule';
import { MAPPING_RULE_TYPE_OBJECT } from '../utils/constants';
import PropTypes from 'prop-types';
import { DragDropContext, Droppable } from 'react-beautiful-dnd';
import { orderRulesAsync } from '../store';
import { MAPPING_RULE_TYPE_DIRECT, MESSAGES } from '../utils/constants';
import EventEmitter from '../utils/EventEmitter';

class MappingsList extends React.Component {
    static propTypes = {
        rules: PropTypes.array.isRequired,
        parentRuleId: PropTypes.string,
        onClickedRemove: PropTypes.func,
    };

    static defaultProps = {
        rules: [],
    };

    state = {
        items: this.getItems(this.props.rules),
    };
    
    constructor(props) {
        super(props);
        this.orderRules = this.orderRules.bind(this);
        this.onDragEnd = this.onDragEnd.bind(this);
        this.handleCreate = this.handleCreate.bind(this);
        this.handleShowSuggestions = this.handleShowSuggestions.bind(this);
    }
    
    componentDidMount() {
        // process reorder requests from single MappingRules
        EventEmitter.on(MESSAGES.RULE.REQUEST_ORDER, this.orderRules);
    }

    componentWillUnmount() {
        // process reorder requests from single MappingRules
        EventEmitter.off(MESSAGES.RULE.REQUEST_ORDER, this.orderRules);
    }

    componentWillReceiveProps(nextProps) {
        if (_.isEqual(this.props, nextProps)) return;

        this.setState({
            items: this.getItems(nextProps.rules),
        });
    }

    // shouldComponentUpdate(nextProps) {
    //     return !_.isEqual(this.props, nextProps);
    // }

    orderRules({ fromPos, toPos }) {
        const childrenRules = this.reorder(
            this.state.items.map(a => a.key),
            fromPos,
            toPos
        );
        orderRulesAsync({
            childrenRules,
            id: this.props.parentRuleId,
        });

        // FIXME: this should be in success part of request in case of error but results in content flickering than
        // manage ordering local
        const items = this.reorder(this.state.items, fromPos, toPos);
        this.setState({
            items,
        });
    }

    onDragStart(result) {}

    // template rendering
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
            fromPos,
            toPos,
            reload,
        });
    };

    handleCreate(infoCreation) {
        EventEmitter.emit(MESSAGES.MAPPING.CREATE, infoCreation);
    };

    handleShowSuggestions(event) {
        event.persist();
        EventEmitter.emit(MESSAGES.MAPPING.SHOW_SUGGESTIONS, event);
    };

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
    }

    reorder(list, startIndex, endIndex) {
        const result = Array.from(list);
        const [removed] = result.splice(startIndex, 1);
        result.splice(endIndex, 0, removed);

        return result;
    }

    render() {
        const { rules } = this.props;

        const listTitle = (
            <CardTitle>
                <div className="mdl-card__title-text">
                    Mapping rules {`(${rules.length})`}
                </div>
            </CardTitle>
        );

        const listItem = (index, item) => (
            <MappingRule
                {...item.props}
                provided
                snapshot
                handleCopy={this.props.handleCopy}
                handleClone={this.props.handleClone}
                onRuleIdChange={this.props.onRuleIdChange}
                onAskDiscardChanges={this.props.onAskDiscardChanges}
                onClickedRemove={this.props.onClickedRemove}
            />
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
                onDragEnd={this.onDragEnd}
            >
                <Droppable droppableId="droppable">
                    {(provided, snapshot) => (
                        <ol className="mdl-list" ref={provided.innerRef}>
                            {_.map(this.state.items, (item, index) =>
                                listItem(index, item, provided, snapshot))}
                            {provided.placeholder}
                        </ol>
                    )}
                </Droppable>
            </DragDropContext>
        );

        const openToBottomFn = () => {
            // Calculates if the floating menu list should be opened to the top or bottom depending on the space to the top.
            let toBottom = false;
            try {
                const floatButtonRect = $('.ecc-floatingactionlist button.mdl-button')[0].getBoundingClientRect();
                const navHeaderRect = $('.ecc-silk-mapping__navheader div.mdl-card--stretch')[0].getBoundingClientRect();
                const availableSpace = floatButtonRect.top - navHeaderRect.bottom;
                const spaceNeededForMenuList = 200; // This is not known before the menu list is rendered, so we assume at most 4 elements
                toBottom = availableSpace < spaceNeededForMenuList;
            } catch (error) {}
            return toBottom;
        };

        const listActions = (
            <FloatingActionList
                fabSize="large"
                fixed
                iconName="add"
                openToBottom={openToBottomFn}
                actions={_.concat(
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
                    (sessionStorage.getItem('copyingData') !== null) ? {
                        icon: 'folder',
                        label: 'Paste mapping',
                        handler: () => this.props.handlePaste(),
                    } : [],
                    {
                        icon: 'lightbulb_outline',
                        label: 'Suggest mappings',
                        handler: this.handleShowSuggestions,
                    },
                )}
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
    }
}

export default MappingsList;
