import React, { useEffect, useState } from "react";
import DeleteModal from "./DeleteModal";
import { projectFileResourceDependents, requestRemoveProjectResource } from "@ducks/workspace/requests";
import { useTranslation } from "react-i18next";
import { UppyFile } from "@uppy/core";
import { ITaskLink } from "@ducks/workspace/typings";
import { Link } from "@eccenca/gui-elements";
import { taskUrl } from "@ducks/router/operations";
import { fileValue } from "@ducks/shared/typings";
import { ErrorResponse } from "../../../services/fetch/responseInterceptor";
import { useModalError } from "../../../hooks/useModalError";

type UppyFileOrResource = UppyFile | { name: string; fullPath?: string; id: string };

interface IProps {
    file: UppyFileOrResource;

    projectId: string;

    onConfirm(fileId?: string | number);
}

export function FileRemoveModal({ projectId, onConfirm, file }: IProps) {
    const [t] = useTranslation();
    const [error, setError] = React.useState<ErrorResponse | undefined>();
    const checkAndDisplayError = useModalError({ setError });
    const [dependentTasks, setDependentTasks] = useState<ITaskLink[]>([]);

    // get file dependencies on open
    useEffect(() => {
        const openDeleteModal = async () => {
            const dependentTasksResponse = await projectFileResourceDependents(projectId, fileValue(file));
            setDependentTasks(dependentTasksResponse.data);
        };

        if (file) {
            setError(undefined);
            openDeleteModal();
        }
    }, [file]);

    const closeDeleteModal = () => {
        setDependentTasks([]);
        onConfirm();
    };

    const deleteFile = async () => {
        try {
            await requestRemoveProjectResource(projectId, fileValue(file));
            onConfirm(file.id);
        } catch (e) {
            checkAndDisplayError(e);
        } finally {
            setDependentTasks([]);
        }
    };

    const renderDeleteModal = () => {
        if (dependentTasks.length > 0) {
            return (
                <div>
                    <p>{t("widget.FileWidget.removeFromDatasets", { fileName: fileValue(file) })}</p>
                    <ul>
                        {dependentTasks.map((task) => (
                            <li key={task.id}>
                                <Link target={"_blank"} href={taskUrl(projectId, task.taskType, task.id)}>
                                    {task.label}
                                </Link>
                            </li>
                        ))}
                    </ul>
                    <p>
                        {t("widget.FileWidget.removeText", "Do you really want to delete file")} {fileValue(file)}?
                    </p>
                </div>
            );
        } else {
            const fileName = fileValue(file);
            return <p>{t("widget.FileWidget.deleted", { fileName })}</p>;
        }
    };

    return (
        <DeleteModal
            isOpen={!!file}
            onDiscard={closeDeleteModal}
            onConfirm={deleteFile}
            render={renderDeleteModal}
            title={t("widget.FileWidget.deleteFile", "Delete File")}
            errorMessage={error && `Deletion failed: ${error.asString()}`}
        />
    );
}
