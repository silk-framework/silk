import React from "react";
import { requestTaskData } from "@ducks/shared/requests";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { ILinkingTaskParameters } from "./linking.types";
import { useTranslation } from "react-i18next";
import { IViewActions } from "../../plugins/PluginRegistry";
import RuleEditor from "../../shared/RuleEditor/RuleEditor";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { IProjectTask } from "@ducks/shared/typings";
import { requestUpdateProjectTask } from "@ducks/workspace/requests";
import utils from "./LinkingRuleEditor.utils";
import ruleUtils from "../shared/rules/rule.utils";
import { IRuleOperatorNode } from "../../shared/RuleEditor/RuleEditor.typings";
import { inputPathOperator } from "../shared/rules/rule.utils";

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
    /** Fetches the parameters of the linking task */
    const fetchTaskData = async (projectId: string, taskId: string) => {
        try {
            const taskData = (await requestTaskData<ILinkingTaskParameters>(projectId, taskId)).data;
            return taskData as IProjectTask<ILinkingTaskParameters>;
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
            const [operatorNodeMap, rootNodes] = ruleUtils.convertToRuleOperatorNodeMap(ruleOperatorNodes);
            if (rootNodes.length > 1) {
                throw Error(`More than one root node found! Root nodes: ${rootNodes.map((n) => n.label).join(", ")}`);
            }
            await requestUpdateProjectTask(projectId, linkingTaskId, {
                data: {
                    parameters: {
                        rule:
                            rootNodes.length === 1
                                ? utils.convertRuleOperatorNodeToSimilarityOperator(rootNodes[0], operatorNodeMap)
                                : undefined,
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

    return (
        <RuleEditor<IProjectTask<ILinkingTaskParameters>, IPluginDetails>
            projectId={projectId}
            taskId={linkingTaskId}
            fetchRuleData={fetchTaskData}
            fetchRuleOperators={fetchLinkingRuleOperatorDetails}
            saveRule={saveLinkageRule}
            convertRuleOperator={ruleUtils.convertRuleOperator}
            viewActions={viewActions}
            convertToRuleOperatorNodes={utils.convertToRuleOperatorNodes}
            additionalRuleOperators={[
                inputPathOperator(
                    "sourcePathInput",
                    "Source path",
                    "The value path of the source input of the linking task."
                ), // TODO: CMEM-3919: i18n
                inputPathOperator(
                    "targetPathInput",
                    "Target path",
                    "The value path of the target input of the linking task."
                ),
            ]}
        />
    );
};
