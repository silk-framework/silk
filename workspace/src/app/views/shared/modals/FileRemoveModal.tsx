import React, { useEffect, useState } from "react";
import DeleteModal from "./DeleteModal";
import { projectFileResourceDependents, removeProjectFileResource } from "@ducks/workspace/requests";
import { useTranslation } from "react-i18next";
export interface IFileDeleteModalOptions {
    fileName: string | null;
    dependentTasks: string[];
}

interface IProps {
    projectId: string;
    fileName: string;
    isOpen: boolean;
    onConfirm(fileName?: string);
}

export function FileRemoveModal({ projectId, isOpen, onConfirm, fileName }: IProps) {
    const [t] = useTranslation();
    const [deleteModalOpts, setDeleteModalOpts] = useState<IFileDeleteModalOptions>({
        fileName: null,
        dependentTasks: [],
    });

    useEffect(() => {
        const openDeleteModal = async () => {
            const dependentTasks = await projectFileResourceDependents(projectId, fileName);
            setDeleteModalOpts({ fileName: fileName, dependentTasks: dependentTasks });
        };

        if (isOpen) {
            openDeleteModal();
        }
    }, [isOpen]);

    const closeDeleteModal = () => {
        setDeleteModalOpts({ fileName: null, dependentTasks: [] });
        onConfirm();
    };

    const deleteFile = async (fileName: string) => {
        try {
            await removeProjectFileResource(projectId, fileName);
            onConfirm(fileName);
        } finally {
            closeDeleteModal();
        }
    };

    const renderDeleteModal = () => {
        if (deleteModalOpts.dependentTasks.length > 0) {
            return (
                <div>
                    <p>{t("widget.FileWidget.removeFromDatasets", { fileName: deleteModalOpts.fileName })}</p>
                    <ul>
                        {deleteModalOpts.dependentTasks.map((task) => (
                            <li key={task}>{task}</li>
                        ))}
                    </ul>
                    <p>
                        {t("widget.FileWidget.removeText", "Do you really want to delete file")}{" "}
                        {deleteModalOpts.fileName}?
                    </p>
                </div>
            );
        } else {
            return <p>{t("widget.FileWidget.deleted", { fileName: deleteModalOpts.fileName })}</p>;
        }
    };

    return (
        <DeleteModal
            isOpen={isOpen}
            onDiscard={closeDeleteModal}
            onConfirm={() => deleteFile(deleteModalOpts.fileName)}
            render={renderDeleteModal}
            title={t("widget.FileWidget.deleteFile", "Delete File")}
        />
    );
}
