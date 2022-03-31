/** Component that handles the linking rule (inline) evaluation. */
import { RuleEditorEvaluationContext } from "../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import React, { ReactElement } from "react";
import { IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";
import { RuleEditorProps } from "../../../shared/RuleEditor/RuleEditor";
import { TaskPlugin } from "@ducks/shared/typings";
import {
    AggregationConfidence,
    ComparisonConfidence,
    IEntityLink,
    IEvaluatedReferenceLinksScore,
    IEvaluationNode,
    IEvaluationValue,
    ILinkingTaskParameters,
} from "../linking.types";
import { IPluginDetails } from "@ducks/common/typings";
import utils from "../LinkingRuleEditor.utils";
import { evaluateLinkingRule, evaluateLinkingRuleAgainstReferenceEntities } from "../LinkingRuleEditor.requests";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { LinkRuleNodeEvaluation } from "./LinkRuleNodeEvaluation";
import { queryParameterValue } from "../../../../utils/basicUtils";

type EvaluationChildType = ReactElement<RuleEditorProps<TaskPlugin<ILinkingTaskParameters>, IPluginDetails>>;

interface LinkingRuleEvaluationProps {
    projectId: string;
    linkingTaskId: string;
    /** The number of links that should be shown inline. */
    numberOfLinkToShow: number;
    /** The children that should be able to use this linking rule evaluation component. */
    children: EvaluationChildType;
}

type EvaluatedEntityLink = IEntityLink & { type: "positive" | "negative" | "unlabelled" };

const REFERENCE_LINK_URL_PARAMETER = "referenceLinksUrl";

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
    const [evaluationResultMap] = React.useState<Map<string, string[][]>>(new Map());
    const [evaluationResultEntities] = React.useState<[string, string][]>([]);
    const [evaluationScore, setEvaluationScore] = React.useState<IEvaluatedReferenceLinksScore | undefined>(undefined);
    const [evaluatesQuickly, setEvaluatesQuickly] = React.useState(false);
    const [nodeUpdateCallbacks] = React.useState(new Map<string, (evaluationValues: string[][] | undefined) => any>());
    const [referenceLinksUrl, setReferenceLinksUrl] = React.useState<string | undefined>(undefined);
    const [evaluationResultsShown, setEvaluationResultsShown] = React.useState<boolean>(false);
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();

    React.useEffect(() => {
        setEvaluationResult([]);
        clearEntities();
        evaluationResultMap.clear();
        nodeUpdateCallbacks.clear();
        setReferenceLinksUrl(queryParameterValue(REFERENCE_LINK_URL_PARAMETER)[0]);
    }, [projectId, linkingTaskId]);

    React.useEffect(() => {
        clearEntities();
        evaluationResult.forEach((link) => evaluationResultEntities.push([link.source, link.target]));
        const valueMaps = evaluationResult.map((link) => linkToValueMap(link));
        nodeUpdateCallbacks.forEach((updateCallback, operatorId) => {
            const evaluationValues = valueMaps.map((valueMap) => {
                const operatorLinkEvaluationValues = valueMap.get(operatorId) ?? [];
                return operatorLinkEvaluationValues;
            });
            evaluationResultMap.set(operatorId, evaluationValues);
            updateCallback(evaluationValues);
        });
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

    const clearEntities = () => evaluationResultEntities.splice(0, evaluationResultEntities.length);

    /** Turns an evaluation tree into a map operatorId => evaluation value */
    const linkToValueMap = (link: EvaluatedEntityLink): Map<string, string[]> => {
        const valueMap = new Map<string, string[]>();
        const traverseEvaluationValueTree = (node: IEvaluationValue) => {
            valueMap.set(node.operatorId, node.values);
            node.children && node.children.forEach((c) => traverseEvaluationValueTree(c));
        };
        const traverseEvaluationTree = (node: IEvaluationNode) => {
            if ((node as AggregationConfidence).children) {
                valueMap.set(node.operatorId, [`Score: ${node.score ?? ""}`]);
                (node as AggregationConfidence).children.forEach((n) => traverseEvaluationTree(n));
            } else {
                const comparison = node as ComparisonConfidence;
                valueMap.set(comparison.operatorId, [`Score: ${node.score ?? ""}`]);
                traverseEvaluationValueTree(comparison.sourceValue);
                traverseEvaluationValueTree(comparison.targetValue);
            }
        };
        link.ruleValues && traverseEvaluationTree(link.ruleValues);
        return valueMap;
    };

    /** Start an evaluation of the linkage rule. */
    const startEvaluation = async (
        ruleOperatorNodes: IRuleOperatorNode[],
        originalTask: any,
        quickEvaluationOnly: boolean = false
    ) => {
        setEvaluationRunning(true);
        try {
            const ruleTree = utils.constructLinkageRuleTree(ruleOperatorNodes);
            const linkSpec = originalTask as TaskPlugin<ILinkingTaskParameters>;
            const linkageRule = linkSpec.parameters.rule;
            const newLinkageRule = { ...linkageRule, operator: ruleTree };
            const result = (
                await evaluateLinkingRuleAgainstReferenceEntities(
                    projectId,
                    linkingTaskId,
                    newLinkageRule,
                    numberOfLinkToShow
                )
            ).data;
            if (result.positive.length === 0 && result.negative.length === 0 && !quickEvaluationOnly) {
                // Fallback to slower linking evaluation
                setEvaluatesQuickly(false);
                const links = (await evaluateLinkingRule(projectId, linkingTaskId, newLinkageRule, numberOfLinkToShow))
                    .data;
                setEvaluationResult(links.slice(0, numberOfLinkToShow).map((l) => ({ ...l, type: "unlabelled" })));
                setEvaluationScore(undefined);
            } else {
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
            }
        } catch (ex) {
            if (ex.isFetchError) {
                registerError(
                    "LinkingRuleEvaluation.startEvaluation",
                    t("taskViews.linkRulesEditor.errors.startEvaluation.msg"),
                    ex
                );
            } else {
                console.warn("Could not fetch evaluation results!", ex);
            }
        } finally {
            setEvaluationRunning(false);
        }
    };

    /** Called by a rule operator node to register for evaluation updates. */
    const registerForEvaluationResults = (
        ruleOperatorId: string,
        evaluationUpdate: (evaluationValues: string[][] | undefined) => void
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
            />
        );
    };
    return (
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
            }}
        >
            {children}
        </RuleEditorEvaluationContext.Provider>
    );
};
