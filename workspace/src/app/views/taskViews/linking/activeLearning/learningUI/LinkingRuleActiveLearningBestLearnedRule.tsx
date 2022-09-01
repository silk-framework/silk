import {
    Card,
    CardHeader,
    CardTitle,
    CardOptions,
    CardContent,
    Divider,
    IconButton,
    Notification,
    Tooltip,
    Tag,
    Spacing,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { IEvaluatedReferenceLinksScore, ILinkingRule, OptionallyLabelledParameter } from "../../linking.types";
import { LinkingRuleActiveLearningBestLearnedRuleModal } from "./LinkingRuleActiveLearningBestLearnedRuleModal";
import { VisualBestLinkingRule } from "./VisualBestLinkingRule";
import linkingRuleUtils from "../../LinkingRuleEditor.utils";
import ruleEditorUtils from "../../../../shared/RuleEditor/RuleEditor.utils";
import { IRuleOperatorNode } from "../../../../shared/RuleEditor/RuleEditor.typings";
import { ruleEditorNodeParameterLabel } from "../../../../shared/RuleEditor/model/RuleEditorModel.typings";

interface LinkingRuleActiveLearningBestLearnedRuleProps {
    rule?: OptionallyLabelledParameter<ILinkingRule>;
    score?: IEvaluatedReferenceLinksScore;
    defaultDisplayVisualRule?: boolean;
}

/**
 * Shows information about the currently best learned linking rule.
 * Shows rule visually when expanded.
 */
export const LinkingRuleActiveLearningBestLearnedRule = ({
    rule,
    score,
    defaultDisplayVisualRule = false
}: LinkingRuleActiveLearningBestLearnedRuleProps) => {
    const [displayVisualRule, setDisplayVisualRule] = React.useState(defaultDisplayVisualRule);
    const [displayVisualRuleModal, setDisplayVisualRuleModal] = React.useState(false);
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const { t } = useTranslation();
    const scoreString = (score?.fMeasure ?? "-").replaceAll(".00", ".0");
    const ruleOperators = rule
        ? activeLearningContext.convertRule(linkingRuleUtils.optionallyLabelledParameterToValue(rule))
        : [];

    const RuleSummary = () => {
        const getTabColor = ruleEditorUtils.linkingRuleOperatorTypeColorFunction();
        const aggregateOperators = (
            ops: IRuleOperatorNode[],
            labelFn: (op: IRuleOperatorNode) => string,
            colorFn: (op: IRuleOperatorNode) => string | undefined
        ): OpLabelWithCountAndColor[] => {
            const opMap = new Map<string, number>();
            const colorMap = new Map<string, string | undefined>();
            ops.forEach((op) => {
                const label = labelFn(op);
                const count = opMap.get(label) ?? 0;
                opMap.set(label, count + 1);
                colorMap.set(label, colorFn(op));
            });
            return [...opMap]
                .map((op) => ({ label: op[0], count: op[1], color: colorMap.get(op[0]) }))
                .sort((a, b) => (a.count > b.count ? 1 : -1));
        };
        const aggregatePathOps = (ops: IRuleOperatorNode[]) =>
            aggregateOperators(
                ops,
                (op) => ruleEditorNodeParameterLabel(op.parameters["path"]) ?? "",
                (op) => getTabColor(op.pluginId)
            );
        const aggregateOps = (ops: IRuleOperatorNode[]) =>
            aggregateOperators(
                ops,
                (op) => op.label,
                (op) => getTabColor(op.pluginType)
            );
        const sourcePaths = aggregatePathOps(
            ruleOperators.filter((op) => op.pluginType === "PathInputOperator" && op.pluginId === "sourcePathInput")
        );
        const targetPaths = aggregatePathOps(
            ruleOperators.filter((op) => op.pluginType === "PathInputOperator" && op.pluginId === "targetPathInput")
        );
        const comparisons = aggregateOps(ruleOperators.filter((op) => op.pluginType === "ComparisonOperator"));
        const transformations = aggregateOps(ruleOperators.filter((op) => op.pluginType === "TransformOperator"));
        const aggregations = aggregateOps(ruleOperators.filter((op) => op.pluginType === "AggregationOperator"));
        return (
            <div>
                {[...sourcePaths, ...targetPaths, ...comparisons, ...transformations, ...aggregations].map((op) => {
                    return (
                        <>
                            <Tag key={op.label} backgroundColor={op.color}>
                                {op.label}
                                {op.count > 1 ? ` (${op.count})` : ""}
                            </Tag>
                            <Spacing size={"tiny"} vertical={true} />
                        </>
                    );
                })}
            </div>
        );
    };

    const BestLearnedRuleHeader = () => {
        return (
            <CardHeader>
                <CardTitle>
                    <Tooltip content={t("ActiveLearning.bestLearnedRule.titleTooltip")}>
                        {t("ActiveLearning.bestLearnedRule.title", { score: scoreString })}
                    </Tooltip>
                </CardTitle>
                <CardOptions>
                    {rule ? (
                        <IconButton
                            data-test-id={"open-best-learned-rule-btn"}
                            name={"toggler-maximize"}
                            onClick={(e) => {
                                e.stopPropagation();
                                setDisplayVisualRuleModal(true);
                            }}
                            text={t("ActiveLearning.bestLearnedRule.showRuleFullscreen")}
                        />
                    ) : null}
                    <IconButton
                        data-test-id={"best-learned-rule-toggler-btn"}
                        name={displayVisualRule ? "toggler-showless" : "toggler-showmore"}
                        text={
                            displayVisualRule
                                ? t("ActiveLearning.bestLearnedRule.hideRule")
                                : t("ActiveLearning.bestLearnedRule.showRule")
                        }
                        onClick={() => {
                            setDisplayVisualRule(!displayVisualRule);
                        }}
                    />
                </CardOptions>
            </CardHeader>
        );
    };

    const Info = () => {
        return <Notification neutral={true} message={t("ActiveLearning.bestLearnedRule.noRule")} />;
    };

    return rule ? (
        <>
            <Card elevation={0} data-test-id={"best-learned-rule-visual"}>
                <BestLearnedRuleHeader />
                <Divider />
                <CardContent>
                    {displayVisualRule ? (
                        <>
                            {activeLearningContext.linkTask && rule && (
                                <VisualBestLinkingRule rule={rule} />
                            )}
                        </>
                    ) : (
                        <>
                            {rule ? <RuleSummary /> : "No rule learned, yet."}
                        </>
                    )}
                </CardContent>
            </Card>
            {activeLearningContext.linkTask && rule && displayVisualRuleModal ? (
                <LinkingRuleActiveLearningBestLearnedRuleModal
                    rule={rule}
                    onClose={() => setDisplayVisualRuleModal(false)}
                />
            ) : null}
        </>
    ) : (
        <Info />
    );
};

interface OpLabelWithCountAndColor {
    label: string;
    count: number;
    color: string | undefined;
}
