/** Component that handles the linking rule (inline) evaluation. */
import {
    RuleEditorEvaluationCallbackContext,
    RuleEditorEvaluationContext
} from "../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import React, { ReactElement } from "react";
import {
    IRuleOperatorNode,
    RULE_EDITOR_NOTIFICATION_INSTANCE,
    RuleValidationError,
} from "../../../shared/RuleEditor/RuleEditor.typings";
import { RuleEditorProps } from "../../../shared/RuleEditor/RuleEditor";
import { TaskPlugin } from "@ducks/shared/typings";
import {
    IEntityLink,
    IEvaluatedReferenceLinks,
    IEvaluatedReferenceLinksScore,
    ILinkingRule,
    ILinkingTaskParameters,
} from "../linking.types";
import { IPluginDetails } from "@ducks/common/typings";
import editorUtils from "../LinkingRuleEditor.utils";
import { evaluateLinkingRule, evaluateLinkingRuleAgainstReferenceEntities } from "../LinkingRuleEditor.requests";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { LinkRuleNodeEvaluation } from "./LinkRuleNodeEvaluation";
import { queryParameterValue } from "../../../../utils/basicUtils";
import utils from "./LinkingRuleEvaluation.utils";
import { FetchError } from "../../../../services/fetch/responseInterceptor";
import { ruleEditorNodeParameterValue } from "../../../shared/RuleEditor/model/RuleEditorModel.typings";
import { PathNotInCacheModal } from "../../shared/evaluations/PathNotInCacheModal";
import evaluationUtils from "../../shared/evaluations/evaluationOperations";
import { SampleError } from "../../../shared/SampleError/SampleError";
import {DIErrorTypes} from "@ducks/error/typings";

type EvaluationChildType = ReactElement<RuleEditorProps<TaskPlugin<ILinkingTaskParameters>, IPluginDetails>>;

interface LinkingRuleEvaluationProps {
    projectId: string;
    linkingTaskId: string;
    /** The number of links that should be shown inline. */
    numberOfLinkToShow: number;
    /** The children that should be able to use this linking rule evaluation component. */
    children: EvaluationChildType;
}

export type EvaluatedEntityLink = IEntityLink & { type: "positive" | "negative" | "unlabelled" };

const REFERENCE_LINK_URL_PARAMETER = "referenceLinksUrl";

export type EvaluationResultType = Array<{ value: string[]; error?: SampleError | null }>;

/** Linking rule evaluation component.
 * Shows (inline) evaluation of the currently shown linking rule.
 */
