import { Button, FieldItem, TextField, SimpleDialog } from "@eccenca/gui-elements";
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
    const [data, setData] = React.useState([{ text: taskId ? taskId : projectId, "data-test-id": "id-copy-btn" }]);
    const [idCopyBtn, combinedCopyBtn] = useCopyButton(data);
    const [t] = useTranslation();

    React.useEffect(() => {
        if (taskId) {
            setData((buttons) => [...buttons, { text: `${projectId}:${taskId}`, "data-test-id": "combined-copy-btn" }]);
        }
    }, [taskId]);

    return (
        <SimpleDialog
            size="small"
            title={t("ShowIdentifierModal.title")}
            isOpen={true}
            onClose={onDiscard}
            actions={[
                <Button key="cancel" onClick={onDiscard} data-test-id="show-cancel-button">
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <FieldItem
                labelProps={{
                    text: taskId
                        ? t("CreateModal.CustomIdentifierInput.TaskId")
                        : t("CreateModal.CustomIdentifierInput.ProjectId")
                }}
            >
                <TextField disabled value={taskId ? taskId : projectId} rightElement={idCopyBtn} />
            </FieldItem>
            {taskId ? (
                <>
                    <FieldItem
                        labelProps={{
                            text: t("ShowIdentifierModal.combinedIdentifier")
                        }}
                        helperText={`{${t("CreateModal.CustomIdentifierInput.ProjectId")}}:{${t("CreateModal.CustomIdentifierInput.TaskId")}}`}
                    >
                        <TextField disabled value={`${projectId}:${taskId}`} rightElement={combinedCopyBtn} />
                    </FieldItem>
                </>
            ) : null}
        </SimpleDialog>
    );
};

export default ShowIdentifierModal;
