import React, { useEffect, useState } from "react";
import { Card, CardTitle } from "gui-elements-deprecated";
import { DragDropContext, Droppable } from "react-beautiful-dnd";
import { getApiDetails } from "../../store";
import DraggableItem from "../MappingRule/DraggableItem";
import rulesToList from "../../utils/rulesToList";
import ListActions from "./ListActions";
import EmptyList from "./EmptyList";
import reorderArray from "../../utils/reorderArray";
import { Spinner } from "@eccenca/gui-elements";
import silkRestApi from "../../../api/silkRestApi";
import useErrorHandler from "../../../../../../hooks/useErrorHandler";
import { IViewActions } from "../../../../../../views/plugins/PluginRegistry";

interface MappingsListProps {
    rules: any[];
    loading: boolean;
    currentRuleId: string;
    parentRuleId?: string;
    isCopying?: boolean;
    handleCopy?: (id, type) => any;
    handlePaste?: () => any;
    handleClone?: (id, type, parent) => any;
    onClickedRemove?: () => any;
    onShowSuggestions?: () => any;
    onRuleIdChange?: (param:  any) => any;
    onAskDiscardChanges?: (param: any) => any;
    openMappingEditor: () => void;
    startFullScreen: boolean;
    // Executes when one of the create mapping options are clicked. The type specifies the type of mapping.
    onMappingCreate?: (mappingSkeleton: { type: "direct" | "object" }) => any;
    viewActions: IViewActions;
}

const nop = () => {};

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
    onMappingCreate = nop,
    openMappingEditor,
    startFullScreen = false,
    viewActions,
}: MappingsListProps) => {
    const [items, setItems] = useState<any[]>(rulesToList(rules, parentRuleId || currentRuleId));
    const [reorderingRequestPending, setReorderingRequestPending] = useState(false);
    const { registerError } = useErrorHandler();
    useEffect(() => {
        setItems(rulesToList(rules, parentRuleId || currentRuleId));
    }, [rules, parentRuleId, currentRuleId]);

    const handleOrderRules = async ({ fromPos, toPos }: { fromPos: number; toPos: number }) => {
        const childrenRules = reorderArray(
            items.map((a) => a.key),
            fromPos,
            toPos
        );

        const { project, transformTask } = getApiDetails();
        if (project != null && transformTask != null && parentRuleId != null) {
            setReorderingRequestPending(true);
            try {
                setItems(reorderArray(items, fromPos, toPos));
                await silkRestApi.reorderRules(project, transformTask, parentRuleId, childrenRules);
            } catch (ex) {
                registerError("MappingsList.handleOrderRules", "Reordering of the mappings rules has failed.", ex);
                // Request failed, rollback change.
                setItems(items);
            } finally {
                setReorderingRequestPending(false);
            }
        }
    };

    const onDragStart = () => {};

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
            {reorderingRequestPending && <Spinner position={"global"} />}
            <Card shadow={0}>
                <CardTitle>
                    <div className="mdl-card__title-text">Mapping rules {`(${rules.length})`}</div>
                </CardTitle>
                {!rules.length ? (
                    <EmptyList />
                ) : (
                    <DragDropContext onDragStart={onDragStart} onDragEnd={onDragEnd}>
                        <Droppable droppableId="droppable">
                            {(provided) => (
                                <ol className="mdl-list" ref={provided.innerRef} {...provided.droppableProps}>
                                    {items.map((item, index) => (
                                        <DraggableItem
                                            {...item.props}
                                            pos={index}
                                            handleCopy={handleCopy}
                                            handleClone={handleClone}
                                            onRuleIdChange={onRuleIdChange}
                                            onAskDiscardChanges={onAskDiscardChanges}
                                            onClickedRemove={onClickedRemove}
                                            onOrderRules={handleOrderRules}
                                            openMappingEditor={openMappingEditor}
                                            mapRuleLoading={loading}
                                            startFullScreen={startFullScreen}
                                            viewActions={viewActions}
                                        />
                                    ))}
                                    {provided.placeholder}
                                </ol>
                            )}
                        </Droppable>
                    </DragDropContext>
                )}
                <ListActions
                    onMappingCreate={onMappingCreate}
                    onPaste={handlePaste}
                    onShowSuggestions={onShowSuggestions}
                    listLoading={loading}
                />
            </Card>
        </div>
    );
};

export default MappingsList;
