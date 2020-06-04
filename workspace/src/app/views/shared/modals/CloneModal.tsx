import React, { useEffect, useState } from "react";
import { Button, FieldItem, SimpleDialog, TextField } from "@wrappers/index";
import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";
import { requestCloneProject, requestCloneTask } from "@ducks/workspace/requests";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { requestProjectMetadata, requestTaskMetadata } from "@ducks/shared/requests";
import { Loading } from "../Loading/Loading";

export interface ICloneOptions {
    item: Partial<ISearchResultsServer>;

    onDiscard(): void;

    onConfirmed?(newLabel: string, detailsPage: string): void;
}

export default function CloneModal({ item, onDiscard, onConfirmed }: ICloneOptions) {
    const [newLabel, setNewLabel] = useState(item.label || item.id);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ErrorResponse>({} as ErrorResponse);
    const [label, setLabel] = useState<string | null>(item.label);

    useEffect(() => {
        prepareCloning();
    }, [item]);

    const prepareCloning = async () => {
        setLoading(true);
        try {
            const response =
                item.projectId && item.id
                    ? await requestTaskMetadata(item.id, item.projectId)
                    : await requestProjectMetadata(item.id ? item.id : item.projectId);
            setLabel(response.data.label);
            setNewLabel(response.data.label);
        } catch (ex) {
            // swallow exception, fallback to ID
        } finally {
            setLoading(false);
        }
    };

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

            const response = projectId
                ? await requestCloneTask(id, projectId, payload)
                : await requestCloneProject(id, payload);
            onConfirmed && onConfirmed(newLabel, response.data.detailsPage);
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

    return loading ? (
        <Loading />
    ) : (
        <SimpleDialog
            size="small"
            title={`Clone ${item.projectId ? "task" : "project"} '${label || item.label || item.id}'`}
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
            <FieldItem
                key={"label"}
                labelAttributes={{
                    htmlFor: "label",
                    text: `Label of cloned ${item.projectId ? "task" : "project"}:`,
                }}
            >
                <TextField onChange={(e) => setNewLabel(e.target.value)} value={newLabel} />
            </FieldItem>
        </SimpleDialog>
    );
}
