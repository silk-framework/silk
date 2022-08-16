import { Button, FieldItem, SimpleDialog, Switch } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { saveActiveLearningResults } from "../LinkingRuleActiveLearning.requests";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import useErrorHandler from "../../../../../hooks/useErrorHandler";

interface LinkingRuleActiveLearningSaveModalProps {
    unsavedReferenceLinks: number;
    unsavedBestRule: boolean;
    onClose: () => any;
}

export const LinkingRuleActiveLearningSaveModal = ({
    unsavedBestRule,
    unsavedReferenceLinks,
    onClose,
}: LinkingRuleActiveLearningSaveModalProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const [saveRule, setSaveRule] = React.useState(false);
    const [saveReferenceLinks, setSaveReferenceLinks] = React.useState(unsavedReferenceLinks > 0);
    const [saving, setSaving] = React.useState(false);

    const onSave = async () => {
        setSaving(true);
        try {
            await saveActiveLearningResults(
                activeLearningContext.projectId,
                activeLearningContext.linkingTaskId,
                saveRule,
                saveReferenceLinks
            );
            onClose();
        } catch (error) {
            registerError(
                "LinkingRuleActiveLearningSaveModal.onSave",
                t("ActiveLearning.saveDialog.saveFailureMessage"),
                error
            );
        } finally {
            setSaving(false);
        }
    };

    return (
        <SimpleDialog
            data-test-id={"active-learning-save-modal"}
            size={"small"}
            title={t("ActiveLearning.saveDialog.title")}
            isOpen={true}
            onClose={onClose}
            actions={[
                <Button
                    affirmative
                    onClick={onSave}
                    loading={saving}
                    disabled={!saveRule && !saveReferenceLinks}
                    data-test-id={"save-btn"}
                >
                    {t("common.action.save")}
                </Button>,
                <Button onClick={onClose} data-test-id={"close-btn"}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <div data-test-id="active-learning-save-form">
                <form>
                    <FieldItem
                        labelProps={{
                            text: t(t("ActiveLearning.saveDialog.saveReferenceLinks.label")),
                        }}
                        messageText={
                            unsavedReferenceLinks <= 0
                                ? t("ActiveLearning.saveDialog.saveReferenceLinks.noReferenceLinksMessage")
                                : t("ActiveLearning.saveDialog.saveReferenceLinks.referenceLinksInfoMessage", {
                                      nr: unsavedReferenceLinks,
                                  })
                        }
                    >
                        <Switch
                            data-test-id={"save-reference-links"}
                            disabled={unsavedReferenceLinks <= 0}
                            checked={saveReferenceLinks}
                            onChange={(value) => setSaveReferenceLinks(value)}
                        />
                    </FieldItem>
                    <FieldItem
                        labelProps={{
                            text: t(t("ActiveLearning.saveDialog.bestLearnedRule.label")),
                        }}
                        messageText={
                            !unsavedBestRule
                                ? t("ActiveLearning.saveDialog.bestLearnedRule.noBestLearnedRuleMessage")
                                : undefined
                        }
                    >
                        <Switch
                            data-test-id={"save-best-rule"}
                            disabled={!unsavedBestRule}
                            checked={saveRule}
                            onChange={(value) => setSaveRule(value)}
                        />
                    </FieldItem>
                </form>
            </div>
        </SimpleDialog>
    );
};
