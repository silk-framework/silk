import React from 'react';
import _ from 'lodash';
import { Card, CardTitle, Info, } from '@eccenca/gui-elements';
import PropTypes from 'prop-types';
import { DragDropContext, Droppable } from 'react-beautiful-dnd';
import { orderRulesAsync } from '../../store';
import DraggableItem from '../MappingRule/DraggableItem';
import rulesToList from '../../utils/rulesToList';
import ListActions from './ListActions';
import EmptyList from './EmptyList';
import reorderArray from '../../utils/reorderArray';

class MappingsList extends React.Component {
    static propTypes = {
        rules: PropTypes.array.isRequired,
        currentRuleId: PropTypes.string.isRequired,
        parentRuleId: PropTypes.string,
        isCopying: PropTypes.bool,
        handleCopy: PropTypes.func,
        handlePaste: PropTypes.func,
        handleClone: PropTypes.func,
        onClickedRemove: PropTypes.func,
        onShowSuggestions: PropTypes.func,
        onRuleIdChange: PropTypes.func,
        onAskDiscardChanges: PropTypes.func
    };
    
    static defaultProps = {
        rules: [],
        isCopying: false,
        handleCopy: () => {
        },
        handlePaste: () => {
        },
        handleClone: () => {
        },
        onClickedRemove: () => {
        },
        onShowSuggestions: () => {
        },
        onRuleIdChange: () => {
        },
        onAskDiscardChanges: () => {
        }
    };
    
    state = {
        items: rulesToList(this.props.rules, this.props.currentRuleId),
    };
    
    constructor(props) {
        super(props);
        this.handleOrderRules = this.handleOrderRules.bind(this);
        this.onDragEnd = this.onDragEnd.bind(this);
    }
    
    componentWillReceiveProps(nextProps) {
        if (!_.isEqual(this.props, nextProps)) {
            this.setState({
                items: rulesToList(nextProps.rules, nextProps.currentRuleId),
            });
        }
    }
    
    handleOrderRules({fromPos, toPos}) {
        const childrenRules = reorderArray(
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
        this.setState({
            items: reorderArray(this.state.items, fromPos, toPos)
        });
    }
    
    onDragStart(result) {
    }
    
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
        this.handleOrderRules({
            fromPos,
            toPos,
            reload,
        });
    };
    
    render() {
        const {
            rules,
            handleCopy,
            handleClone,
            onRuleIdChange,
            onAskDiscardChanges,
            onClickedRemove,
            onShowSuggestions,
            handlePaste,
            onMappingCreate
        } = this.props;
        
        return (
            <div className="ecc-silk-mapping__ruleslist">
                <Card shadow={0}>
                    <CardTitle>
                        <div className="mdl-card__title-text">
                            Mapping rules {`(${rules.length})`}
                        </div>
                    </CardTitle>
                    {
                        !rules.length
                            ? <EmptyList/>
                            : (
                                <DragDropContext
                                    onDragStart={this.onDragStart}
                                    onDragEnd={this.onDragEnd}
                                >
                                    <Droppable droppableId="droppable">
                                        {(provided) => (
                                            <ol className="mdl-list"
                                                ref={provided.innerRef}
                                                {...provided.droppableProps}
                                            >
                                                {
                                                    this.state.items.map((item, index) => <DraggableItem
                                                        {...item.props}
                                                        pos={index}
                                                        handleCopy={handleCopy}
                                                        handleClone={handleClone}
                                                        onRuleIdChange={onRuleIdChange}
                                                        onAskDiscardChanges={onAskDiscardChanges}
                                                        onClickedRemove={onClickedRemove}
                                                        onOrderRules={this.handleOrderRules}
                                                    />)
                                                }
                                                {provided.placeholder}
                                            </ol>
                                        )}
                                    </Droppable>
                                </DragDropContext>
                            )
                    }
                    <ListActions
                        onMappingCreate={onMappingCreate}
                        onPaste={handlePaste}
                        onShowSuggestions={onShowSuggestions}
                    />
                </Card>
            </div>
        );
    }
}

export default MappingsList;
