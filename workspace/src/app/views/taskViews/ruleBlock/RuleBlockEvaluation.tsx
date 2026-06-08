import React, { ReactElement } from "react";
import { useTranslation } from "react-i18next";
import { IPluginDetails } from "@ducks/common/typings";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { RuleEditorProps } from "../../shared/RuleEditor/RuleEditor";
import {
    IRuleOperatorNode,
    RULE_EDITOR_NOTIFICATION_INSTANCE,
    RuleValidationError,
} from "../../shared/RuleEditor/RuleEditor.typings";
import {
    RuleEditorEvaluationConfigMenu,
    RuleEditorEvaluationContext,
} from "../../shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import { LinkRuleNodeEvaluation } from "../linking/evaluation/LinkRuleNodeEvaluation";
import { EvaluationResultType } from "../linking/evaluation/LinkingRuleEvaluation";
import evaluationUtils from "../shared/evaluations/evaluationOperations";
import ruleUtils from "../shared/rules/rule.utils";
import { EvaluatedTransformEntity } from "../transform/transform.types";
import { IRuleBlockInputExample, IRuleBlockModel, RuleBlockPort, IRuleBlockTaskParameters } from "./ruleBlock.types";
import { requestRuleBlockEvaluation } from "./ruleBlock.requests";
import { IProjectTask } from "@ducks/shared/typings";
import { SampleError } from "../../shared/SampleError/SampleError";
import { RuleBlockEvaluationOptionalContext } from "./RuleBlockEvaluationOptionalContext";

type RuleBlockTaskData = IProjectTask<IRuleBlockTaskParameters>;
type EvaluationChildType = ReactElement<RuleEditorProps<RuleBlockTaskData, IPluginDetails>>;

interface RuleBlockEvaluationProps {
    projectId: string;
    ruleBlockTaskId: string;
    numberOfEntitiesToShow: number;
    getPorts: () => RuleBlockPort[];
    getInputExamples: () => IRuleBlockInputExample[];
    getEvaluationInputExamples: () => IRuleBlockInputExample[];
    getSelectedEvaluationExampleIds: () => string[];
    onOpenExampleValuesDialog?: (highlightedPortId?: string) => void;
    children: EvaluationChildType;
}

const createCurrentRuleBlockModel = (
    ruleOperatorNodes: IRuleOperatorNode[],
    originalTask: RuleBlockTaskData,
    ports: RuleBlockPort[],
    inputExamples: IRuleBlockInputExample[],
): IRuleBlockModel => {
    const [operatorNodeMap, rootNodes] = ruleUtils.convertToRuleOperatorNodeMap(ruleOperatorNodes, true);
    if (rootNodes.length !== 1) {
        throw new RuleValidationError(
            `There must be exactly one root node, but ${rootNodes.length} have been found.`,
            rootNodes.map((rootNode) => ({
                nodeId: rootNode.nodeId,
            })),
        );
    }
    const currentModel = originalTask.data.parameters.ruleBlockModel;
    return {
        ports,
        inputExamples,
        operatorTree: ruleUtils.convertRuleOperatorNodeToValueInput(rootNodes[0], operatorNodeMap),
        layout: ruleUtils.ruleLayout(ruleOperatorNodes),
        uiAnnotations: currentModel?.uiAnnotations,
    };
};

const evaluationTreeToValueMap = (evaluation: EvaluatedTransformEntity): Map<string, EvaluationResultType[number]> => {
    const valueMap = new Map<string, EvaluationResultType[number]>();

    const traverseEvaluationTree = (currentEvaluation: EvaluatedTransformEntity) => {
        let error: SampleError | undefined = undefined;
        if (currentEvaluation.error) {
            error = {
                error: currentEvaluation.error,
                entity: "",
                stacktrace: currentEvaluation.stacktrace,
                values: currentEvaluation.children?.map((child) => child.values),
            };
        }
        valueMap.set(currentEvaluation.operatorId, { value: currentEvaluation.values, error });
        currentEvaluation.children?.forEach((child) => traverseEvaluationTree(child));
    };

    traverseEvaluationTree(evaluation);
    return valueMap;
};

