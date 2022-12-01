import { taskUrl } from "@ducks/router/operations";
import { projectFileResourceDependents, requestRemoveProjectResource } from "@ducks/workspace/requests";
import { ITaskLink } from "@ducks/workspace/typings";
import { Link } from "@eccenca/gui-elements";
import { UppyFile } from "@uppy/core";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import DeleteModal from "./DeleteModal";

type UppyFileOrResource = UppyFile | { name: string; id: string };

interface IProps {
    file: UppyFileOrResource;

    projectId: string;

    onConfirm(fileId?: string | number);
}

export function FileRemoveModal({ projectId, onConfirm, file }: IProps) {
    const [t] = useTranslation();

    const [dependentTasks, setDependentTasks] = useState<ITaskLink[]>([]);

    // get file dependencies on open
    useEffect(() => {
        const openDeleteModal = async () => {
            const dependentTasksResponse = await projectFileResourceDependents(projectId, file.name);
            setDependentTasks(dependentTasksResponse.data);
        };

        if (file) {
            openDeleteModal();
        }
    }, [file]);

    const closeDeleteModal = () => {
        setDependentTasks([]);
        onConfirm();
    };

    const deleteFile = async () => {
        try {
            await requestRemoveProjectResource(projectId, file.name);
            onConfirm(file.id);
        } finally {
            closeDeleteModal();
        }
    };

    const renderDeleteModal = () => {
        if (dependentTasks.length > 0) {
            return (
                <div>
                    <p>{t("widget.FileWidget.removeFromDatasets", { fileName: file.name })}</p>
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
                        {t("widget.FileWidget.removeText", "Do you really want to delete file")} {file.name}?
                    </p>
                </div>
            );
        } else {
            const fileName = file?.name;
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
        />
    );
}
