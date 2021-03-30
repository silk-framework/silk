import React from "react";

//components
import {
    AutoCompleteField,
    Button,
    FieldItem,
    Notification,
    Select,
    SimpleDialog,
    Spacing,
    TextField,
} from "@gui-elements/index";
import { Loading } from "../Loading/Loading";
import { ICloneOptions } from "./CloneModal";
import { ErrorResponse, FetchError } from "services/fetch/responseInterceptor";
import { useTranslation } from "react-i18next";
import { requestProjectMetadata, requestTaskMetadata } from "@ducks/shared/requests";
import { requestCopyProject, requestCopyTask, requestSearchList } from "@ducks/workspace/requests";

//Component Interface
interface CopyToModalProps extends ICloneOptions {
    onConfirmed: () => void;
}

const CopyToModal: React.FC<CopyToModalProps> = ({ item, onDiscard, onConfirmed }) => {
    const [newLabel, setNewLabel] = React.useState<string>(item.label || item.id);
    const [error, setError] = React.useState<ErrorResponse | null>(null);
    const [loading, setLoading] = React.useState<boolean>(false);
    const [label, setLabel] = React.useState<string | null>(item.label);
    const [targetProject, setTargetProject] = React.useState();
    const [t] = useTranslation();

    React.useEffect(() => {
        copyingSetup();
    }, [item]);

    const copyingSetup = async () => {
        setLoading(true);
        try {
            const response =
                item.projectId && item.id
                    ? await requestTaskMetadata(item.id, item.projectId)
                    : await requestProjectMetadata(item.projectId);
            /**
             * response - project
             * item - item of a project
             */
            const currentLabel = !!response.data.label ? response.data.label : !!item.id ? item.id : item.projectId;
            setLabel(currentLabel);
            setNewLabel(t("common.messages.cloneOf", { item: currentLabel }));
        } catch (ex) {
            // swallow exception, fallback to ID
        } finally {
            setLoading(false);
        }
    };

    const handleCopyingAction = async () => {
        const { projectId, id } = item;
        setError(null);
        try {
            setLoading(true);
            const payload = {
                targetProject,
                dryRun: false,
            };
            const response = id
                ? await requestCopyTask(projectId, id, payload)
                : await requestCopyProject(projectId, payload);
            console.log({ response });
            onConfirmed();
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

    const handleSearch = async (textQuery: string) => {
        try {
            const payload = {
                limit: 10,
                offset: 0,
                itemType: "project",
                textQuery,
            };
            const results = await (await requestSearchList(payload)).results;
            return results;
        } catch (err) {
            // do nothing
        }
    };

    if (loading) {
        return <Loading />;
    }

    return (
        <SimpleDialog
            size="small"
            title={
                t("common.action.CopySmth", {
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
                    onClick={handleCopyingAction}
                    disabled={!newLabel}
                    data-test-id={"copy-modal-button"}
                >
                    {t("common.action.copy")}
                </Button>,
                <Button key="cancel" onClick={onDiscard}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <FieldItem
                key={"label"}
                labelAttributes={{
                    htmlFor: "label",
                    text: t("common.messages.copyModalTitle", {
                        item: item.id ? t("common.dataTypes.task") : t("common.dataTypes.project"),
                    }),
                }}
            >
                <TextField onChange={(e) => setNewLabel(e.target.value)} value={newLabel} />
            </FieldItem>
            <FieldItem
                key={"copy-label"}
                labelAttributes={{
                    htmlFor: "copy-label",
                    text: t("common.messages.copyModalProjectSelect"),
                }}
            >
                <AutoCompleteField
                    onSearch={handleSearch}
                    onChange={(value) => setTargetProject(value)}
                    itemValueRenderer={(item) => item.label}
                    itemValueSelector={(item: any) => item.id}
                    itemRenderer={(item) => item.label}
                    noResultText={t("common.messages.noItems", {
                        items: t("common.dataTypes.project"),
                    })}
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
};

export default CopyToModal;
