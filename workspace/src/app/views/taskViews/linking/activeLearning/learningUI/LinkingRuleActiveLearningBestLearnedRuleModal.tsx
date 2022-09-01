import { LinkingRuleEditor, LinkingRuleEditorOptionalContext } from "../../LinkingRuleEditor";
import React from "react";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { ILinkingRule, OptionallyLabelledParameter } from "../../linking.types";
import { SimpleDialog, IconButton } from "@eccenca/gui-elements";
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
            size="fullscreen"
            headerOptions={[
                <IconButton
                    onClick={onClose}
                    data-test-id={"close-btn"}
                    text={t("common.action.close")}
                    name="navigation-close"
                />
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
                <div style={{ position: "relative", height: "100%" }}>
                    <LinkingRuleEditor
                        projectId={activeLearningContext.projectId}
                        linkingTaskId={activeLearningContext.linkingTaskId}
                    />
                </div>
            </LinkingRuleEditorOptionalContext.Provider>
        </SimpleDialog>
    ) : null;
};
