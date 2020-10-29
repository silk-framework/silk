import DeleteModal from "./DeleteModal";
import React, { useEffect, useState } from "react";
import { Loading } from "../Loading/Loading";
import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";
import { requestRemoveProject, requestRemoveTask } from "@ducks/workspace/requests";
import { requestProjectMetadata, requestTaskMetadata } from "@ducks/shared/requests";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { useTranslation } from "react-i18next";

interface IProps {
    item: Partial<ISearchResultsServer>;

    onClose: () => void;

    onConfirmed?();
}

/** Modal for task deletion. */
export function ItemDeleteModal({ item, onClose, onConfirmed }: IProps) {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ErrorResponse | null>(null);

    const [deleteModalOptions, setDeleteModalOptions] = useState({});
    const [t] = useTranslation();

    useEffect(() => {
        prepareDelete();
    }, [item]);

    const handleConfirmRemove = async () => {
        const { id, projectId } = item;
        setError(null);
        try {
            setLoading(true);
            if (projectId) {
                await requestRemoveTask(id, projectId);
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
        setDeleteModalOptions({
            render: () => <Loading description={t("Deletedialog.loading", "Loading delete dialog.")} />,
        });
        const deleteTitle = t("common.action.DeleteSmth", {
            smth: t(item.projectId ? "common.dataTypes.task" : "common.dataTypes.project"),
        });

        try {
            const data: any =
                item.projectId && item.id
                    ? await requestTaskMetadata(item.id, item.projectId)
                    : await requestProjectMetadata(item.id ? item.id : item.projectId);

            // Skip check the relations for projects
            if (data.relations && data.relations.dependentTasksDirect.length) {
                setDeleteModalOptions({
                    confirmationRequired: true,
                    render: () => (
                        <div>
                            {t("DeleteModal.confirmMsg", { name: data.label || item.id })}
                            <ul>
                                {data.relations.dependentTasksDirect.map((rel) => (
                                    <li key={rel}>{rel}</li>
                                ))}
                            </ul>
                        </div>
                    ),
                    title: t("common.action.DeleteSmth", {
                        smth: t(item.projectId ? "common.dataTypes.task" : "common.dataTypes.project"),
                    }),
                });
            } else {
                setDeleteModalOptions({
                    confirmationRequired: false,
                    render: () => (
                        <p>
                            {t("DeleteModal.deleteResource", {
                                type: t(item.projectId ? "common.dataTypes.task" : "common.dataTypes.project"),
                                name: data.label || item.id,
                            })}
                        </p>
                    ),
                    title: deleteTitle,
                });
            }
        } catch (e) {
            setDeleteModalOptions({
                confirmationRequired: false,
                render: () => (
                    <p>
                        {t("DeleteModal.deleteResource", {
                            type: t(item.projectId ? "common.dataTypes.task" : "common.dataTypes.project"),
                            name: item.label || item.id || item.projectId,
                        })}
                    </p>
                ),
                title: deleteTitle,
            });
        }
    };

    return (
        <DeleteModal
            data-test-id={"deleteItemModal"}
            isOpen={true}
            onDiscard={onClose}
            onConfirm={handleConfirmRemove}
            {...deleteModalOptions}
            removeLoading={loading}
            errorMessage={error && error.asString()}
        />
    );
}
