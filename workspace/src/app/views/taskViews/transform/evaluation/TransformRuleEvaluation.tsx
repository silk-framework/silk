import { IPluginDetails } from "@ducks/common/typings";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import React, { ReactElement } from "react";
import { useTranslation } from "react-i18next";
import { RuleEditorProps } from "views/shared/RuleEditor/RuleEditor";
import {
    IRuleOperatorNode,
    RULE_EDITOR_NOTIFICATION_INSTANCE,
    RuleValidationError,
} from "../../../shared/RuleEditor/RuleEditor.typings";
import { EvaluatedTransformEntity, IComplexMappingRule } from "../transform.types";
import { evaluateTransformRule } from "../transform.requests";
import { FetchError } from "../../../../services/fetch/responseInterceptor";
import {
    RuleEditorEvaluationContext,
    RuleEditorEvaluationNotification,
} from "../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import ruleUtils from "../../shared/rules/rule.utils";
import { transformToValueMap } from "../transformEditor.utils";
import { LinkRuleNodeEvaluation } from "../../linking/evaluation/LinkRuleNodeEvaluation";
import { EvaluationResultType } from "../../linking/evaluation/LinkingRuleEvaluation";
import evaluationUtils from "../../shared/evaluations/evaluationOperations";
import { GlobalMappingEditorContext } from "../../../pages/MappingEditor/contexts/GlobalMappingEditorContext";
import { requestTaskContextInfo } from "@ducks/workspace/requests";

type EvaluationChildType = ReactElement<RuleEditorProps<IComplexMappingRule, IPluginDetails>>;

interface TransformRuleEvaluationProps {
    projectId: string;
    transformTaskId: string;
    /** The rule ID of the container rule, e.g. "root". */
    containerRuleId: string;
    /** The number of links that should be shown inline. */
    numberOfLinkToShow: number;
    /** The children that should be able to use this linking rule evaluation component. */
    children: EvaluationChildType;
}

