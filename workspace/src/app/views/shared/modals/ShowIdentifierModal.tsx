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
    taskId: string;
    projectId: string;
}

const ShowIdentifierModal: React.FC<ShowIdentifierProps> = ({ onDiscard, taskId, projectId }) => {
    const [t] = useTranslation();
    const [projectCopyBtn, taskCopyBtn] = useCopyButton([{ text: projectId }, { text: taskId }]);

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
                    <CardTitle>Project Id</CardTitle>
                </CardHeader>
                <CardContent>
                    <TextField disabled value={projectId} rightElement={projectCopyBtn} />
                </CardContent>
            </Card>
            <Spacing size="medium" vertical />
            <Card>
                <CardHeader>
                    <CardTitle>Task Id</CardTitle>
                </CardHeader>
                <CardContent>
                    <TextField disabled value={taskId} rightElement={taskCopyBtn} />
                </CardContent>
            </Card>
        </SimpleDialog>
    );
};

export default ShowIdentifierModal;
