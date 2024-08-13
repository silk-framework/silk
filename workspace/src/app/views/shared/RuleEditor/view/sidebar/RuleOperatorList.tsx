import { Button, Card, List, OverviewItem, OverviewItemActions, Spacing } from "@eccenca/gui-elements";
import { extractSearchWords } from "@eccenca/gui-elements/src/components/Typography/Highlighter";
import React from "react";
import { useTranslation } from "react-i18next";
import { IRuleOperator, RuleOperatorNodeParameters } from "../../RuleEditor.typings";
import { RuleOperator } from "./RuleOperator";
import { IPreConfiguredRuleOperator } from "./RuleEditorOperatorSidebar.typings";

interface RuleOperatorListProps<T> {
    /** The rule operators that should be shown. */
    ruleOperatorList: IRuleOperator[];
    /** The text search query. */
    textQuery: string;
    /** Pre-configured operators. The operators must be of an existing plugin type and ID, but can have pre-configured parameters. */
    preConfiguredOperators?: IPreConfiguredOperators<T>[];
    /** If set to true, pre-configured operators are only shown when a filter query has been entered. */
    showPreconfiguredOperatorsOnlyWithQuery: boolean;
}

export interface IPreConfiguredOperators<T> {
    /** The original operator format. */
    originalOperators: T[];
    /** Returns true if the given item is of the original type T. */
    isOriginalOperator: (item: T | IRuleOperator) => boolean;
    /** Unique ID of an item. */
    itemId: (item: T) => string;
    /** A conversion function that is applied only when a pre-configured operator needs to be rendered. */
    toPreConfiguredRuleOperator: (T) => IPreConfiguredRuleOperator;
    /** If the operators should be put in front of the list or at the bottom. */
    position: "bottom" | "top";
}

/** The list of rule operators that is shown in the sidebar of the rule editor. */
export function RuleOperatorList<T>({
    ruleOperatorList,
    textQuery,
    preConfiguredOperators,
    showPreconfiguredOperatorsOnlyWithQuery,
}: RuleOperatorListProps<T>) {
    const { t } = useTranslation();
    const searchWords = extractSearchWords(textQuery, true);
    const [currentlyCycledTaskId] = React.useState<string | undefined>(undefined);
    const [taskCycleIndex] = React.useState<number>(0);
    const totalMatches = 0; // FIXME: Node cycle logic

    const overAllRuleList =
        !showPreconfiguredOperatorsOnlyWithQuery || textQuery
            ? mergeOperators(ruleOperatorList, preConfiguredOperators)
            : ruleOperatorList;

    const resetCycleTask = () => {
        // FIXME: Node cycle logic
    };
    const cycleThroughTaskNodes = (operatorId: string) => {
        // FIXME: Node cycle logic
    };

    /** Add operator plugin data to drag event. For pre-configured operators also serialize the parameter values. */
    const onDragStartByPluginId =
        (pluginType: string, pluginId: string, parameterValues?: RuleOperatorNodeParameters) =>
        (e: React.DragEvent<HTMLDivElement>) => {
            const pluginData = JSON.stringify({ pluginType, pluginId, parameterValues });
            e.dataTransfer.setData("application/reactflow", pluginData);
        };

    const itemRenderer = (ruleOperator: IRuleOperator | IPreConfiguredRuleOperator) => {
        /** currently active taskItem */
        const isActiveTaskItem = currentlyCycledTaskId === ruleOperator.pluginId;
        return (
            <div
                data-test-id={"ruleEditor-sidebar-draggable-operator"}
                draggable={true}
                onDragStart={onDragStartByPluginId(
                    ruleOperator.pluginType,
                    ruleOperator.pluginId,
                    (ruleOperator as IPreConfiguredRuleOperator).parameterOverwrites
                )}
                style={{ cursor: "grab" }}
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
                                        tooltipProps={{ placement: "bottom", usePortal: false }}
                                        onClick={resetCycleTask}
                                    />
                                ) : null}
                                <Button
                                    minimal
                                    data-test-id={"cycle-through-nodes"}
                                    rightIcon={"navigation-jump"}
                                    text={isActiveTaskItem ? `${(taskCycleIndex || 0) + 1}/${totalMatches}` : ""}
                                    tooltip={t("RuleEditor.sidebar.cycleTooltip", { totalMatches })}
                                    tooltipProps={{ placement: "bottom", usePortal: false }}
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

    /** Converts the original version of a pre-configured operator into a IPreConfiguredRuleOperator. */
    const listItemRenderer = (listItem: IRuleOperator | T) => {
        if (preConfiguredOperators && preConfiguredOperators.find((c) => c.isOriginalOperator(listItem))) {
            const preConfiguredOperatorConfig = preConfiguredOperators.find((c) => c.isOriginalOperator(listItem));
            return itemRenderer(preConfiguredOperatorConfig!!.toPreConfiguredRuleOperator(listItem));
        } else {
            return itemRenderer(listItem as IRuleOperator);
        }
    };

    const itemId = (listItem: IRuleOperator | T) => {
        if (preConfiguredOperators && preConfiguredOperators.find((c) => c.isOriginalOperator(listItem))) {
            const preConfiguredOperatorConfig = preConfiguredOperators.find((c) => c.isOriginalOperator(listItem));
            return preConfiguredOperatorConfig!!.itemId(listItem as T);
        } else {
            return `${(listItem as IRuleOperator).pluginType}_${(listItem as IRuleOperator).pluginId}`;
        }
    };

    return overAllRuleList.length ? (
        <List<IRuleOperator | T>
            items={overAllRuleList}
            itemId={itemId}
            itemRenderer={listItemRenderer}
            limitOptions={{ initialMax: 20, stepSize: 20 }}
        />
    ) : (
        <div>{t("RuleEditor.sidebar.emptyList")}</div>
    );
}

function mergePreConfiguredOperators<T>(preConfiguredOperators: IPreConfiguredOperators<T>[]): T[] {
    const result: T[] = [];
    preConfiguredOperators.forEach((c) => result.push(...c.originalOperators));
    return result;
}

/** Merges the list of "normal" and pre-configured operators. */
function mergeOperators<T>(
    ruleOperatorList: IRuleOperator[],
    preConfiguredOperators?: IPreConfiguredOperators<T | IRuleOperator>[]
) {
    if (preConfiguredOperators) {
        if (ruleOperatorList.length === 0) {
            return mergePreConfiguredOperators(preConfiguredOperators);
        } else if (preConfiguredOperators.every((c) => c.originalOperators.length === 0)) {
            return ruleOperatorList;
        } else {
            const topPreConfiguredOperators = preConfiguredOperators.filter((c) => c.position === "top");
            const bottomPreConfiguredOperators = preConfiguredOperators.filter((c) => c.position === "bottom");
            const result: (T | IRuleOperator)[] = [
                topPreConfiguredOperators.map((c) => c.originalOperators),
                ruleOperatorList,
                bottomPreConfiguredOperators.map((c) => c.originalOperators),
            ].flat(2);
            return result;
        }
    } else {
        return ruleOperatorList;
    }
}
