import { Button, SimpleDialog, Spacing } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

import { ActiveLearningSessionInfo } from "../LinkingRuleActiveLearning.typings";
import { ActiveLearningSessionInfoWidget } from "../shared/ActiveLearningSessionInfoWidget";

interface Props {
    activeLearningSessionInfo?: ActiveLearningSessionInfo;
    close: () => any;
}

/** Warning modal that informs the current user that a learning session is already running. */
export const SessionRunningWarningModal = ({ activeLearningSessionInfo, close }: Props) => {
    const [t] = useTranslation();

    return (
        <SimpleDialog
            data-test-id={"reset-learning-state-modal"}
            isOpen={true}
            onClose={close}
            size={"small"}
            title={t("ActiveLearning.sessionExistsModal.title")}
            actions={[
                <Button key="close" onClick={close}>
                    {t("common.action.close")}
                </Button>,
            ]}
        >
            <p>{t("ActiveLearning.sessionExistsModal.description")}</p>
            <Spacing />
            <ActiveLearningSessionInfoWidget activeLearningSessionInfo={activeLearningSessionInfo} />
        </SimpleDialog>
    );
};
