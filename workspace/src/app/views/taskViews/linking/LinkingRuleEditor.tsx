import React from "react";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { ILinkingTaskParameters } from "./linking.types";
import { useTranslation } from "react-i18next";
import { IViewActions } from "../../plugins/PluginRegistry";
import RuleEditor from "../../shared/RuleEditor/RuleEditor";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { requestUpdateProjectTask } from "@ducks/workspace/requests";
import utils from "./LinkingRuleEditor.utils";
import ruleUtils from "../shared/rules/rule.utils";
import { IRuleOperatorNode } from "../../shared/RuleEditor/RuleEditor.typings";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import linkingRuleRequests, { fetchLinkSpec } from "./LinkingRuleEditor.requests";
import { PathWithMetaData } from "../shared/rules/rule.typings";
import { TaskPlugin } from "@ducks/shared/typings";
import {
    ruleEditorNodeParameterValue,
    RuleEditorNodeParameterValue,
} from "../../shared/RuleEditor/model/RuleEditorModel.typings";

export interface LinkingRuleEditorProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    linkingTaskId: string;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
}

/** Editor for creating and changing linking rule operator trees. */
export const LinkingRuleEditor = ({ projectId, linkingTaskId, viewActions }: LinkingRuleEditorProps) => {
    // The linking task parameters
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const prefLang = useSelector(commonSel.localeSelector);
    // Label for source paths
    const [sourcePathLabels] = React.useState<Map<string, string>>(new Map());
    const [targetPathLabels] = React.useState<Map<string, string>>(new Map());
    React.useEffect(() => {
        fetchLabels("source");
        fetchLabels("target");
    }, [projectId, linkingTaskId, prefLang]);

    /** Fetches the labels of either the source or target data source and sets them in the corresponding label map. */
    const fetchLabels = async (sourceOrTarget: "source" | "target") => {
        const paths = await linkingRuleRequests.fetchLinkingCachedPaths(
            projectId,
            linkingTaskId,
            sourceOrTarget,
            true,
            prefLang
        );
        let labelMap = sourcePathLabels;
        if (sourceOrTarget === "source") {
            sourcePathLabels.clear();
        } else {
            targetPathLabels.clear();
            labelMap = targetPathLabels;
        }
        paths.data.forEach((path) => {
            if ((path as PathWithMetaData)?.label) {
                const pathWithMetaData = path as PathWithMetaData;
                labelMap.set(pathWithMetaData.value, pathWithMetaData.label!!);
            }
        });
    };
    /** Fetches the parameters of the linking task */
    const fetchTaskData = async (projectId: string, taskId: string) => {
        try {
            const taskData = (await fetchLinkSpec(projectId, taskId, true, prefLang)).data;
            return taskData;
        } catch (err) {
            registerError(
                "LinkingRuleEditor_fetchLinkingTask",
                t("taskViews.linkRulesEditor.errors.fetchTaskData.msg"),
                err
            );
        }
    };
    /** Fetches the list of operators that can be used in a linking task. */
    const fetchLinkingRuleOperatorDetails = async () => {
        try {
            const response = (await requestRuleOperatorPluginDetails(false)).data;
            return Object.values(response);
        } catch (err) {
            registerError(
                "LinkingRuleEditor_fetchLinkingRuleOperatorDetails",
                t("taskViews.linkRulesEditor.errors.fetchLinkingRuleOperatorDetails.msg"),
                err
            );
        }
    };

    /** Save the rule. */
    const saveLinkageRule = async (ruleOperatorNodes: IRuleOperatorNode[]) => {
        try {
            const [operatorNodeMap, rootNodes] = ruleUtils.convertToRuleOperatorNodeMap(ruleOperatorNodes, true);
            if (
                rootNodes.length === 1 &&
                rootNodes[0].pluginType !== "ComparisonOperator" &&
                rootNodes[0].pluginType !== "AggregationOperator"
            ) {
                throw Error("Rule tree root must either be an aggregation or comparison!");
            }

            await requestUpdateProjectTask(projectId, linkingTaskId, {
                data: {
                    parameters: {
                        rule: {
                            operator:
                                rootNodes.length === 1
                                    ? utils.convertRuleOperatorNodeToSimilarityOperator(rootNodes[0], operatorNodeMap)
                                    : undefined,
                            layout: ruleUtils.ruleLayout(ruleOperatorNodes),
                        },
                    },
                },
            });
            return true;
        } catch (err) {
            registerError(
                "LinkingRuleEditor_saveLinkageRule",
                t("taskViews.linkRulesEditor.errors.saveLinkageRule.msg"),
                err
            );
            return false;
        }
    };

    // TODO: CMEM-3919: Add i18n to parameter specs
    const weightParameterSpec = ruleUtils.parameterSpecification({
        label: "Weight",
        description:
            "The weight parameter can be used by the parent aggregation when combining " +
            "its input values. Only certain aggregations will consider weighted inputs. Examples are the weighted average " +
            "aggregation, quadraticMean and geometricMean.",
        type: "int",
        advanced: true,
        defaultValue: "1",
    });

    const thresholdParameterSpec = ruleUtils.parameterSpecification({
        label: "Threshold",
        description:
            "The maximum distance. For normalized distance measures, the threshold should be between 0.0 and 1.0.",
        type: "float",
        defaultValue: "0.0",
    });

    const sourcePathInput = () =>
        ruleUtils.inputPathOperator(
            "sourcePathInput",
            "Source path",
            "The value path of the source input of the linking task.",
            // FIXME: At the moment we use the validation to show a path label if it exists as message. When we have full label support in auto-completion this is probably not needed anymore.
            (value: RuleEditorNodeParameterValue) => {
                const parameterValue = ruleEditorNodeParameterValue(value);
                return {
                    valid: true,
                    message: parameterValue ? sourcePathLabels.get(parameterValue) : undefined,
                };
            }
        );

    const targetPathInput = () =>
        ruleUtils.inputPathOperator(
            "targetPathInput",
            "Target path",
            "The value path of the target input of the linking task.",
            // FIXME: At the moment we use the validation to show a path label if it exists as message
            (value: string) => {
                return {
                    valid: true,
                    message: targetPathLabels.get(value),
                };
            }
        );

    return (
        <RuleEditor<TaskPlugin<ILinkingTaskParameters>, IPluginDetails>
            projectId={projectId}
            taskId={linkingTaskId}
            fetchRuleData={fetchTaskData}
            fetchRuleOperators={fetchLinkingRuleOperatorDetails}
            saveRule={saveLinkageRule}
            convertRuleOperator={ruleUtils.convertRuleOperator}
            viewActions={viewActions}
            convertToRuleOperatorNodes={utils.convertToRuleOperatorNodes}
            additionalRuleOperators={[sourcePathInput(), targetPathInput()]}
            addAdditionParameterSpecifications={(pluginDetails) => {
                switch (pluginDetails.pluginType) {
                    case "ComparisonOperator":
                        return [
                            ["threshold", thresholdParameterSpec],
                            ["weight", weightParameterSpec],
                        ];
                    case "AggregationOperator":
                        return [["weight", weightParameterSpec]];
                    default:
                        return [];
                }
            }}
            validateConnection={ruleUtils.validateConnection}
            tabs={[
                ruleUtils.sidebarTabs.all,
                utils.inputPathTab(projectId, linkingTaskId, sourcePathInput(), "source", (ex) =>
                    registerError(
                        "linking-rule-editor-fetch-source-paths",
                        t("taskViews.linkRulesEditor.errors.fetchLinkingPaths.msg"),
                        ex
                    )
                ),
                utils.inputPathTab(projectId, linkingTaskId, targetPathInput(), "target", (ex) =>
                    registerError(
                        "linking-rule-editor-fetch-source-paths",
                        t("taskViews.linkRulesEditor.errors.fetchLinkingPaths.msg"),
                        ex
                    )
                ),
                ruleUtils.sidebarTabs.comparison,
                ruleUtils.sidebarTabs.transform,
                ruleUtils.sidebarTabs.aggregation,
            ]}
        />
    );
};
