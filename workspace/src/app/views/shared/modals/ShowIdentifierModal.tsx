import {
    Button,
    TextField,
    SimpleDialog,
    Spacing,
    Card,
    CardHeader,
    CardTitle,
    CardContent,
} from "@gui-elements/index";
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
            <Card>
                <CardHeader>
                    <CardTitle>{taskId ? `{TaskId}` : `{projectId}`}</CardTitle>
                </CardHeader>
                <CardContent>
                    <TextField disabled value={taskId ? taskId : projectId} rightElement={idCopyBtn} />
                </CardContent>
            </Card>
            {taskId ? (
                <>
                    <Card>
                        <CardHeader>
                            <CardTitle>{`{projectId}:{taskId}`}</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <TextField disabled value={`${projectId}:${taskId}`} rightElement={combinedCopyBtn} />
                        </CardContent>
                    </Card>
                </>
            ) : null}
            <Spacing size="medium" vertical />
        </SimpleDialog>
    );
};

export default ShowIdentifierModal;