export const TransformRuleEvaluation: React.FC<TransformRuleEvaluationProps> = ({
    projectId,
    transformTaskId,
    numberOfLinkToShow,
    containerRuleId,
    children,
}) => {
    const [evaluationRunning, setEvaluationRunning] = React.useState<boolean>(false);
    const [evaluationResult, setEvaluationResult] = React.useState<EvaluatedTransformEntity[]>([]);
    const [evaluationResultMap] = React.useState<Map<string, EvaluationResultType>>(new Map());
    const [evaluationResultsShown, setEvaluationResultsShown] = React.useState<boolean>(false);
    const [nodeUpdateCallbacks] = React.useState(
        new Map<string, (evaluationValues: EvaluationResultType | undefined) => any>()
    );
    const [ruleValidationError, setRuleValidationError] = React.useState<RuleValidationError | undefined>(undefined);
    const [validationNotifications, setValidationNotifications] = React.useState<RuleEditorEvaluationNotification[]>(
        []
    );
    const { registerError, registerErrorI18N } = useErrorHandler();
    const [t] = useTranslation();
    const taskContextWarningShown = React.useRef(false);
    const mappingEditorContext = React.useContext(GlobalMappingEditorContext);
    // The root node of the sub-tree that will be evaluated
    const evaluatedSubTreeNode = React.useRef<string>();
    const [evaluationError, setEvaluationError] = React.useState<string | undefined>();

    const addValidationNotification = React.useCallback((n: RuleEditorEvaluationNotification) => {
        setValidationNotifications((old) => [n, ...old]);
    }, []);

    const removeValidationNotification = React.useCallback((n: RuleEditorEvaluationNotification) => {
        setValidationNotifications((old) => old.filter((oldN) => oldN !== n));
    }, []);

    React.useEffect(() => {
        setEvaluationResult([]);
        evaluationResultMap.clear();
        nodeUpdateCallbacks.clear();
    }, [projectId, transformTaskId]);

    React.useEffect(() => {
        try {
            const valueMaps = evaluationResult.map((transform) => transformToValueMap(transform));
            nodeUpdateCallbacks.forEach((updateCallback, operatorId) => {
                const evaluationValues = valueMaps.map((valueMap) => {
                    return valueMap.get(operatorId) ?? { value: [] };
                });
                evaluationResultMap.set(operatorId, evaluationValues);
                updateCallback(evaluationValues);
            });
        } catch (ex) {
            console.warn("Unexpected error has occurred while processing the evaluation result.", ex);
        }
    }, [evaluationResult]);

    const toggleEvaluationResults = (show: boolean) => {
        if (show) {
            nodeUpdateCallbacks.forEach((updateCallback, ruleOperatorId) => {
                updateCallback(evaluationResultMap.get(ruleOperatorId));
            });
        } else {
            nodeUpdateCallbacks.forEach((updateCallback, ruleOperatorId) => {
                updateCallback(undefined);
            });
        }
        setEvaluationResultsShown(show);
    };

    const fetchTransformRuleEvaluation: (
        rule: IComplexMappingRule
    ) => Promise<EvaluatedTransformEntity[] | undefined> = async (rule: IComplexMappingRule) => {
        try {
            const result = await evaluateTransformRule(
                projectId,
                transformTaskId,
                containerRuleId,
                rule,
                numberOfLinkToShow
            );
            return result.data;
        } catch (ex) {
            if (ex.isFetchError && (ex as FetchError).httpStatus !== 409) {
                registerErrorI18N(
                    "taskViews.transformRulesEditor.errors.fetchTransformEvaluationValues.msg",
                    ex, {errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE});
            } else {
                throw ex;
            }
        }
    };

    const setEvaluationRootNode = React.useCallback((nodeId: string | undefined) => {
        evaluatedSubTreeNode.current = nodeId;
    }, []);

    const evaluationRootNode = React.useCallback(() => {
        return evaluatedSubTreeNode.current;
    }, []);

    const canBeEvaluated = React.useCallback((nodeType: string | undefined) => {
        return true;
    }, []);

    /** Start an evaluation of the linkage rule. */
    const startEvaluation = async (
        _ruleOperatorNodes: IRuleOperatorNode[],
        originalRule: any,
        quickEvaluationOnly: boolean = false
    ) => {
        setEvaluationRunning(true);
        setRuleValidationError(undefined);
        let ruleOperatorNodes = _ruleOperatorNodes;
        if (evaluatedSubTreeNode.current) {
            ruleOperatorNodes = evaluationUtils.getSubTreeNodes(ruleOperatorNodes, evaluatedSubTreeNode.current);
        }
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
            };
            const result = await fetchTransformRuleEvaluation(rule);
            setEvaluationResult(result ?? []);
        } catch (ex) {
            if (ex.isFetchError) {
                registerError(
                    "TransformRuleEvaluation.startEvaluation",
                    t("taskViews.linkRulesEditor.errors.startEvaluation.msg"),
                    ex, {errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE});
            } else if (ex.isRuleValidationError) {
                setRuleValidationError(ex);
            } else {
                registerError(
                    "LinkingRuleEvaluation.beforeStartEvaluation",
                    t("taskViews.linkRulesEditor.errors.beforeStartEvaluation.msg"),
                    ex, {errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE});
            }
        } finally {
            setEvaluationRunning(false);
        }
        if (
            mappingEditorContext.taskContext &&
            (mappingEditorContext.taskContext.inputTasks ?? []).length &&
            !taskContextWarningShown.current
        ) {
            const contextInfo = (
                await requestTaskContextInfo(projectId, transformTaskId, mappingEditorContext.taskContext)
            ).data;
            taskContextWarningShown.current = true;
            if (contextInfo.originalInputs != null && !contextInfo.originalInputs) {
                const notification: RuleEditorEvaluationNotification = {
                    intent: "warning",
                    message: t("taskViews.transformRulesEditor.evaluation.evaluationDifferentContext"),
                };
                notification.onDiscard = () => removeValidationNotification(notification);
                addValidationNotification(notification);
            }
        }
    };

    /** Called by a rule operator node to register for evaluation updates. */
    const registerForEvaluationResults = (
        ruleOperatorId: string,
        evaluationUpdate: (evaluationValues: EvaluationResultType | undefined) => void
    ) => {
        nodeUpdateCallbacks.set(ruleOperatorId, evaluationUpdate);
        evaluationUpdate(evaluationResultMap.get(ruleOperatorId));
    };

    /** Factory method used by the rule editor to create an evaluation element. */
    const createRuleEditorEvaluationComponent = (ruleOperatorId: string): JSX.Element => {
        return (
            <LinkRuleNodeEvaluation
                ruleOperatorId={ruleOperatorId}
                registerForEvaluationResults={registerForEvaluationResults}
                unregister={() => nodeUpdateCallbacks.delete(ruleOperatorId)}
                numberOfLinksToShow={numberOfLinkToShow}
                noResultMsg={t("taskViews.transformRulesEditor.evaluation.noResults")}
            />
        );
    };

    const clearRuleValidationError = () => {
        setRuleValidationError(undefined);
    };

    return (
        <RuleEditorEvaluationContext.Provider
            value={{
                supportsEvaluation: true,
                supportsQuickEvaluation: false,
                startEvaluation,
                createRuleEditorEvaluationComponent,
                evaluationRunning,
                toggleEvaluationResults,
                evaluationScore: undefined,
                // There is no evaluation result for mapping rules
                evaluationResultsShown: evaluationResultsShown,
                ruleValidationError,
                clearRuleValidationError,
                // Not needed yet
                fetchTriggerEvaluationFunction: () => {},
                setEvaluationRootNode,
                evaluationRootNode,
                canBeEvaluated,
                notifications: validationNotifications,
                ruleType: "transform",
            }}
        >
            {children}
        </RuleEditorEvaluationContext.Provider>
    );
};

export default TransformRuleEvaluation;
