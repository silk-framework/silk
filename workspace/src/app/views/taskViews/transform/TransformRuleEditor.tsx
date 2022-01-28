import React from "react";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { IComplexMappingRule } from "./transform.types";
import { useTranslation } from "react-i18next";
import { IViewActions } from "../../plugins/PluginRegistry";
import { RuleEditor } from "../../shared/RuleEditor/RuleEditor";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { putTransformRule, requestTransformRule } from "./transform.requests";
import {
    convertRuleOperator,
    convertRuleOperatorNodeToValueInput,
    convertToRuleOperatorNodeMap,
} from "../linking/LinkingRuleEditor";
import { IRuleOperatorNode } from "../../shared/RuleEditor/RuleEditor.typings";
import { extractOperatorNodeFromValueInput } from "../shared/rules/rule.utils";

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
    const saveTransformRule = async (ruleOperatorNodes: IRuleOperatorNode[], originalRule: IComplexMappingRule) => {
        try {
            const [operatorNodeMap, rootNodes] = convertToRuleOperatorNodeMap(ruleOperatorNodes);
            if (rootNodes.length !== 1) {
                throw Error(
                    `There must be exactly one root node, but ${
                        rootNodes.length
                    } have been found! Root nodes: ${rootNodes.map((n) => n.label).join(", ")}`
                );
            }
            const rule: IComplexMappingRule = {
                ...originalRule,
                sourcePaths: [],
                operator: convertRuleOperatorNodeToValueInput(rootNodes[0], operatorNodeMap),
            };
            await putTransformRule(projectId, transformTaskId, ruleId, rule);
            return true;
        } catch (err) {
            registerError(
                "TransformRuleEditor_saveTransformRule",
                t("taskViews.transformRulesEditor.errors.saveTransformRule.msg"),
                err
            );
            return false;
        }
    };

    /** Converts the linking task rule to the internal representation. */
    const convertToRuleOperatorNodes = (mappingRule: IComplexMappingRule): IRuleOperatorNode[] => {
        const operatorNodes: IRuleOperatorNode[] = [];
        extractOperatorNodeFromValueInput(mappingRule.operator, operatorNodes, false);
        return operatorNodes;
    };

    return (
        <RuleEditor<IComplexMappingRule, IPluginDetails>
            projectId={projectId}
            taskId={transformTaskId}
            fetchRuleData={fetchTransformRule}
            fetchRuleOperators={fetchTransformRuleOperatorList}
            saveRule={saveTransformRule}
            convertRuleOperator={convertRuleOperator}
            convertToRuleOperatorNodes={convertToRuleOperatorNodes}
        />
    );
};
