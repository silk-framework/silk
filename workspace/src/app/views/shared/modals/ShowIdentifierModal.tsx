import { Button, FieldItem, TextField, SimpleDialog } from "@eccenca/gui-elements";
import useCopyButton from "../../../hooks/useCopyButton";
import React from "react";
import { useTranslation } from "react-i18next";
import { useInitFrontend } from "../../pages/MappingEditor/api/silkRestApi.hooks";
import { requestProjectUri } from "@ducks/workspace/requests";
import useErrorHandler from "../../../hooks/useErrorHandler";

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
    const initData = useInitFrontend();
    const [idCopyBtn, combinedCopyBtn, uriCopyBtn] = useCopyButton(data);
    const [projectUri, setProjectUri] = React.useState<string | undefined>(undefined);
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();

    const resourceUri = (projectUri: string, taskId: string | undefined) =>
        taskId ? `http://dataintegration.eccenca.com/${projectId}/${taskId}` : projectUri;

    React.useEffect(() => {
        if (taskId) {
            setData((buttons) => [...buttons, { text: `${projectId}:${taskId}`, "data-test-id": "combined-copy-btn" }]);
        }
    }, [taskId]);

    React.useEffect(() => {
        if (initData?.dmBaseUrl) {
            fetchProjectUri(projectId);
        }
    }, [initData?.dmBaseUrl, projectId]);

    const fetchProjectUri = async (projectId: string) => {
        try {
            const { uri } = (await requestProjectUri(projectId)).data;
            setData((buttons) => [...buttons, { text: resourceUri(uri, taskId), "data-test-id": "uri-copy-btn" }]);
            setProjectUri(uri);
        } catch (ex) {
            registerError("ShowIdentifierModal.fetchProjectUri", "Could not fetch project/task URI for display.", ex);
        }
    };

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
                        : t("CreateModal.CustomIdentifierInput.ProjectId"),
                }}
            >
                <TextField disabled value={taskId ? taskId : projectId} rightElement={idCopyBtn} />
            </FieldItem>
            {taskId ? (
                <FieldItem
                    labelProps={{
                        text: t("ShowIdentifierModal.combinedIdentifier"),
                    }}
                    helperText={`{${t("CreateModal.CustomIdentifierInput.ProjectId")}}:{${t(
                        "CreateModal.CustomIdentifierInput.TaskId"
                    )}}`}
                >
                    <TextField disabled value={`${projectId}:${taskId}`} rightElement={combinedCopyBtn} />
                </FieldItem>
            ) : null}
            {projectUri ? (
                <FieldItem
                    labelProps={{
                        text: taskId
                            ? t("CreateModal.CustomIdentifierInput.TaskUri")
                            : t("CreateModal.CustomIdentifierInput.ProjectUri"),
                    }}
                >
                    <TextField
                        disabled
                        value={resourceUri(projectUri, taskId)}
                        rightElement={taskId ? uriCopyBtn : combinedCopyBtn}
                    />
                </FieldItem>
            ) : null}
        </SimpleDialog>
    );
};

export default ShowIdentifierModal;
