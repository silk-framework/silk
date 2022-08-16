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
import { LinkingRuleEditor, LinkingRuleEditorOptionalContext } from "../../LinkingRuleEditor";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { IEvaluatedReferenceLinksScore, ILinkingRule, OptionallyLabelledParameter } from "../../linking.types";

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
    const scoreString = score?.fMeasure ?? "-";
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
                        <h1>{t("ActiveLearning.bestLearnedRule.title", { score: scoreString })}</h1>
                    </OverviewItemLine>
                    <OverviewItemLine>{rule ? "Show rule details" : "No rule learned, yet."}</OverviewItemLine>
                </OverviewItemDescription>
                <OverviewItemActions>
                    {rule ? (
                        <IconButton
                            data-test-id={"best-learned-rule-toggler-btn"}
                            name={displayVisualRule ? "toggler-showless" : "toggler-showmore"}
                            text={
                                displayVisualRule
                                    ? t("ActiveLearning.bestLearnedRule.hideRule")
                                    : t("ActiveLearning.bestLearnedRule.showRule")
                            }
                        />
                    ) : null}
                </OverviewItemActions>
            </OverviewItem>
        );
    };

    const Info = () => {
        return <Notification neutral={true} message={t("ActiveLearning.bestLearnedRule.noRule")} />;
    };

    const VisualRule = () => {
        return activeLearningContext.linkTask && rule ? (
            <LinkingRuleEditorOptionalContext.Provider
                value={{
                    linkingRule: {
                        ...activeLearningContext.linkTask,
                        parameters: {
                            ...activeLearningContext.linkTask.parameters,
                            rule: rule,
                        },
                    },
                    showRuleOnly: true,
                }}
            >
                <LinkingRuleEditor
                    projectId={activeLearningContext.projectId}
                    linkingTaskId={activeLearningContext.linkingTaskId}
                />
            </LinkingRuleEditorOptionalContext.Provider>
        ) : null;
    };

    return rule ? (
        <Card isOnlyLayout elevation={0} data-test-id={"best-learned-rule-visual"}>
            <BestLearnedRule />
            {rule && displayVisualRule && (
                <>
                    <Divider />
                    <WhiteSpaceContainer paddingTop="small" paddingRight="tiny" paddingLeft="tiny">
                        <VisualRule />
                    </WhiteSpaceContainer>
                </>
            )}
        </Card>
    ) : (
        <Info />
    );
};
