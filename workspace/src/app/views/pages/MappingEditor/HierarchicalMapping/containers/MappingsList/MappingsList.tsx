import React, { useEffect, useState } from "react";
import {
    Card,
    CardHeader,
    CardOptions,
    CardTitle,
    Divider,
    StickyTarget,
    StickyTargetProps,
    OverviewItemList,
} from "@eccenca/gui-elements";
import { DndContext, KeyboardSensor, useSensor, useSensors } from "@dnd-kit/core";
import {
    arrayMove,
    SortableContext,
    sortableKeyboardCoordinates,
    verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { getApiDetails } from "../../store";
import DraggableItem from "../MappingRule/DraggableItem";
import rulesToList from "../../utils/rulesToList";
import ListActions from "./ListActions";
import EmptyList from "./EmptyList";
import { Spinner } from "@eccenca/gui-elements";
import silkRestApi from "../../../api/silkRestApi";
import useErrorHandler from "../../../../../../hooks/useErrorHandler";
import { IViewActions } from "../../../../../../views/plugins/PluginRegistry";
import dndkitUtils from "../../../../../../utils/dndkitUtils";
import { restrictToVerticalAxis } from "@dnd-kit/modifiers";

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
    onRuleIdChange?: (param: any) => any;
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

    const sensors = useSensors(
        useSensor(dndkitUtils.DefaultMouseSensor, dndkitUtils.defaultMouseSensorOptions),
        useSensor(KeyboardSensor, {
            coordinateGetter: sortableKeyboardCoordinates,
        }),
    );

    useEffect(() => {
        setItems(rulesToList(rules, parentRuleId || currentRuleId));
    }, [rules, parentRuleId, currentRuleId]);

    const handleOrderRules = async ({ fromPos, toPos }: { fromPos: number; toPos: number }) => {
        const reorderedItems = arrayMove(items, fromPos, toPos);
        const childrenRules = reorderedItems.map((a) => a.key);

        const { project, transformTask } = getApiDetails();
        if (project != null && transformTask != null && parentRuleId != null) {
            setReorderingRequestPending(true);
            try {
                setItems(reorderedItems);
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

    const onDragEnd = (event) => {
        const { active, over } = event;

        // dropped outside the list
        if (!over) {
            return;
        }
        // no actual movement
        if (active.id === over.id) {
            return;
        }

        const fromPos = items.findIndex((item) => `draggable-${item.key}` === active.id);
        const toPos = items.findIndex((item) => `draggable-${item.key}` === over.id);

        if (fromPos !== -1 && toPos !== -1) {
            handleOrderRules({
                fromPos,
                toPos,
            });
        }
    };

    return (
        <div className="ecc-silk-mapping__ruleslist">
            {reorderingRequestPending && <Spinner position={"global"} />}
            <Card elevation={0}>
                <StickyTarget local background={"card"} offset={`${-1}px` as StickyTargetProps["offset"]}>
                    <CardHeader>
                        <CardTitle>Mapping rules {`(${rules.length})`}</CardTitle>
                        <CardOptions>
                            <ListActions
                                onMappingCreate={onMappingCreate}
                                onPaste={handlePaste}
                                onShowSuggestions={onShowSuggestions}
                                listLoading={loading}
                            />
                        </CardOptions>
                    </CardHeader>
                    <Divider />
                </StickyTarget>
                {!rules.length ? (
                    <EmptyList />
                ) : (
                    <DndContext sensors={sensors} onDragEnd={onDragEnd} modifiers={[restrictToVerticalAxis]}>
                        <SortableContext
                            items={items.map((item) => `draggable-${item.key}`)}
                            strategy={verticalListSortingStrategy}
                        >
                            <OverviewItemList hasDivider>
                                {items.map((item, index) => (
                                    <DraggableItem
                                        key={item.key}
                                        {...item.props}
                                        parentRuleId={currentRuleId}
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
                            </OverviewItemList>
                        </SortableContext>
                    </DndContext>
                )}
            </Card>
        </div>
    );
};

export default MappingsList;
