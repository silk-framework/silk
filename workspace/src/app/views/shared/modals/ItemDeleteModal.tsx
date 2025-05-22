import DeleteModal, { IDeleteModalOptions } from "./DeleteModal";
import React, { useEffect, useState } from "react";
import { Loading } from "../Loading/Loading";
import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";
import { requestRemoveProject, requestRemoveTask } from "@ducks/workspace/requests";
import { requestProjectMetadata, requestTaskMetadata } from "@ducks/shared/requests";
import { useTranslation } from "react-i18next";
import { IModalItem, ITaskMetadataResponse } from "@ducks/shared/typings";
import { Spacing, Tooltip, Link } from "@eccenca/gui-elements";

interface IProps {
    item: IModalItem;

    onClose: () => void;

    onConfirmed?();

    notifications?: React.ReactNode | React.ReactNode[];

    deleteDisabled?: boolean;
}

/** Modal for task deletion. */
export function ItemDeleteModal({ item, onClose, onConfirmed, notifications, deleteDisabled }: IProps) {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ErrorResponse | undefined>(undefined);

    const [deleteModalOptions, setDeleteModalOptions] = useState<
        (Partial<IDeleteModalOptions> & { onConfirm(): void }) | undefined
    >(undefined);
    const [t] = useTranslation();

    useEffect(() => {
        prepareDelete();
    }, [item]);

    const handleConfirmRemove = (withDependentTaskDeletion: boolean) => async () => {
        const { id, projectId } = item;
        setError(undefined);
        try {
            setLoading(true);
            if (id) {
                await requestRemoveTask(id, projectId, withDependentTaskDeletion);
            } else {
                await requestRemoveProject(projectId as string);
            }
            onConfirmed && onConfirmed();
        } catch (e) {
            if (e.isFetchError) {
                setError((e as FetchError).errorResponse);
            } else {
                console.warn(e);
            }
        } finally {
            setLoading(false);
        }
    };

    const prepareDelete = async () => {
        const itemType = t(
            item.id
                ? `common.dataTypes.${item.type ? item.type.toLowerCase() : "genericArtefactLabel"}`
                : "common.dataTypes.project",
        );
        setDeleteModalOptions({
            render: () => <Loading description={t("Deletedialog.loading", "Loading delete dialog.")} delay={0} />,
            onConfirm: handleConfirmRemove(false),
        });
        const deleteTitle = t("common.action.DeleteSmth", {
            smth: itemType,
        });

        try {
            const data =
                item.projectId && item.id
                    ? (await requestTaskMetadata(item.id, item.projectId, true)).data
                    : (await requestProjectMetadata(item.projectId as string)).data;

            // Skip check the relations for projects
            if (
                (data as ITaskMetadataResponse).relations &&
                (data as ITaskMetadataResponse).relations.dependentTasksAll.length
            ) {
                setDeleteModalOptions({
                    confirmationRequired: true,
                    render: () => (
                        <div>
                            {t("DeleteModal.confirmMsg", { name: data.label || item.id, itemType: itemType })}
                            <Spacing />
                            <ul>
                                {(data as ITaskMetadataResponse).relations.dependentTasksAll.map((taskRef) =>
                                    typeof taskRef === "string" ? (
                                        <li key={taskRef}>{taskRef}</li>
                                    ) : (
                                        <li key={taskRef.id}>
                                            <Link href={taskRef.taskLink} target="_blank">
                                                <Tooltip content={t("common.action.openInNewTab")}>
                                                    {taskRef.label ?? taskRef.id}
                                                </Tooltip>
                                            </Link>
                                        </li>
                                    ),
                                )}
                            </ul>
                        </div>
                    ),
                    title: t("common.action.DeleteSmth", {
                        smth: itemType,
                    }),
                    onConfirm: handleConfirmRemove(true),
                });
            } else {
                setDeleteModalOptions({
                    confirmationRequired: false,
                    render: () => (
                        <p>
                            {t("DeleteModal.deleteResource", {
                                type: itemType[0].toUpperCase() + itemType.substring(1),
                                name: data.label || item.id || item.projectId,
                            })}
                            {!item.id && (
                                <> {t("DeleteModal.projectNote", "All contained items will be deleted, too.")}</>
                            )}
                        </p>
                    ),
                    title: deleteTitle,
                    onConfirm: handleConfirmRemove(false),
                });
            }
        } catch (e) {
            setDeleteModalOptions({
                confirmationRequired: false,
                render: () => (
                    <p>
                        {t("DeleteModal.deleteResource", {
                            type: itemType[0].toUpperCase() + itemType.substring(1),
                            name: item.label || item.id || item.projectId,
                        })}
                    </p>
                ),
                title: deleteTitle,
                onConfirm: handleConfirmRemove(false),
            });
        }
    };
    return deleteModalOptions ? (
        <DeleteModal
            data-test-id={"deleteItemModal"}
            isOpen={true}
            onDiscard={onClose}
            {...deleteModalOptions}
            removeLoading={loading}
            deleteDisabled={deleteDisabled}
            errorMessage={error && `Deletion failed: ${error.asString()}`}
            notifications={notifications}
        />
    ) : null;
}
