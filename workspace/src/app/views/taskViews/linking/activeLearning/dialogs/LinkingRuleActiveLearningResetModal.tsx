import { Button, SimpleDialog } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { resetActiveLearningSession } from "../LinkingRuleActiveLearning.requests";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";

interface Props {
    /** Closes the modal. */
    close: (hasReset: boolean) => any;
}

/** Modal that allows to reset the learning state. */
export const LinkingRuleActiveLearningResetModal = ({ close }: Props) => {
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [resetting, setResetting] = React.useState(false);

    const resetState = async () => {
        setResetting(true);
        try {
            await resetActiveLearningSession(activeLearningContext.projectId, activeLearningContext.linkingTaskId);
            close(true);
        } catch (err) {
            registerError(
                "LinkingRuleActiveLearningResetModal.resetState",
                t("ActiveLearning.resetDialog.failed"),
                err
            );
        } finally {
            setResetting(false);
        }
    };

    return (
        <SimpleDialog
            data-test-id={"reset-learning-state-modal"}
            isOpen={true}
            size={"small"}
            title={t("ActiveLearning.resetDialog.title")}
            actions={[
                <Button key="reset" onClick={resetState} loading={resetting} disruptive={true}>
                    {t("ActiveLearning.resetDialog.reset")}
                </Button>,
                <Button key="cancel" onClick={() => close(false)}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <p>{t("ActiveLearning.resetDialog.description")}</p>
        </SimpleDialog>
    );
};
