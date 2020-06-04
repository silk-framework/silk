import DeleteModal from "./DeleteModal";
import React, { useEffect, useState } from "react";
import { Loading } from "../Loading/Loading";
import { ErrorResponse } from "../../../services/fetch/responseInterceptor";
import { requestRemoveProject, requestRemoveTask } from "@ducks/workspace/requests";
import { requestProjectMetadata, requestTaskMetadata } from "@ducks/shared/requests";
import { ISearchResultsServer } from "@ducks/workspace/typings";

interface IProps {
    item: Partial<ISearchResultsServer>;

    onClose: () => void;

    onConfirmed?();
}

/** Modal for task deletion. */
export function ItemDeleteModal({ item, onClose, onConfirmed }: IProps) {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ErrorResponse>({} as ErrorResponse);

    const [deleteModalOptions, setDeleteModalOptions] = useState({});

    useEffect(() => {
        prepareDelete();
    }, [item]);

    const handleConfirmRemove = async () => {
        const { id, projectId } = item;
        try {
            setLoading(true);
            if (projectId) {
                await requestRemoveTask(id, projectId);
            } else {
                await requestRemoveProject(id);
            }
            onConfirmed && onConfirmed();
        } catch (e) {
            setError(e);
        } finally {
            setLoading(false);
        }
    };

    const prepareDelete = async () => {
        setDeleteModalOptions({
            render: () => <Loading description="Loading delete dialog." />,
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
                            <p>Task '{data.label || item.id}' is used by other tasks! </p>
                            <p>Deleting this task will also delete all depending tasks listed below:</p>
                            <ul>
                                {data.relations.dependentTasksDirect.map((rel) => (
                                    <li key={rel}>{rel}</li>
                                ))}
                            </ul>
                        </div>
                    ),
                    title: `Delete ${item.projectId ? "task" : "project"}`,
                });
            } else {
                setDeleteModalOptions({
                    confirmationRequired: false,
                    render: () => (
                        <p>
                            {item.projectId ? "Task" : "Project"} '{data.label || item.id}' will be deleted.
                        </p>
                    ),
                    title: `Delete ${item.projectId ? "task" : "project"}`,
                });
            }
        } catch (e) {
            setDeleteModalOptions({
                confirmationRequired: false,
                render: () => (
                    <p>
                        {item.projectId ? "Task" : "Project"} '{item.label || item.id || item.projectId}' will be
                        deleted.
                    </p>
                ),
                title: `Delete ${item.projectId ? "task" : "project"}`,
            });
        }
    };

    return <DeleteModal isOpen={true} onDiscard={onClose} onConfirm={handleConfirmRemove} {...deleteModalOptions} />;
}
