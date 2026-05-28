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
    RuleEditorEvaluationContext,
} from "../../shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import { LinkRuleNodeEvaluation } from "../linking/evaluation/LinkRuleNodeEvaluation";
import { EvaluationResultType } from "../linking/evaluation/LinkingRuleEvaluation";
import evaluationUtils from "../shared/evaluations/evaluationOperations";
import ruleUtils from "../shared/rules/rule.utils";
import { EvaluatedTransformEntity } from "../transform/transform.types";
import { IRuleBlockInputExample, IRuleBlockModel, IRuleBlockPort, IRuleBlockTaskParameters } from "./ruleBlock.types";
import { requestRuleBlockEvaluation } from "./ruleBlock.requests";
import { IProjectTask } from "@ducks/shared/typings";
import { SampleError } from "../../shared/SampleError/SampleError";

type RuleBlockTaskData = IProjectTask<IRuleBlockTaskParameters>;
type EvaluationChildType = ReactElement<RuleEditorProps<RuleBlockTaskData, IPluginDetails>>;

interface RuleBlockEvaluationProps {
    projectId: string;
    ruleBlockTaskId: string;
    numberOfEntitiesToShow: number;
    getPorts: () => IRuleBlockPort[];
    children: EvaluationChildType;
}

const createCurrentRuleBlockModel = (
    ruleOperatorNodes: IRuleOperatorNode[],
    originalTask: RuleBlockTaskData,
    ports: IRuleBlockPort[],
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
    const sortedPorts = [...ports].sort(
        (left, right) => left.displayOrder - right.displayOrder || left.id.localeCompare(right.id),
    );
    const mockedInputExamples: IRuleBlockInputExample[] = [
        {
            id: "mock-example-1",
            inputs: Object.fromEntries(
                sortedPorts.map((port, index) => [port.id, [`Port ${index + 1} value`]] as const),
            ),
        },
    ];
    return {
        ports,
        inputExamples: mockedInputExamples,
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
    children,
}: RuleBlockEvaluationProps) => {
    const [evaluationRunning, setEvaluationRunning] = React.useState<boolean>(false);
    const [evaluationResult, setEvaluationResult] = React.useState<EvaluatedTransformEntity[]>([]);
    const [evaluationResultMap] = React.useState<Map<string, EvaluationResultType>>(new Map());
    const [evaluationResultsShown, setEvaluationResultsShown] = React.useState<boolean>(false);
    const [nodeUpdateCallbacks] = React.useState(
        new Map<string, (evaluationValues: EvaluationResultType | undefined) => any>(),
    );
    const [ruleValidationError, setRuleValidationError] = React.useState<RuleValidationError | undefined>(undefined);
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();
    const evaluatedSubTreeNode = React.useRef<string>(undefined);

    React.useEffect(() => {
        setEvaluationResult([]);
        evaluationResultMap.clear();
        nodeUpdateCallbacks.clear();
    }, [projectId, ruleBlockTaskId]);

    React.useEffect(() => {
        try {
            const valueMaps = evaluationResult.map((transform) => evaluationTreeToValueMap(transform));
            nodeUpdateCallbacks.forEach((updateCallback, operatorId) => {
                const evaluationValues = valueMaps.map((valueMap) => {
                    return valueMap.get(operatorId) ?? { value: [] };
                });
                evaluationResultMap.set(operatorId, evaluationValues);
                updateCallback(evaluationResultsShown ? evaluationValues : undefined);
            });
        } catch (ex) {
            console.warn("Unexpected error has occurred while processing the rule block evaluation result.", ex);
        }
    }, [evaluationResult, evaluationResultsShown]);

    const toggleEvaluationResults = (show: boolean) => {
        if (show) {
            nodeUpdateCallbacks.forEach((updateCallback, ruleOperatorId) => {
                updateCallback(evaluationResultMap.get(ruleOperatorId));
            });
        } else {
            nodeUpdateCallbacks.forEach((updateCallback) => {
                updateCallback(undefined);
            });
        }
        setEvaluationResultsShown(show);
    };

    const setEvaluationRootNode = React.useCallback((nodeId: string | undefined) => {
        evaluatedSubTreeNode.current = nodeId;
    }, []);

    const evaluationRootNode = React.useCallback(() => {
        return evaluatedSubTreeNode.current;
    }, []);

    const canBeEvaluated = React.useCallback((_nodeType: string | undefined) => {
        return true;
    }, []);

    const startEvaluation = async (
        _ruleOperatorNodes: IRuleOperatorNode[],
        originalTask: RuleBlockTaskData,
        _quickEvaluationOnly: boolean = false,
    ) => {
        setEvaluationRunning(true);
        setRuleValidationError(undefined);
        let ruleOperatorNodes = _ruleOperatorNodes;
        if (evaluatedSubTreeNode.current) {
            ruleOperatorNodes = evaluationUtils.getSubTreeNodes(ruleOperatorNodes, evaluatedSubTreeNode.current);
        }
        try {
            const currentRuleBlockModel = createCurrentRuleBlockModel(ruleOperatorNodes, originalTask, getPorts());
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
    };

    const registerForEvaluationResults = (
        ruleOperatorId: string,
        evaluationUpdate: (evaluationValues: EvaluationResultType | undefined) => void,
    ) => {
        nodeUpdateCallbacks.set(ruleOperatorId, evaluationUpdate);
        evaluationUpdate(evaluationResultMap.get(ruleOperatorId));
    };

    const createRuleEditorEvaluationComponent = (ruleOperatorId: string): React.JSX.Element => {
        return (
            <LinkRuleNodeEvaluation
                ruleOperatorId={ruleOperatorId}
                registerForEvaluationResults={registerForEvaluationResults}
                unregister={() => nodeUpdateCallbacks.delete(ruleOperatorId)}
                numberOfLinksToShow={numberOfEntitiesToShow}
                noResultMsg={t("taskViews.ruleBlock.evaluation.noResults")}
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
                evaluationResultsShown,
                ruleValidationError,
                clearRuleValidationError,
                fetchTriggerEvaluationFunction: () => {},
                setEvaluationRootNode,
                evaluationRootNode,
                canBeEvaluated,
                ruleType: "transform",
            }}
        >
            {children}
        </RuleEditorEvaluationContext.Provider>
    );
};

export default RuleBlockEvaluation;
