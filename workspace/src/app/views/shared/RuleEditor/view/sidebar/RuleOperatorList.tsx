import { Button, Card, List, OverviewItem, OverviewItemActions, Spacing } from "gui-elements";
import { extractSearchWords } from "gui-elements/src/components/Typography/Highlighter";
import React from "react";
import { useTranslation } from "react-i18next";
import { IRuleOperator } from "../../RuleEditor.typings";
import { RuleOperator } from "./RuleOperator";

interface RuleOperatorListProps {
    /** The rule operators that should be shown. */
    ruleOperatorList: IRuleOperator[];
    /** The text search query. */
    textQuery: string;
}

/** The list of rule operators that is shown in the sidebar of the rule editor. */
export const RuleOperatorList = ({ ruleOperatorList, textQuery }: RuleOperatorListProps) => {
    const { t } = useTranslation();
    const searchWords = extractSearchWords(textQuery, true);
    const [currentlyCycledTaskId, setCurrentlyCycledTaskId] = React.useState<string | undefined>(undefined);
    const [taskCycleIndex, setTaskCycleIndex] = React.useState<number>(0);
    const totalMatches = 0; // TODO

    const resetCycleTask = () => {
        // TODO
    };
    const cycleThroughTaskNodes = (operatorId: string) => {
        // TODO
    };

    /* Add operator plugin data to drag event. */
    const onDragStartByPluginId = (pluginType: string, pluginId: string) => (e: React.DragEvent<HTMLDivElement>) => {
        const pluginData = JSON.stringify({ pluginType, pluginId });
        e.dataTransfer.setData("text/plain", pluginData);
    };

    const itemRenderer = (ruleOperator: IRuleOperator) => {
        /** currently active taskItem */
        const isActiveTaskItem = currentlyCycledTaskId === ruleOperator.pluginId;
        return (
            <div
                data-test-id={"ruleEditor-sidebar-draggable-operator"}
                key={ruleOperator.pluginId}
                draggable={true}
                onDragStart={onDragStartByPluginId(ruleOperator.pluginType, ruleOperator.pluginId)}
            >
                <Card data-test-id={"ruleEditor-sidebar-draggable-operator-" + ruleOperator.pluginId} isOnlyLayout>
                    <OverviewItem hasSpacing={true}>
                        <RuleOperator ruleOperator={ruleOperator} searchWords={searchWords} textQuery={textQuery} />
                        {totalMatches && totalMatches > 0 ? (
                            <OverviewItemActions>
                                {isActiveTaskItem ? (
                                    <Button
                                        minimal
                                        data-test-id={"cancel-cycling-through-nodes"}
                                        rightIcon={"operation-clear"}
                                        tooltip={t("RuleEditor.sidebar.cancelCycling")}
                                        tooltipProperties={{ position: "bottom", usePortal: false }}
                                        onClick={resetCycleTask}
                                    />
                                ) : null}
                                <Button
                                    minimal
                                    data-test-id={"cycle-through-nodes"}
                                    rightIcon={"navigation-jump"}
                                    text={isActiveTaskItem ? `${(taskCycleIndex || 0) + 1}/${totalMatches}` : ""}
                                    tooltip={t("RuleEditor.sidebar.cycleTooltip", { totalMatches })}
                                    tooltipProperties={{ position: "bottom", usePortal: false }}
                                    onClick={() => cycleThroughTaskNodes(ruleOperator.pluginId)}
                                />
                            </OverviewItemActions>
                        ) : null}
                    </OverviewItem>
                </Card>
                <Spacing size="tiny" />
            </div>
        );
    };

    const itemId = (ruleOperator: IRuleOperator) => `${ruleOperator.pluginType}_${ruleOperator.pluginId}`;

    return ruleOperatorList.length ? (
        <List<IRuleOperator>
            items={ruleOperatorList}
            itemId={itemId}
            itemRenderer={itemRenderer}
            limitOptions={{ initialMax: 20, stepSize: 20 }}
        />
    ) : (
        <div>{t("RuleEditor.sidebar.emptyList")}</div>
    );
};
