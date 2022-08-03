import {
    Card,
    Divider,
    IconButton,
    Notification,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    WhiteSpaceContainer,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

interface LinkingRuleActiveLearningBestLearnedRuleProps {
    rule?: any; // TODO: Proper type
}

/**
 * Shows information about the currently best learned linking rule.
 * Shows rule visually when expanded.
 */
export const LinkingRuleActiveLearningBestLearnedRule = ({ rule }: LinkingRuleActiveLearningBestLearnedRuleProps) => {
    const [displayVisualRule, setDisplayVisualRule] = React.useState(false);
    const { t } = useTranslation();
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
                        <h1>{t("ActiveLearning.bestLearnedRule.title", { score: "TODO: Insert score in percent" })}</h1>
                    </OverviewItemLine>
                    <OverviewItemLine>TODO: Show rule details</OverviewItemLine>
                </OverviewItemDescription>
                <OverviewItemActions>
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
            {rule && displayVisualRule && (
                <>
                    <Divider />
                    <WhiteSpaceContainer paddingTop="small" paddingRight="tiny" paddingLeft="tiny">
                        TODO: Display rule visually
                    </WhiteSpaceContainer>
                </>
            )}
        </Card>
    ) : (
        <Info />
    );
};
