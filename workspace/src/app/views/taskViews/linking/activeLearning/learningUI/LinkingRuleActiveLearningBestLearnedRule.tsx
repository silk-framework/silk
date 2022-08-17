import {
    Card,
    Divider,
    IconButton,
    Notification,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    Tooltip,
    WhiteSpaceContainer,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { IEvaluatedReferenceLinksScore, ILinkingRule, OptionallyLabelledParameter } from "../../linking.types";
import { LinkingRuleActiveLearningBestLearnedRuleModal } from "./LinkingRuleActiveLearningBestLearnedRuleModal";
import { VisualBestLinkingRule } from "./VisualBestLinkingRule";

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
    const [displayVisualRuleModal, setDisplayVisualRuleModal] = React.useState(false);
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
                    <OverviewItemLine>{rule ? "TODO: Show rule details" : "No rule learned, yet."}</OverviewItemLine>
                </OverviewItemDescription>
                <OverviewItemActions>
                    {rule ? (
                        <IconButton
                            data-test-id={"open-best-learned-rule-btn"}
                            name={"item-viewdetails"}
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
                    />
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
            {activeLearningContext.linkTask && rule && displayVisualRule && (
                <>
                    <Divider />
                    <WhiteSpaceContainer paddingTop="small" paddingRight="tiny" paddingLeft="tiny">
                        <VisualBestLinkingRule rule={rule} />
                    </WhiteSpaceContainer>
                </>
            )}
            {activeLearningContext.linkTask && rule && displayVisualRuleModal ? (
                <LinkingRuleActiveLearningBestLearnedRuleModal
                    rule={rule}
                    onClose={() => setDisplayVisualRuleModal(false)}
                />
            ) : null}
        </Card>
    ) : (
        <Info />
    );
};