export const LinkingRuleEvaluation = ({
    projectId,
    linkingTaskId,
    numberOfLinkToShow,
    children,
}: LinkingRuleEvaluationProps) => {
    const [evaluationRunning, setEvaluationRunning] = React.useState<boolean>(false);
    const [evaluationResult, setEvaluationResult] = React.useState<EvaluatedEntityLink[]>([]);
    const [evaluationResultMap] = React.useState<Map<string, EvaluationResultType>>(new Map());
    const [evaluationResultEntities] = React.useState<[string, string][]>([]);
    const [evaluationScore, setEvaluationScore] = React.useState<IEvaluatedReferenceLinksScore | undefined>(undefined);
    const [evaluatesQuickly, setEvaluatesQuickly] = React.useState(false);
    const [nodeUpdateCallbacks] = React.useState(
        new Map<string, (evaluationValues: EvaluationResultType | undefined) => any>()
    );
    const [referenceLinksUrl, setReferenceLinksUrl] = React.useState<string | undefined>(undefined);
    const [evaluationResultsShown, setEvaluationResultsShown] = React.useState<boolean>(false);
    const [ruleValidationError, setRuleValidationError] = React.useState<RuleValidationError | undefined>(undefined);
    const [evaluatedRuleOperatorIds, setEvaluatedRuleOperatorIds] = React.useState<string[]>([]);
    const [errorModalEnabled, setErrorModalEnabled] = React.useState(true);
    const popupErrorsEnabled = React.useRef(true)
    popupErrorsEnabled.current = errorModalEnabled
    // The root node of the sub-tree that will be evaluated
    const evaluatedSubTreeNode = React.useRef<string>();

    const [pathNotInCacheValidationError, setPathNotInCacheValidationError] = React.useState<
        { path: string; toTarget: boolean } | undefined
    >(undefined);
    /** Contains the function to trigger an evaluation. */
    const triggerEvaluation = React.useRef<(() => any) | undefined>(undefined);
    const { registerError: _registerError } = useErrorHandler();
    const [t] = useTranslation();

    const registerError = (errorKey: string, error: DIErrorTypes) => {
        _registerError(errorKey, t(errorKey), error, {
            errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE,
            notAutoOpen: !popupErrorsEnabled.current
        })
    }

    const clearRuleValidationErrors = () => {
        setRuleValidationError(undefined);
        setPathNotInCacheValidationError(undefined);
    };

    const enableErrorModal = React.useCallback((enable: boolean) => {
        setErrorModalEnabled(enable)
    }, [])

    React.useEffect(() => {
        setEvaluationResult([]);
        clearEntities();
        evaluationResultMap.clear();
        nodeUpdateCallbacks.clear();
        setReferenceLinksUrl(queryParameterValue(REFERENCE_LINK_URL_PARAMETER)[0]);
    }, [projectId, linkingTaskId]);

    React.useEffect(() => {
        clearEntities();
        try {
            evaluationResult.forEach((link) => evaluationResultEntities.push([link.source, link.target]));
            const valueMaps = evaluationResult.map((link) => utils.linkToValueMap(link));
            nodeUpdateCallbacks.forEach((updateCallback, operatorId) => {
                const evaluationValues = valueMaps.map((valueMap) => {
                    return valueMap.get(operatorId) ?? { value: [] };
                });
                evaluationResultMap.set(operatorId, evaluationValues);
                if (evaluationResultsShown) {
                    updateCallback(!evaluatedRuleOperatorIds.includes(operatorId) ? undefined : evaluationValues);
                }
            });
        } catch (ex) {
            console.warn("Unexpected error has occurred while processing the evaluation result.", ex);
        }
    }, [evaluationResult, evaluationResultsShown, evaluatedRuleOperatorIds.join("|")]);

    const toggleEvaluationResults = (show: boolean) => {
        if (show) {
            nodeUpdateCallbacks.forEach((updateCallback, ruleOperatorId) => {
                updateCallback(
                    !evaluatedRuleOperatorIds.includes(ruleOperatorId)
                        ? undefined
                        : evaluationResultMap.get(ruleOperatorId)
                );
            });
        } else {
            nodeUpdateCallbacks.forEach((updateCallback, ruleOperatorId) => {
                updateCallback(undefined);
            });
        }
        setEvaluationResultsShown(show);
    };

    const clearEntities = () => evaluationResultEntities.splice(0, evaluationResultEntities.length);

    const fetchReferenceLinksEvaluation: (
        linkageRule: ILinkingRule
    ) => Promise<IEvaluatedReferenceLinks | undefined> = async (linkageRule: ILinkingRule) => {
        try {
            const result = await evaluateLinkingRuleAgainstReferenceEntities(
                projectId,
                linkingTaskId,
                linkageRule,
                numberOfLinkToShow
            );
            return result.data;
        } catch (ex) {
            if (ex.isFetchError && (ex as FetchError).httpStatus !== 409) {
                registerError("taskViews.linkRulesEditor.errors.fetchReferenceLinks.msg", ex);
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
        return nodeType === "aggregator" || nodeType === "comparator";
    }, []);

    /** Start an evaluation of the linkage rule. */
    const startEvaluation = async (
        _ruleOperatorNodes: IRuleOperatorNode[],
        originalTask: any,
        quickEvaluationOnly: boolean = false
    ) => {
        setEvaluationRunning(true);
        let ruleOperatorNodes = _ruleOperatorNodes;
        if (evaluatedSubTreeNode.current) {
            ruleOperatorNodes = evaluationUtils.getSubTreeNodes(ruleOperatorNodes, evaluatedSubTreeNode.current);
        }
        setEvaluatedRuleOperatorIds(ruleOperatorNodes.map((r) => r.nodeId));
        clearRuleValidationErrors();
        try {
            const ruleTree = editorUtils.constructLinkageRuleTree(ruleOperatorNodes);
            const linkSpec = originalTask as TaskPlugin<ILinkingTaskParameters>;
            const linkageRule = editorUtils.optionallyLabelledParameterToValue(linkSpec.parameters.rule);
            const newLinkageRule = { ...linkageRule, operator: ruleTree };
            const result = await fetchReferenceLinksEvaluation(newLinkageRule);
            if (!quickEvaluationOnly && (!result || (result.positive.length === 0 && result.negative.length === 0))) {
                // Fallback to slower linking evaluation
                setEvaluatesQuickly((previous) => !result);
                const links = (await evaluateLinkingRule(projectId, linkingTaskId, newLinkageRule, numberOfLinkToShow))
                    .data;
                setEvaluationResult(links.slice(0, numberOfLinkToShow).map((l) => ({ ...l, type: "unlabelled" })));
                setEvaluationScore(undefined);
            } else if (result) {
                // Fast reference links evaluation available
                setEvaluatesQuickly(true);
                setEvaluationScore(result.evaluationScore);
                const negativeLinks: EvaluatedEntityLink[] = result.negative
                    .slice(0, Math.max(Math.floor(numberOfLinkToShow / 2), numberOfLinkToShow - result.positive.length))
                    .map((l) => ({ ...l, type: "negative" }));
                const positiveLinks: EvaluatedEntityLink[] = result.positive
                    .slice(0, Math.max(Math.ceil(numberOfLinkToShow / 2), numberOfLinkToShow - result.negative.length))
                    .map((l) => ({ ...l, type: "positive" }));
                setEvaluationResult([...positiveLinks, ...negativeLinks]);
            } else {
                setEvaluationScore(undefined);
                setEvaluationResult([]);
            }
        } catch (ex) {
            if (ex.isFetchError) {
                if ((ex as FetchError).httpStatus === 409) {
                    const path = (ex as FetchError).errorResponse.detail;
                    const pathNode = ruleOperatorNodes.find(
                        (op) =>
                            op.pluginType === "PathInputOperator" &&
                            ruleEditorNodeParameterValue(op.parameters.path) === path
                    );
                    if (pathNode) {
                        setPathNotInCacheValidationError({ path, toTarget: pathNode.pluginId === "targetPathInput" });
                    }
                } else {
                    registerError("taskViews.linkRulesEditor.errors.startEvaluation.msg", ex);
                }
            } else if (ex.isRuleValidationError) {
                setRuleValidationError(ex);
            } else {
                registerError("taskViews.linkRulesEditor.errors.beforeStartEvaluation.msg", ex);
            }
        } finally {
            setEvaluationRunning(false);
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
                referenceLinksUrl={referenceLinksUrl}
                numberOfLinksToShow={numberOfLinkToShow}
            />
        );
    };

    const fetchTriggerEvaluationFunction = React.useCallback((trigggerFn: () => any) => {
        triggerEvaluation.current = trigggerFn;
    }, []);

    return <RuleEditorEvaluationCallbackContext.Provider value={{
        enableErrorModal
    }}>
        <RuleEditorEvaluationContext.Provider
            value={{
                supportsEvaluation: true,
                supportsQuickEvaluation: evaluatesQuickly,
                startEvaluation,
                createRuleEditorEvaluationComponent,
                evaluationRunning,
                toggleEvaluationResults,
                evaluationScore,
                evaluationResultsShown,
                referenceLinksUrl,
                ruleValidationError,
                clearRuleValidationError: clearRuleValidationErrors,
                fetchTriggerEvaluationFunction,
                setEvaluationRootNode,
                evaluationRootNode,
                canBeEvaluated,
                ruleType: "linking",
            }}
        >
            {errorModalEnabled && pathNotInCacheValidationError && triggerEvaluation.current && (
                <PathNotInCacheModal
                    projectId={projectId}
                    linkingTaskId={linkingTaskId}
                    toTarget={pathNotInCacheValidationError.toTarget}
                    path={pathNotInCacheValidationError.path}
                    onAddPath={() => {
                        clearRuleValidationErrors();
                        triggerEvaluation.current?.();
                    }}
                    onClose={() => clearRuleValidationErrors()}
                />
            )}
            {children}
        </RuleEditorEvaluationContext.Provider>
    </RuleEditorEvaluationCallbackContext.Provider>
};
