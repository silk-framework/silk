import DeleteModal from "./DeleteModal";
import React, { useEffect, useState } from "react";
import { sharedOp } from "@ducks/shared";
import { workspaceOp } from "@ducks/workspace";
import { useDispatch } from "react-redux";
import { Loading } from "../Loading/Loading";

interface IProps {
    selectedItem: IProjectOrTask;
    onClose: () => void;
}

export interface IProjectOrTask {
    // If projectId is undefined this specifies a project ID, else a task ID
    id: string;
    // For tasks this is defined. For projects this is undefined.
    projectId?: string;
    label?: string;
}

/** Modal for task deletion. */
export function ItemDeleteModal({ selectedItem, onClose }: IProps) {
    const [deleteModalOptions, setDeleteModalOptions] = useState({});
    const dispatch = useDispatch();

    const handleConfirmRemove = () => {
        const { id, projectId } = selectedItem;
        dispatch(workspaceOp.fetchRemoveTaskAsync(id, projectId));
        onClose();
    };

    const prepareDelete = async () => {
        setDeleteModalOptions({
            render: () => <Loading description="Loading delete dialog." />,
        });

        try {
            const data = await sharedOp.getTaskMetadataAsync(selectedItem.id, selectedItem.projectId);

            // Skip check the relations for projects
            if (data.relations && data.relations.dependentTasksDirect.length) {
                setDeleteModalOptions({
                    confirmationRequired: true,
                    render: () => (
                        <div>
                            <p>Task '{data.label || selectedItem.id}' is used by other tasks! </p>
                            <p>Deleting this task will also delete all depending tasks listed below:</p>
                            <ul>
                                {data.relations.dependentTasksDirect.map((rel) => (
                                    <li key={rel}>{rel}</li>
                                ))}
                            </ul>
                        </div>
                    ),
                    title: `Delete ${selectedItem.projectId ? "task" : "project"}`,
                });
            } else {
                setDeleteModalOptions({
                    confirmationRequired: false,
                    render: () => (
                        <p>
                            {selectedItem.projectId ? "Task" : "Project"} '{data.label || selectedItem.id}' will be
                            deleted.
                        </p>
                    ),
                    title: `Delete ${selectedItem.projectId ? "task" : "project"}`,
                });
            }
        } catch (e) {
            setDeleteModalOptions({
                confirmationRequired: false,
                render: () => (
                    <p>
                        {selectedItem.projectId ? "Task" : "Project"} '
                        {selectedItem.label || selectedItem.id || selectedItem.projectId}' will be deleted.
                    </p>
                ),
                title: `Delete ${selectedItem.projectId ? "task" : "project"}`,
            });
        }
    };

    useEffect(() => {
        prepareDelete();
    }, [selectedItem]);

    return <DeleteModal isOpen={true} onDiscard={onClose} onConfirm={handleConfirmRemove} {...deleteModalOptions} />;
}
