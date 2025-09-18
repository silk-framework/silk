import React from "react";
import useErrorHandler from "../../../hooks/useErrorHandler";
import {
    ActualRule,
    IComplexMappingRule,
    ITransformRule,
    ITransformTaskParameters,
    NewTransformRule,
    PartialBy,
    RuleParameterType,
    RuleReference,
} from "./transform.types";
import { useTranslation } from "react-i18next";
import { IViewActions } from "../../plugins/PluginRegistry";
import RuleEditor, { RuleOperatorFetchFnType } from "../../shared/RuleEditor/RuleEditor";
import { requestRuleOperatorPluginsDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import {
    autoCompleteTransformSourcePath,
    partialAutoCompleteTransformInputPaths,
    putTransformRule,
    requestTransformRule,
} from "./transform.requests";
import {
    IRuleOperatorNode,
    RULE_EDITOR_NOTIFICATION_INSTANCE,
    RuleSaveNodeError,
    RuleSaveResult,
    RuleValidationError,
} from "../../shared/RuleEditor/RuleEditor.typings";
import ruleUtils from "../shared/rules/rule.utils";
import { optionallyLabelledParameterToValue } from "../linking/linking.types";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import { inputPathTab } from "./transformEditor.utils";
import { FetchError } from "../../../services/fetch/responseInterceptor";
import TransformRuleEvaluation from "./evaluation/TransformRuleEvaluation";
import { DatasetCharacteristics } from "../../shared/typings";
import { requestDatasetCharacteristics, requestTaskData } from "@ducks/shared/requests";
import { GlobalMappingEditorContext } from "../../pages/MappingEditor/contexts/GlobalMappingEditorContext";
import { StickyNote } from "@eccenca/gui-elements";
import { IPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";

export interface TransformRuleEditorProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    transformTaskId: string;
    /** The container rule ID, i.e. of either the root or an object rule. */
    containerRuleId: string;
    /** The transform rule that should be edited. This needs to be a value mapping rule. Either given as rule object or referenced via rule ID (i.e. already persisted rule). */
    ruleDefinition: RuleParameterType;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
    /** After the initial fit to view, zoom to the specified Zoom level to avoid showing too small nodes. */
    initialFitToViewZoomLevel?: number;
    /** The instance of the transform rule editor. This needs to be unique if multiple instances of the linking editor are displayed on the same page. */
    instanceId: string;
    /** Additional components that will be placed in the tool bar left to the save button. */
    additionalToolBarComponents?: () => JSX.Element | JSX.Element[];
}

/** Editor for creating and changing transform rule operator trees. */
export const TransformRuleEditor = ({
    projectId,
    transformTaskId,
    containerRuleId,
    ruleDefinition,
    initialFitToViewZoomLevel,
    instanceId,
    additionalToolBarComponents,
    viewActions,
}: TransformRuleEditorProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const mappingEditorContext = React.useContext(GlobalMappingEditorContext);
    const isReferencedRule = "ruleId" in ruleDefinition;
    /** Fetches the parameters of the transform rule. */
    const fetchTransformRule = async (projectId: string, taskId: string): Promise<IComplexMappingRule | undefined> => {
        try {
            const taskData: NewTransformRule = isReferencedRule
                ? (await requestTransformRule(projectId, taskId, (ruleDefinition as RuleReference).ruleId)).data
                : (ruleDefinition as ActualRule).rule;
            if (taskData.type !== "complex") {
                throw new Error("Edit of container/object rules is not supported!");
            } else {
                return taskData as IComplexMappingRule;
            }
        } catch (err) {
            registerError(
                "transformRuleEditor_fetchTransformRule",
                t("taskViews.transformRulesEditor.errors.fetchTransformRule.msg"),
                err,
                { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
            );
        }
    };
    /** Fetches the list of operators that can be used in a transform task. */
    const fetchTransformRuleOperatorList = async () => {
        try {
            const response = (await requestRuleOperatorPluginsDetails(true)).data;
            return Object.values(response);
        } catch (err) {
            registerError(
                "TransformRuleEditor_fetchTransformRuleOperatorDetails",
                t("taskViews.transformRulesEditor.errors.fetchTransformRuleOperatorDetails.msg"),
                err,
                { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
            );
        }
    };

    /** Save the rule. */
    const saveTransformRule = async (
        ruleOperatorNodes: IRuleOperatorNode[],
        stickyNotes: StickyNote[],
        originalRule: IComplexMappingRule,
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
            if (isReferencedRule) {
                await putTransformRule(projectId, transformTaskId, (ruleDefinition as RuleReference).ruleId, rule);
            } else {
                await (ruleDefinition as ActualRule).saveRule(rule);
            }
            return {
                success: true,
            };
        } catch (err) {
            if ((err as RuleValidationError).isRuleValidationError) {
                return err;
            } else {
                if (err.isHttpError && err.httpStatus === 400 && Array.isArray((err as FetchError).body?.issues)) {
                    const fetchError = err as FetchError;
                    const nodeErrors: RuleSaveNodeError[] = fetchError.body.issues.map((issue) => ({
                        nodeId: issue.id,
                        message: issue.message,
                    }));
                    return new RuleValidationError(
                        t("taskViews.transformRulesEditor.errors.saveTransformRule.msg"),
                        nodeErrors,
                    );
                } else {
                    return {
                        success: false,
                        errorMessage: `${t("taskViews.transformRulesEditor.errors.saveTransformRule.msg")}${
                            err.message ? ": " + err.message : ""
                        }`,
                    };
                }
            }
        }
    };

    /** Converts the linking task rule to the internal representation. */
    const convertToRuleOperatorNodes = (
        mappingRule: IComplexMappingRule,
        ruleOperator: RuleOperatorFetchFnType,
    ): IRuleOperatorNode[] => {
        const operatorNodes: IRuleOperatorNode[] = [];
        ruleUtils.extractOperatorNodeFromValueInput(mappingRule.operator, operatorNodes, false, ruleOperator);
        const nodePositions = mappingRule.layout.nodePositions;
        operatorNodes.forEach((node) => {
            const pos = nodePositions[node.nodeId];
            if (pos) {
                node.position = {
                    x: pos.x,
                    y: pos.y,
                };
                node.dimension = {
                    ...node.dimension,
                    width: pos.width ?? undefined,
                    height: pos.height ?? undefined,
                };
            }
        });
        return operatorNodes;
    };

    const getStickyNotes = (mapping: IComplexMappingRule): StickyNote[] =>
        (mapping && optionallyLabelledParameterToValue(mapping.uiAnnotations.stickyNotes)) || [];

    const inputPathAutoCompletion = async (term: string, limit: number): Promise<IAutocompleteDefaultResponse[]> => {
        try {
            const response = await autoCompleteTransformSourcePath(
                projectId,
                transformTaskId,
                isReferencedRule ? (ruleDefinition as RuleReference).ruleId : containerRuleId,
                term,
                mappingEditorContext.taskContext,
                limit,
            );
            let results = response.data.map((data) => ({ ...data, valueType: "" }));
            if (term.trim() === "") {
                results.unshift({ value: "", label: `<${t("common.words.emptyPath")}>`, valueType: "StringValue" });
                //remove keep at limit size
                results = results.splice(0, limit);
            }
            return results;
        } catch (err) {
            registerError(
                "LinkingRuleEditor_inputPathAutoCompletion",
                t("taskViews.linkRulesEditor.errors.inputPathAutoCompletion.msg"),
                err,
                { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
            );
            return [];
        }
    };

    const fetchPartialAutoCompletionResult = React.useCallback(
        () =>
            async (inputString: string, cursorPosition: number): Promise<IPartialAutoCompleteResult | undefined> => {
                try {
                    const result = await partialAutoCompleteTransformInputPaths(
                        projectId,
                        transformTaskId,
                        containerRuleId,
                        inputString,
                        cursorPosition,
                        200,
                    );
                    return result.data;
                } catch (err) {
                    registerError(
                        "TransformRuleEditor_partialAutoCompletion",
                        t("taskViews.transformRulesEditor.errors.partialPathAutoCompletion.msg"),
                        err,
                    );
                }
            },
        [],
    );

    const sourcePathInput = () =>
        ruleUtils.inputPathOperator(
            "sourcePathInput",
            "Source path",
            ["Source path"],
            "The value path of the input source of the transformation task.",
            inputPathAutoCompletion,
        );

    const fetchDatasetCharacteristics = async () => {
        const result = new Map<string, DatasetCharacteristics>();
        try {
            const taskData = (await requestTaskData<ITransformTaskParameters>(projectId, transformTaskId)).data;
            const parameters: ITransformTaskParameters = taskData.data.parameters;
            const characteristics = await requestDatasetCharacteristics(projectId, parameters.selection.inputId);
            result.set("sourcePathInput", characteristics.data);
        } catch (ex) {
            // Return 404 if the dataset does not exist or the task is not a dataset
            if (ex.httpStatus !== 404) {
                registerError(
                    "TransformRuleEditor-fetchDatasetCharacteristics",
                    "Dataset characteristics could not be fetched. UI-support for language filters will not be available.",
                    ex,
                    { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
                );
            }
        }
        return result;
    };

    const tabs = React.useMemo(() => {
        return [
            ruleUtils.sidebarTabs.all,
            inputPathTab(
                projectId,
                transformTaskId,
                isReferencedRule ? (ruleDefinition as RuleReference).ruleId : containerRuleId,
                sourcePathInput(),
                (ex) =>
                    registerError(
                        "linking-rule-editor-fetch-source-paths",
                        t("taskViews.linkRulesEditor.errors.fetchLinkingPaths.msg"),
                        ex,
                        { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
                    ),
                mappingEditorContext.taskContext,
            ),
            ruleUtils.sidebarTabs.transform,
        ];
    }, []);

    return (
        <TransformRuleEvaluation
            projectId={projectId}
            transformTaskId={transformTaskId}
            containerRuleId={containerRuleId}
            numberOfLinkToShow={5}
        >
            <RuleEditor<IComplexMappingRule, IPluginDetails>
                projectId={projectId}
                taskId={transformTaskId}
                fetchRuleData={fetchTransformRule}
                fetchRuleOperators={fetchTransformRuleOperatorList}
                saveRule={saveTransformRule}
                convertRuleOperator={ruleUtils.convertRuleOperator}
                convertToRuleOperatorNodes={convertToRuleOperatorNodes}
                partialAutoCompletion={fetchPartialAutoCompletionResult}
                viewActions={viewActions}
                additionalToolBarComponents={additionalToolBarComponents}
                getStickyNotes={getStickyNotes}
                additionalRuleOperators={[sourcePathInput()]}
                validateConnection={ruleUtils.validateConnection}
                tabs={tabs}
                showRuleOnly={false}
                initialFitToViewZoomLevel={initialFitToViewZoomLevel}
                instanceId={instanceId}
                fetchDatasetCharacteristics={fetchDatasetCharacteristics}
                saveInitiallyEnabled={(ruleDefinition as ActualRule).saveInitiallyEnabled ?? false}
            />
        </TransformRuleEvaluation>
    );
};
