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
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <Card>
                <CardHeader>
                    <CardTitle>{`{project-id}`}</CardTitle>
                </CardHeader>
                <CardContent>
                    <TextField disabled value={projectId} rightElement={projectCopyBtn} />
                </CardContent>
            </Card>
            {taskId ? (
                <>
                    <Card>
                        <CardHeader>
                            <CardTitle>{`{project-id}:{task-id}`}</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <TextField disabled value={`${projectId}:${taskId}`} rightElement={taskCopyBtn} />
                        </CardContent>
                    </Card>
                </>
            ) : null}
            <Spacing size="medium" vertical />
        </SimpleDialog>
    );
};

export default ShowIdentifierModal;
