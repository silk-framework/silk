import React from "react";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { IComplexMappingRule } from "./transform.types";
import { useTranslation } from "react-i18next";
import { IViewActions } from "../../plugins/PluginRegistry";
import RuleEditor, { RuleOperatorFetchFnType } from "../../shared/RuleEditor/RuleEditor";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { autoCompleteTransformSourcePath, putTransformRule, requestTransformRule } from "./transform.requests";
import { IRuleOperatorNode, RuleSaveResult } from "../../shared/RuleEditor/RuleEditor.typings";
import ruleUtils from "../shared/rules/rule.utils";
import { IStickyNote } from "../shared/task.typings";
import { LabelledParameterValue, OptionallyLabelledParameter } from "../linking/linking.types";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import { inputPathTab } from "./transformEditor.utils";

export interface TransformRuleEditorProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    transformTaskId: string;
    /** The transform rule that should be edited. This needs to be a value mapping rule. */
    ruleId: string;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
    /** Additional components that will be placed in the tool bar left to the save button. */
    additionalToolBarComponents?: () => JSX.Element | JSX.Element[];
}

/** Editor for creating and changing transform rule operator trees. */
export const TransformRuleEditor = ({
    projectId,
    transformTaskId,
    ruleId,
    additionalToolBarComponents,
    viewActions,
}: TransformRuleEditorProps) => {
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
                uiAnnotations: {
                    stickyNotes,
                },
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

    /** Get the value of an optionally labelled parameter value. */
    function optionallyLabelledParameterToValue<T>(optionallyLabelledValue: OptionallyLabelledParameter<T>): T {
        return (optionallyLabelledValue as LabelledParameterValue<T>).value
            ? (optionallyLabelledValue as LabelledParameterValue<T>).value
            : (optionallyLabelledValue as T);
    }

    const getStickyNotes = (mapping: IComplexMappingRule): IStickyNote[] =>
        (mapping && optionallyLabelledParameterToValue(mapping.uiAnnotations.stickyNotes)) || [];

    const inputPathAutoCompletion = async (term: string, limit: number): Promise<IAutocompleteDefaultResponse[]> => {
        try {
            const response = await autoCompleteTransformSourcePath(projectId, transformTaskId, ruleId);
            const results = response.data.map((data) => ({ ...data, valueType: "URI" }));
            if (term.trim() === "") {
                results.unshift({ value: "", label: `<${t("common.words.emptyPath")}>`, valueType: "StringValue" });
            }
            return results;
        } catch (err) {
            //Todo error message needs to be adapted to transform editor
            registerError(
                "LinkingRuleEditor_inputPathAutoCompletion",
                t("taskViews.linkRulesEditor.errors.inputPathAutoCompletion.msg"),
                err
            );
            return [];
        }
    };

    const sourcePathInput = () =>
        ruleUtils.inputPathOperator(
            "sourcePathInput",
            "Value path",
            "The value path of the input source of the transformation task.",
            inputPathAutoCompletion
        );

    return (
        <RuleEditor<IComplexMappingRule, IPluginDetails>
            projectId={projectId}
            taskId={transformTaskId}
            fetchRuleData={fetchTransformRule}
            fetchRuleOperators={fetchTransformRuleOperatorList}
            saveRule={saveTransformRule}
            convertRuleOperator={ruleUtils.convertRuleOperator}
            convertToRuleOperatorNodes={convertToRuleOperatorNodes}
            viewActions={viewActions}
            additionalToolBarComponents={additionalToolBarComponents}
            getStickyNotes={getStickyNotes}
            additionalRuleOperators={[
                sourcePathInput()
            ]}
            validateConnection={ruleUtils.validateConnection}
            tabs={[
                ruleUtils.sidebarTabs.all,
                inputPathTab(projectId, transformTaskId, ruleId, sourcePathInput(), (ex) =>
                    registerError(
                        "linking-rule-editor-fetch-source-paths",
                        t("taskViews.linkRulesEditor.errors.fetchLinkingPaths.msg"),
                        ex
                    )
                ),
                ruleUtils.sidebarTabs.transform,
            ]}
        />
    );
};
