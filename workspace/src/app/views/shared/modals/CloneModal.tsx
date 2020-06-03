import React, { useState } from "react";
import { Button, SimpleDialog, TextField } from "@wrappers/index";
import { ErrorResponse } from "../../../services/fetch/responseInterceptor";
import { requestCloneProject, requestCloneTask } from "@ducks/workspace/requests";
import { ISearchResultsServer } from "@ducks/workspace/typings";

export interface ICloneOptions {
    item: Partial<ISearchResultsServer>;

    onDiscard(): void;

    onConfirmed?(newLabel: string): void;
}

export default function CloneModal({ item, onDiscard, onConfirmed }: ICloneOptions) {
    const [newLabel, setNewLabel] = useState(item.label || item.id);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ErrorResponse>({} as ErrorResponse);

    const handleCloning = async () => {
        const { projectId, id } = item;

        try {
            setLoading(true);
            const payload = {
                metaData: {
                    label: newLabel,
                    description: item.description,
                },
            };

            if (projectId) {
                await requestCloneTask(id, projectId, payload);
            } else {
                await requestCloneProject(id, payload);
            }
            onConfirmed && onConfirmed(newLabel);
        } catch (e) {
            setError(e);
        } finally {
            setLoading(false);
        }
    };

    return (
        <SimpleDialog
            size="small"
            title="Cloning"
            isOpen={true}
            onClose={onDiscard}
            actions={[
                <Button key="clone" affirmative onClick={handleCloning} disabled={!newLabel}>
                    Clone
                </Button>,
                <Button key="cancel" onClick={onDiscard}>
                    Cancel
                </Button>,
            ]}
        >
            <TextField onChange={(e) => setNewLabel(e.target.value)} value={newLabel} />
        </SimpleDialog>
    );
}
