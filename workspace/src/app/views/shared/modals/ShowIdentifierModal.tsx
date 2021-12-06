import { Button, TextField, SimpleDialog, Spacing, TitleSubsection } from "@gui-elements/index";
import useCopyButton from "../../../hooks/useCopyButton";
import React from "react";
import { useTranslation } from "react-i18next";

interface ShowIdentifierProps {
    /**close the modal**/
    onDiscard: () => void;

    /**project Id only visible in a project ctx */
    projectId: string;

    /** taskId only visible under a selected task in a project ctx*/
    taskId?: string;
}

const ShowIdentifierModal: React.FC<ShowIdentifierProps> = ({ onDiscard, taskId, projectId }) => {
    const [buttons] = React.useState<Array<{ text: string }>>([{ text: projectId }]);
    const [t] = useTranslation();

    React.useEffect(() => {
        if (taskId) {
            buttons.push({ text: taskId });
        }
    }, [taskId]);

    const [projectCopyBtn, taskCopyBtn] = useCopyButton(buttons);

    return (
        <SimpleDialog
            size="small"
            title={t("ShowIdentifierModal.title")}
            isOpen={true}
            onClose={onDiscard}
            actions={[
                <Button key="cancel" onClick={onDiscard}>
                    {t("common.action.close")}
                </Button>,
            ]}
        >
            <TitleSubsection>{`{${t("CreateModal.CustomIdentifierInput.ProjectId")}}`}</TitleSubsection>
            <TextField disabled value={projectId} rightElement={projectCopyBtn} />
            {taskId ? (
                <>
                    <Spacing size="medium" />
                    <TitleSubsection>
                        {`
                            {${t("CreateModal.CustomIdentifierInput.ProjectId")}}
                            :
                            {${t("CreateModal.CustomIdentifierInput.TaskId")}}
                        `}
                    </TitleSubsection>
                    <TextField disabled value={`${projectId}:${taskId}`} rightElement={taskCopyBtn} />
                </>
            ) : null}
        </SimpleDialog>
    );
};

export default ShowIdentifierModal;
