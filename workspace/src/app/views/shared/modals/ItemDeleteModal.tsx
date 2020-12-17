import DeleteModal, { IDeleteModalOptions } from "./DeleteModal";
import React, { useEffect, useState } from "react";
import { Loading } from "../Loading/Loading";
import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";
import { requestRemoveProject, requestRemoveTask } from "@ducks/workspace/requests";
import { requestProjectMetadata, requestTaskMetadata } from "@ducks/shared/requests";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { useTranslation } from "react-i18next";
import { ITaskMetadataResponse } from "@ducks/shared/typings";
import { Spacing } from "@gui-elements/index";

interface IProps {
    item: Partial<ISearchResultsServer>;

    onClose: () => void;

    onConfirmed?();
}

/** Modal for task deletion. */
export function ItemDeleteModal({ item, onClose, onConfirmed }: IProps) {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ErrorResponse | null>(null);

    const [deleteModalOptions, setDeleteModalOptions] = useState<
        (Partial<IDeleteModalOptions> & { onConfirm(): void }) | undefined
    >(undefined);
    const [t] = useTranslation();

    useEffect(() => {
        prepareDelete();
    }, [item]);

    const handleConfirmRemove = (withDependentTaskDeletion: boolean) => async () => {
        const { id, projectId } = item;
        setError(null);
        try {
            setLoading(true);
            if (projectId) {
                await requestRemoveTask(id, projectId, withDependentTaskDeletion);
            } else {
                await requestRemoveProject(id);
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
            item.projectId
                ? `common.dataTypes.${item.type ? item.type : "genericArtefactLabel"}`
                : "common.dataTypes.project"
        );
        setDeleteModalOptions({
            render: () => <Loading description={t("Deletedialog.loading", "Loading delete dialog.")} />,
            onConfirm: handleConfirmRemove(false),
        });
        const deleteTitle = t("common.action.DeleteSmth", {
            smth: itemType,
        });

        try {
            const data =
                item.projectId && item.id
                    ? (await requestTaskMetadata(item.id, item.projectId)).data
                    : (await requestProjectMetadata(item.id ? item.id : item.projectId)).data;

            // Skip check the relations for projects
            if (
                (data as ITaskMetadataResponse).relations &&
                (data as ITaskMetadataResponse).relations.dependentTasksDirect.length
            ) {
                setDeleteModalOptions({
                    confirmationRequired: true,
                    render: () => (
                        <div>
                            {t("DeleteModal.confirmMsg", { name: data.label || item.id, itemType: itemType })}
                            <Spacing />
                            <ul>
                                {(data as ITaskMetadataResponse).relations.dependentTasksDirect.map((rel) => (
                                    <li key={rel}>{rel}</li>
                                ))}
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
                                type: itemType,
                                name: data.label || item.id,
                            })}
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
                            type: itemType,
                            name: item.label || item.id || item.projectId,
                        })}
                    </p>
                ),
                title: deleteTitle,
                onConfirm: handleConfirmRemove(false),
            });
        }
    };

    return (
        <DeleteModal
            data-test-id={"deleteItemModal"}
            isOpen={true}
            onDiscard={onClose}
            {...deleteModalOptions}
            removeLoading={loading}
            errorMessage={error && `Deletion failed: ${error.asString()}`}
        />
    );
}
