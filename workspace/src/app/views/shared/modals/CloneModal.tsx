import React, {KeyboardEventHandler, useEffect, useState} from "react";
import { Button, FieldItem, Notification, SimpleDialog, Spacing, TextField } from "@eccenca/gui-elements";
import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";
import { requestCloneProject, requestCloneTask } from "@ducks/workspace/requests";
import { requestProjectMetadata, requestTaskMetadata } from "@ducks/shared/requests";
import { Loading } from "../Loading/Loading";
import { useTranslation } from "react-i18next";
import { IModalItem } from "@ducks/shared/typings";
import useHotKey from "../HotKeyHandler/HotKeyHandler";

export interface ICloneOptions {
    item: IModalItem;

    onDiscard(): void;

    onConfirmed?(newLabel: string, detailsPage: string): void;
}

export default function CloneModal({ item, onDiscard, onConfirmed }: ICloneOptions) {
    // Value of the new label for the cloned project or task
    const [newLabel, setNewLabel] = useState(item.label || item.id || item.projectLabel || item.projectId);
    const [description, setDescription] = useState(item.description);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ErrorResponse | null>(null);
    // Label of the project or task that should be cloned
    const [label, setLabel] = useState<string | undefined>(item.label);
    const [t] = useTranslation();

    useEffect(() => {
        prepareCloning();
    }, [item]);

    const prepareCloning = async () => {
        setLoading(true);
        try {
            const response =
                item.projectId && item.id
                    ? await requestTaskMetadata(item.id, item.projectId)
                    : await requestProjectMetadata(item.projectId);
            const currentLabel = !!response.data.label ? response.data.label : !!item.id ? item.id : item.projectId;
            setLabel(currentLabel);
            setDescription(response.data.description);
            setNewLabel(t("common.messages.cloneOf", { item: currentLabel }));
        } catch (ex) {
            // swallow exception, fallback to ID
        } finally {
            setLoading(false);
        }
    };

    const handleCloning = async () => {
        const { projectId, id } = item;
        setError(null);
        try {
            setLoading(true);
            const payload = {
                metaData: {
                    label: newLabel,
                    description,
                },
            };

            const response = id
                ? await requestCloneTask(id, projectId, payload)
                : await requestCloneProject(projectId, payload);
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

    useHotKey({hotkey: "enter", handler: handleCloning})
    const enterHandler: KeyboardEventHandler<HTMLInputElement> = React.useCallback((event): void => {
        if(event.key === "Enter") {
            handleCloning()
        }
    }, [])

    return loading ? (
        <Loading delay={0} />
    ) : (
        <SimpleDialog
            data-test-id={"clone-item-to-modal"}
            size="small"
            title={
                t("common.action.CloneSmth", {
                    smth: t(item.id ? "common.dataTypes.task" : "common.dataTypes.project"),
                }) +
                    ": " +
                    label ||
                item.label ||
                item.id
            }
            isOpen={true}
            onClose={onDiscard}
            actions={[
                <Button
                    key="clone"
                    affirmative
                    onClick={handleCloning}
                    disabled={!newLabel}
                    data-test-id={"clone-modal-button"}
                >
                    {t("common.action.clone")}
                </Button>,
                <Button key="cancel" onClick={onDiscard}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <FieldItem
                key={"label"}
                labelProps={{
                    htmlFor: "label",
                    text: t("common.messages.cloneModalTitle", {
                        item: item.id ? t("common.dataTypes.task") : t("common.dataTypes.project"),
                    }),
                }}
            >
                <TextField
                    onChange={(e) => setNewLabel(e.target.value)}
                    value={newLabel}
                    autoFocus={true}
                    onKeyUp={enterHandler}
                />
            </FieldItem>
            {error && (
                <>
                    <Spacing />
                    <Notification message={error.asString()} danger />
                </>
            )}
        </SimpleDialog>
    );
}
