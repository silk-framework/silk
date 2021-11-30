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
    onDiscard: () => void;
    taskId?: string;
    projectId: string;
}

const ShowIdentifierModal: React.FC<ShowIdentifierProps> = ({ onDiscard, taskId, projectId }) => {
    const [t] = useTranslation();
    const buttons = [{ text: projectId }];
    if (taskId) {
        buttons.push({ text: taskId });
    }
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
