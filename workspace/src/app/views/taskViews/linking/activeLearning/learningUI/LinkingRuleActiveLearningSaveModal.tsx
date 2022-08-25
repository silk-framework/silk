import { Button, FieldItem, SimpleDialog, Spacing, Spinner, Switch } from "@eccenca/gui-elements";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { saveActiveLearningResults } from "../LinkingRuleActiveLearning.requests";
import {
    ActiveLearningSessionInfoWidget,
    useActiveLearningSessionInfo,
} from "../shared/ActiveLearningSessionInfoWidget";
import { IEvaluatedReferenceLinksScore, ILinkingRule, OptionallyLabelledParameter } from "../../linking.types";
import { LinkingRuleActiveLearningBestLearnedRule } from "./LinkingRuleActiveLearningBestLearnedRule";

interface LinkingRuleActiveLearningSaveModalProps {
    unsavedBestRule: OptionallyLabelledParameter<ILinkingRule> | undefined;
    evaluationScore?: IEvaluatedReferenceLinksScore;
    onClose: () => any;
}

export const LinkingRuleActiveLearningSaveModal = ({
    unsavedBestRule,
    evaluationScore,
    onClose,
}: LinkingRuleActiveLearningSaveModalProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const [saveRule, setSaveRule] = React.useState(false);
    const [saving, setSaving] = React.useState(false);
    const { sessionInfo, loading } = useActiveLearningSessionInfo(
        activeLearningContext.projectId,
        activeLearningContext.linkingTaskId
    );
    const [saveReferenceLinks, setSaveReferenceLinks] = React.useState(false);
    const unsavedReferenceLinks = sessionInfo?.referenceLinks
        ? sessionInfo.referenceLinks.addedLinks + sessionInfo.referenceLinks.removedLinks
        : undefined;

    useEffect(() => {
        if (sessionInfo) {
            if (sessionInfo.referenceLinks.addedLinks + sessionInfo.referenceLinks.removedLinks > 0) {
                setSaveReferenceLinks(true);
            }
        }
    }, [sessionInfo]);

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
            size={"large"}
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
                            unsavedReferenceLinks != null
                                ? unsavedReferenceLinks <= 0
                                    ? t("ActiveLearning.saveDialog.saveReferenceLinks.noReferenceLinksMessage")
                                    : t("ActiveLearning.saveDialog.saveReferenceLinks.referenceLinksInfoMessage", {
                                          nr: unsavedReferenceLinks,
                                      })
                                : undefined
                        }
                    >
                        <Switch
                            data-test-id={"save-reference-links"}
                            disabled={(unsavedReferenceLinks ?? 0) <= 0}
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
                <Spacing />
                {unsavedBestRule ? (
                    <LinkingRuleActiveLearningBestLearnedRule rule={unsavedBestRule} score={evaluationScore} />
                ) : null}
                <Spacing />
                {loading ? (
                    <Spinner />
                ) : sessionInfo ? (
                    <ActiveLearningSessionInfoWidget activeLearningSessionInfo={sessionInfo} />
                ) : null}
            </div>
        </SimpleDialog>
    );
};
