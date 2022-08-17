import {
    Card,
    IconButton,
    Notification,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    Tooltip,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { IEvaluatedReferenceLinksScore, ILinkingRule, OptionallyLabelledParameter } from "../../linking.types";
import { LinkingRuleActiveLearningBestLearnedRuleModal } from "./LinkingRuleActiveLearningBestLearnedRuleModal";

interface LinkingRuleActiveLearningBestLearnedRuleProps {
    rule?: OptionallyLabelledParameter<ILinkingRule>;
    score?: IEvaluatedReferenceLinksScore;
}

/**
 * Shows information about the currently best learned linking rule.
 * Shows rule visually when expanded.
 */
export const LinkingRuleActiveLearningBestLearnedRule = ({
    rule,
    score,
}: LinkingRuleActiveLearningBestLearnedRuleProps) => {
    const [displayVisualRule, setDisplayVisualRule] = React.useState(false);
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const { t } = useTranslation();
    const scoreString = (score?.fMeasure ?? "-").replaceAll(".00", ".0");
    const BestLearnedRule = () => {
        return (
            <OverviewItem
                hasSpacing
                onClick={() => {
                    setDisplayVisualRule(!displayVisualRule);
                }}
            >
                <OverviewItemDescription>
                    <OverviewItemLine large>
                        <h1>
                            <Tooltip content={t("ActiveLearning.bestLearnedRule.titleTooltip")}>
                                {t("ActiveLearning.bestLearnedRule.title", { score: scoreString })}
                            </Tooltip>
                        </h1>
                    </OverviewItemLine>
                    <OverviewItemLine>{rule ? "Show rule details" : "No rule learned, yet."}</OverviewItemLine>
                </OverviewItemDescription>
                <OverviewItemActions>
                    {rule ? (
                        <IconButton
                            data-test-id={"open-best-learned-rule-btn"}
                            name={"item-viewdetails"}
                            text={t("ActiveLearning.bestLearnedRule.showRule")}
                        />
                    ) : null}
                </OverviewItemActions>
            </OverviewItem>
        );
    };

    const Info = () => {
        return <Notification neutral={true} message={t("ActiveLearning.bestLearnedRule.noRule")} />;
    };

    return rule ? (
        <Card isOnlyLayout elevation={0} data-test-id={"best-learned-rule-visual"}>
            <BestLearnedRule />
            {activeLearningContext.linkTask && rule && displayVisualRule ? (
                <LinkingRuleActiveLearningBestLearnedRuleModal
                    rule={rule}
                    onClose={() => setDisplayVisualRule(false)}
                />
            ) : null}
        </Card>
    ) : (
        <Info />
    );
};
