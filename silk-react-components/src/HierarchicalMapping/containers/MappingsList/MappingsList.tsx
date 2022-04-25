import React, {useEffect, useState} from 'react';
import {Card, CardTitle,} from 'gui-elements-deprecated';
import {DragDropContext, Droppable} from 'react-beautiful-dnd';
import {orderRulesAsync} from '../../store';
import DraggableItem from '../MappingRule/DraggableItem';
import rulesToList from '../../utils/rulesToList';
import ListActions from './ListActions';
import EmptyList from './EmptyList';
import reorderArray from '../../utils/reorderArray';

interface MappingsListProps {
    rules: any[],
    loading: boolean,
    currentRuleId: string,
    parentRuleId?: string,
    isCopying?: boolean,
    handleCopy?: () => any,
    handlePaste?: () => any,
    handleClone?: () => any,
    onClickedRemove?: () => any,
    onShowSuggestions?: () => any,
    onRuleIdChange?: () => any,
    onAskDiscardChanges?: () => any
    // Executes when one of the create mapping options are clicked. The type specifies the type of mapping.
    onMappingCreate?: (mappingSkeleton: { type: "direct" | "object" }) => any
}

const nop = () => {
}

/** The list of mapping rules of an object/root mapping. */
const MappingsList = ({
                          rules = [],
                          loading,
                          currentRuleId,
                          parentRuleId,
                          isCopying = false,
                          handleCopy = nop,
                          handlePaste = nop,
                          handleClone = nop,
                          onClickedRemove = nop,
                          onShowSuggestions = nop,
                          onRuleIdChange = nop,
                          onAskDiscardChanges = nop,
                          onMappingCreate = nop
                      }: MappingsListProps) => {
    const [items, setItems] = useState<any[]>(rulesToList(rules, parentRuleId || currentRuleId))

    useEffect(() => {
        setItems(rulesToList(rules, parentRuleId || currentRuleId))
    }, [rules, parentRuleId, currentRuleId])

    const handleOrderRules = ({fromPos, toPos}: {fromPos: number, toPos: number}) => {
        const childrenRules = reorderArray(
            items.map(a => a.key),
            fromPos,
            toPos
        );
        
        orderRulesAsync({
            childrenRules,
            id: parentRuleId,
        });
        
        // FIXME: this should be in success part of request in case of error but results in content flickering than
        // manage ordering local
        setItems(reorderArray(items, fromPos, toPos))
    }
    
    const onDragStart = () => { }
    
    // template rendering
    const onDragEnd = (result) => {
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
        
        handleOrderRules({
            fromPos,
            toPos,
        });
    };

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
                                    onDragStart={onDragStart}
                                    onDragEnd={onDragEnd}
                                >
                                    <Droppable droppableId="droppable">
                                        {(provided) => (
                                            <ol className="mdl-list"
                                                ref={provided.innerRef}
                                                {...provided.droppableProps}
                                            >
                                                {
                                                    items.map((item, index) => <DraggableItem
                                                        {...item.props}
                                                        pos={index}
                                                        handleCopy={handleCopy}
                                                        handleClone={handleClone}
                                                        onRuleIdChange={onRuleIdChange}
                                                        onAskDiscardChanges={onAskDiscardChanges}
                                                        onClickedRemove={onClickedRemove}
                                                        onOrderRules={handleOrderRules}
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
                        listLoading={loading}
                    />
                </Card>
            </div>
        );
}

export default MappingsList;
