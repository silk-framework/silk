import {
    Button,
    FieldItem,
    FieldItemRow,
    Notification,
    SimpleDialog,
    Spacing,
    Switch
} from "@eccenca/gui-elements";
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
    const { sessionInfo } = useActiveLearningSessionInfo(
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
            notifications={(saveReferenceLinks || saveRule) ? (
                <>
                    {saveReferenceLinks && sessionInfo && (
                        <ActiveLearningSessionInfoWidget activeLearningSessionInfo={sessionInfo} />
                    )}
                    {saveReferenceLinks && saveRule && <Spacing />}
                    {saveRule && (
                        <Notification warning>
                            Your current linking rule will be overwritten.
                        </Notification>
                    )}
                </>
            ) : undefined}
        >
            <div data-test-id="active-learning-save-form">
                <FieldItemRow justifyItemWidths>
                    <FieldItem
                        style={{alignSelf: "flex-start"}}
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
                        >
                            {t("ActiveLearning.saveDialog.saveReferenceLinks.label")}
                        </Switch>
                    </FieldItem>
                    <FieldItem
                        style={{alignSelf: "flex-start"}}
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
                        >
                            {t("ActiveLearning.saveDialog.bestLearnedRule.label")}
                        </Switch>
                    </FieldItem>
                </FieldItemRow>
                <Spacing />
                {unsavedBestRule ? (
                    <LinkingRuleActiveLearningBestLearnedRule
                        rule={unsavedBestRule}
                        score={evaluationScore}
                        defaultDisplayVisualRule
                    />
                ) : null}
            </div>
        </SimpleDialog>
    );
};