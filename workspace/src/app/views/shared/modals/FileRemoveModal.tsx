import React, { useEffect, useState } from "react";
import DeleteModal from "./DeleteModal";
import { projectFileResourceDependents, requestRemoveProjectResource } from "@ducks/workspace/requests";
import { useTranslation } from "react-i18next";
import { UppyFile } from "@uppy/core";

type UppyFileOrResource = UppyFile | { name: string; id: string };

interface IProps {
    file: UppyFileOrResource;

    projectId: string;

    onConfirm(fileId?: string | number);
}

export function FileRemoveModal({ projectId, onConfirm, file }: IProps) {
    const [t] = useTranslation();

    const [dependentTasks, setDependentTasks] = useState<any>([]);

    // get file dependencies on open
    useEffect(() => {
        const openDeleteModal = async () => {
            const dependentTasks = await projectFileResourceDependents(projectId, file.name);
            setDependentTasks(dependentTasks);
        };

        if (file) {
            openDeleteModal();
        }
    }, [file]);

    const closeDeleteModal = () => {
        setDependentTasks([]);
        onConfirm(file.id);
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
                            <li key={task}>{task}</li>
                        ))}
                    </ul>
                    <p>
                        {t("widget.FileWidget.removeText", "Do you really want to delete file")} {file.name}?
                    </p>
                </div>
            );
        } else {
            return <p>{t("widget.FileWidget.deleted", { fileName: file?.name })}</p>;
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
