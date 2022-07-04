import React from "react";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { IComplexMappingRule } from "./transform.types";
import { useTranslation } from "react-i18next";
import { IViewActions } from "../../plugins/PluginRegistry";
import RuleEditor, { RuleOperatorFetchFnType } from "../../shared/RuleEditor/RuleEditor";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { putTransformRule, requestTransformRule } from "./transform.requests";
import { IRuleOperatorNode, RuleSaveResult } from "../../shared/RuleEditor/RuleEditor.typings";
import ruleUtils from "../shared/rules/rule.utils";
import { IStickyNote } from "../shared/task.typings";

export interface TransformRuleEditorProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    transformTaskId: string;
    /** The transform rule that should be edited. This needs to be a value mapping rule. */
    ruleId: string;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
}

/** Editor for creating and changing transform rule operator trees. */
export const TransformRuleEditor = ({ projectId, transformTaskId, ruleId }: TransformRuleEditorProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    /** Fetches the parameters of the transform rule. */
    const fetchTransformRule = async (projectId: string, taskId: string): Promise<IComplexMappingRule | undefined> => {
        try {
            const taskData = (await requestTransformRule(projectId, taskId, ruleId)).data;
            if (taskData.type !== "complex") {
                throw new Error("Edit of container/object rules is not supported!");
            } else {
                return taskData as IComplexMappingRule;
            }
        } catch (err) {
            registerError(
                "transformRuleEditor_fetchTransformRule",
                t("taskViews.transformRulesEditor.errors.fetchTransformRule.msg"),
                err
            );
        }
    };
    /** Fetches the list of operators that can be used in a transform task. */
    const fetchTransformRuleOperatorList = async () => {
        try {
            const response = (await requestRuleOperatorPluginDetails(true)).data;
            return Object.values(response);
        } catch (err) {
            registerError(
                "TransformRuleEditor_fetchTransformRuleOperatorDetails",
                t("taskViews.transformRulesEditor.errors.fetchTransformRuleOperatorDetails.msg"),
                err
            );
        }
    };

    /** Save the rule. */
    const saveTransformRule = async (
        ruleOperatorNodes: IRuleOperatorNode[],
        stickyNotes: IStickyNote[],
        originalRule: IComplexMappingRule
    ): Promise<RuleSaveResult> => {
        try {
            //Todo add sticky notes to saveRule and backend
            const [operatorNodeMap, rootNodes] = ruleUtils.convertToRuleOperatorNodeMap(ruleOperatorNodes, true);
            if (rootNodes.length !== 1) {
                return {
                    success: false,
                    errorMessage: `There must be exactly one root node, but ${
                        rootNodes.length
                    } have been found! Root nodes: ${rootNodes.map((n) => n.label).join(", ")}`,
                    nodeErrors: rootNodes.map((rootNode) => ({
                        nodeId: rootNode.nodeId,
                    })),
                };
            }
            const rule: IComplexMappingRule = {
                ...originalRule,
                sourcePaths: [],
                operator: ruleUtils.convertRuleOperatorNodeToValueInput(rootNodes[0], operatorNodeMap),
                layout: ruleUtils.ruleLayout(ruleOperatorNodes),
            };
            await putTransformRule(projectId, transformTaskId, ruleId, rule);
            return {
                success: true,
            };
        } catch (err) {
            console.log("Error", err);
            registerError(
                "TransformRuleEditor_saveTransformRule",
                t("taskViews.transformRulesEditor.errors.saveTransformRule.msg"),
                err
            );
            return {
                success: false,
                errorMessage: t("taskViews.transformRulesEditor.errors.saveTransformRule.msg"),
            };
        }
    };

    /** Converts the linking task rule to the internal representation. */
    const convertToRuleOperatorNodes = (
        mappingRule: IComplexMappingRule,
        ruleOperator: RuleOperatorFetchFnType
    ): IRuleOperatorNode[] => {
        const operatorNodes: IRuleOperatorNode[] = [];
        ruleUtils.extractOperatorNodeFromValueInput(mappingRule.operator, operatorNodes, false, ruleOperator);
        const nodePositions = mappingRule.layout;
        operatorNodes.forEach((node) => (node.position = nodePositions[node.nodeId]));
        return operatorNodes;
    };

    return (
        <RuleEditor<IComplexMappingRule, IPluginDetails>
            projectId={projectId}
            taskId={transformTaskId}
            fetchRuleData={fetchTransformRule}
            fetchRuleOperators={fetchTransformRuleOperatorList}
            saveRule={saveTransformRule}
            convertRuleOperator={ruleUtils.convertRuleOperator}
            convertToRuleOperatorNodes={convertToRuleOperatorNodes}
            additionalRuleOperators={[
                ruleUtils.inputPathOperator(
                    "valuePathInput",
                    "Value path",
                    "The value path of the input source of the transformation task."
                ), // FIXME: Add i18n for rule operators
            ]}
            validateConnection={ruleUtils.validateConnection}
            tabs={[ruleUtils.sidebarTabs.all, ruleUtils.sidebarTabs.transform]}
        />
    );
};
