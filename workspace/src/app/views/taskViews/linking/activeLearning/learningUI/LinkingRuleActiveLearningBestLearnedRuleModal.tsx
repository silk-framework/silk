import { LinkingRuleEditor, LinkingRuleEditorOptionalContext } from "../../LinkingRuleEditor";
import React from "react";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { ILinkingRule, OptionallyLabelledParameter } from "../../linking.types";
import { Button, SimpleDialog } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

interface Props {
    rule: OptionallyLabelledParameter<ILinkingRule>;
    onClose: () => any;
}

/** Displays the given rule visually in a modal. */
export const LinkingRuleActiveLearningBestLearnedRuleModal = ({ rule, onClose }: Props) => {
    const [t] = useTranslation();
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);

    return activeLearningContext.linkTask && rule ? (
        <SimpleDialog
            data-test-id={"active-learning-best-rule-visual"}
            onClose={onClose}
            title={t("ActiveLearning.bestLearnedRule.visualRuleTitle")}
            isOpen={true}
            startInFullScreenMode={true}
            actions={[
                <Button onClick={onClose} data-test-id={"close-btn"}>
                    {t("common.action.close")}
                </Button>,
            ]}
        >
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
                <div style={{ position: "relative", height: "1080px" }}>
                    <LinkingRuleEditor
                        projectId={activeLearningContext.projectId}
                        linkingTaskId={activeLearningContext.linkingTaskId}
                    />
                </div>
            </LinkingRuleEditorOptionalContext.Provider>
        </SimpleDialog>
    ) : null;
};