const RuleBlockEvaluation = ({
    projectId,
    ruleBlockTaskId,
    numberOfEntitiesToShow,
    getPorts,
    getInputExamples,
    getEvaluationInputExamples,
    getSelectedEvaluationExampleIds,
    onOpenExampleValuesDialog,
    children,
}: RuleBlockEvaluationProps) => {
    const optionalContext = React.useContext(RuleBlockEvaluationOptionalContext);
    const usesExternalEvaluation = optionalContext.externalEvaluationResults !== undefined;
    const [evaluationRunning, setEvaluationRunning] = React.useState<boolean>(false);
    const [evaluationResult, setEvaluationResult] = React.useState<EvaluatedTransformEntity[]>([]);
    const [evaluationResultsShown, setEvaluationResultsShown] = React.useState<boolean>(false);
    const [ruleValidationError, setRuleValidationError] = React.useState<RuleValidationError | undefined>(undefined);
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();
    const evaluatedSubTreeNode = React.useRef<string>(undefined);
    const evaluationResultsShownRef = React.useRef(evaluationResultsShown);
    const evaluationResultMap = React.useRef<Map<string, EvaluationResultType>>(new Map());
    const nodeUpdateCallbacks = React.useRef(
        new Map<string, (evaluationValues: EvaluationResultType | undefined) => any>(),
    );
    evaluationResultsShownRef.current = evaluationResultsShown;

    React.useEffect(() => {
        // Load external evaluations results if existing
        setEvaluationResult(optionalContext.externalEvaluationResults ?? []);
        setEvaluationResultsShown(usesExternalEvaluation);
        evaluationResultsShownRef.current = usesExternalEvaluation;
        if (usesExternalEvaluation) {
            nodeUpdateCallbacks.current.forEach((updateCallback, ruleOperatorId) => {
                updateCallback(evaluationResultMap.current.get(ruleOperatorId) ?? []);
            });
        } else {
            nodeUpdateCallbacks.current.forEach((updateCallback) => {
                updateCallback(undefined);
            });
        }
        setEvaluationRunning(false);
        setRuleValidationError(undefined);
        evaluationResultMap.current.clear();
    }, [optionalContext.externalEvaluationResults, projectId, ruleBlockTaskId, usesExternalEvaluation]);

    React.useEffect(() => {
        try {
            const valueMaps = evaluationResult.map((transform) => evaluationTreeToValueMap(transform));
            const operatorIds = new Set<string>();
            valueMaps.forEach((valueMap) => {
                valueMap.forEach((_value, operatorId) => {
                    operatorIds.add(operatorId);
                });
            });
            evaluationResultMap.current.clear();
            operatorIds.forEach((operatorId) => {
                const evaluationValues = valueMaps.map((valueMap) => {
                    return valueMap.get(operatorId) ?? { value: [] };
                });
                evaluationResultMap.current.set(operatorId, evaluationValues);
            });
            nodeUpdateCallbacks.current.forEach((updateCallback, operatorId) => {
                updateCallback(evaluationValuesForOperator(operatorId, evaluationResultsShownRef.current));
            });
        } catch (ex) {
            console.warn("Unexpected error has occurred while processing the rule block evaluation result.", ex);
        }
    }, [evaluationResult, evaluationResultsShown]);

    const evaluationValuesForOperator = React.useCallback((
        operatorId: string,
        showEvaluationResults: boolean,
    ): EvaluationResultType | undefined => {
        if (!showEvaluationResults) {
            return undefined;
        }
        return evaluationResultMap.current.get(operatorId) ?? [];
    }, []);

    const toggleEvaluationResults = React.useCallback((show: boolean) => {
        if (show) {
            nodeUpdateCallbacks.current.forEach((updateCallback, ruleOperatorId) => {
                updateCallback(evaluationValuesForOperator(ruleOperatorId, true));
            });
        } else {
            nodeUpdateCallbacks.current.forEach((updateCallback) => {
                updateCallback(undefined);
            });
        }
        evaluationResultsShownRef.current = show;
        setEvaluationResultsShown(show);
    }, [evaluationValuesForOperator]);

    const setEvaluationRootNode = React.useCallback((nodeId: string | undefined) => {
        evaluatedSubTreeNode.current = nodeId;
    }, []);

    const evaluationRootNode = React.useCallback(() => {
        return evaluatedSubTreeNode.current;
    }, []);

    const canBeEvaluated = React.useCallback((_nodeType: string | undefined) => {
        return true;
    }, []);

    const startEvaluation = React.useCallback(async (
        _ruleOperatorNodes: IRuleOperatorNode[],
        originalTask: RuleBlockTaskData,
        _quickEvaluationOnly: boolean = false,
    ) => {
        if (usesExternalEvaluation) {
            return;
        }
        setEvaluationRunning(true);
        setRuleValidationError(undefined);
        let ruleOperatorNodes = _ruleOperatorNodes;
        if (evaluatedSubTreeNode.current) {
            ruleOperatorNodes = evaluationUtils.getSubTreeNodes(ruleOperatorNodes, evaluatedSubTreeNode.current);
        }
        try {
            const currentRuleBlockModel = createCurrentRuleBlockModel(
                ruleOperatorNodes,
                originalTask,
                getPorts(),
                getEvaluationInputExamples(),
            );
            const result = await requestRuleBlockEvaluation(projectId, ruleBlockTaskId, currentRuleBlockModel);
            setEvaluationResult(result.data ?? []);
        } catch (ex) {
            if (ex.isRuleValidationError) {
                setRuleValidationError(ex);
            } else {
                registerError(
                    "RuleBlockEvaluation.startEvaluation",
                    t("taskViews.ruleBlock.errors.evaluate"),
                    ex,
                    { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
                );
            }
        } finally {
            setEvaluationRunning(false);
        }
    }, [getEvaluationInputExamples, getPorts, projectId, registerError, ruleBlockTaskId, t, usesExternalEvaluation]);

    const registerForEvaluationResults = React.useCallback((
        ruleOperatorId: string,
        evaluationUpdate: (evaluationValues: EvaluationResultType | undefined) => void,
    ) => {
        nodeUpdateCallbacks.current.set(ruleOperatorId, evaluationUpdate);
        evaluationUpdate(evaluationValuesForOperator(ruleOperatorId, evaluationResultsShownRef.current));
    }, [evaluationValuesForOperator]);

    const unregisterForEvaluationResults = React.useCallback((ruleOperatorId: string) => {
        nodeUpdateCallbacks.current.delete(ruleOperatorId);
    }, []);

    const createRuleEditorEvaluationComponent = React.useCallback((ruleOperatorId: string): React.JSX.Element => {
        const noResultMsg =
            getInputExamples().length > 0
                ? t("taskViews.ruleBlock.evaluation.noResults")
                : t(
                    "taskViews.ruleBlock.evaluation.noInputExamples",
                    "No input examples exist yet. Example values can be added via the evaluation menu or input port node menu.",
                );
        return (
            <LinkRuleNodeEvaluation
                ruleOperatorId={ruleOperatorId}
                registerForEvaluationResults={registerForEvaluationResults}
                unregister={() => unregisterForEvaluationResults(ruleOperatorId)}
                numberOfLinksToShow={numberOfEntitiesToShow}
                noResultMsg={noResultMsg}
            />
        );
    }, [getInputExamples, numberOfEntitiesToShow, registerForEvaluationResults, t, unregisterForEvaluationResults]);

    const clearRuleValidationError = React.useCallback(() => {
        setRuleValidationError(undefined);
    }, []);

    const selectedEvaluationExampleCount = getSelectedEvaluationExampleIds().length;

    const evaluationConfigMenu: RuleEditorEvaluationConfigMenu | undefined = React.useMemo(() =>
        !usesExternalEvaluation && onOpenExampleValuesDialog
            ? {
                  "data-test-id": "rule-block-evaluation-config-menu",
                  badge: selectedEvaluationExampleCount > 0 ? selectedEvaluationExampleCount : undefined,
                  tooltip:
                      selectedEvaluationExampleCount > 0
                          ? t("taskViews.ruleBlock.examples.dialog.selectionActiveTooltip", {
                                defaultValue: "Show more options. Evaluation is restricted to {{count}} selected examples.",
                                count: selectedEvaluationExampleCount,
                            })
                          : t("common.action.moreOptions", "Show more options"),
                  menuItems: [
                      {
                          "data-test-id": "rule-block-open-example-values",
                          icon: "item-settings" as const,
                          action: () => onOpenExampleValuesDialog(),
                          tooltip: t("taskViews.ruleBlock.exampleValues"),
                      },
                  ],
              }
            : undefined,
    [onOpenExampleValuesDialog, selectedEvaluationExampleCount, t, usesExternalEvaluation]);

    const evaluationContextValue = React.useMemo(
        () => ({
            supportsEvaluation: true,
            supportsQuickEvaluation: false,
            startEvaluation,
            createRuleEditorEvaluationComponent,
            evaluationRunning,
            toggleEvaluationResults,
            evaluationScore: undefined,
            evaluationResultsShown,
            ruleValidationError,
            clearRuleValidationError,
            fetchTriggerEvaluationFunction: () => {},
            evaluationConfigMenu,
            setEvaluationRootNode,
            evaluationRootNode,
            canBeEvaluated,
            ruleType: "transform" as const,
        }),
        [
            canBeEvaluated,
            clearRuleValidationError,
            createRuleEditorEvaluationComponent,
            evaluationConfigMenu,
            evaluationResultsShown,
            evaluationRootNode,
            evaluationRunning,
            ruleValidationError,
            setEvaluationRootNode,
            startEvaluation,
            toggleEvaluationResults,
        ],
    );

    return (
        <RuleEditorEvaluationContext.Provider
            value={evaluationContextValue}
        >
            {children}
        </RuleEditorEvaluationContext.Provider>
    );
};

export default RuleBlockEvaluation;
