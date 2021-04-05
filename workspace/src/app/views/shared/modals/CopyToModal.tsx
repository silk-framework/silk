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
import { debounce } from "../../../utils/debounce";

//Component Interface
interface CopyToModalProps extends ICloneOptions {
    onConfirmed: () => void;
}

interface CopyPayloadProps {
    targetProject: string;
    dryRun: boolean;
}

const CopyToModal: React.FC<CopyToModalProps> = ({ item, onDiscard, onConfirmed }) => {
    const [newLabel, setNewLabel] = React.useState<string>(item.label || item.id);
    const [error, setError] = React.useState<ErrorResponse | null>(null);
    const [loading, setLoading] = React.useState<boolean>(false);
    const [label, setLabel] = React.useState<string | null>(item.label);
    const [targetProject, setTargetProject] = React.useState();
    const [results, setResults] = React.useState<any[]>([]);
    const [info, setInfo] = React.useState<string>("");

    const [t] = useTranslation();

    /*****************@TODO refactor useEffect calls into separate hooks, maybe :) *****************/
    React.useEffect(() => {
        copyingSetup();
    }, [item]);

    const removeFromList = (list: Array<any>) => {
        /** if task filter using project label */
        if (item.id && item.projectLabel) {
            return list.filter((l) => l.label !== item.projectLabel);
        } else {
            // else if project artefact
            return list.filter((l) => l.label !== label);
        }
    };

    //preload the project lists with default data
    React.useEffect(() => {
        (async () => {
            const payload = {
                limit: 10,
                offset: 0,
                itemType: "project",
            };
            setResults(removeFromList(await (await requestSearchList(payload)).results));
        })();
    }, [item, label]);

    /***************** END of side effects *****************/
    /**
     *
     * @param info
     * @returns {String}
     */
    const compressInfoToReadableText = (info: {
        overwrittenTasks: Array<string>;
        copiedTasks: Array<string>;
    }): string => {
        return `Copying (${info.copiedTasks.length}) tasks, ${info.copiedTasks.join(",")}, Overwriting (${
            info.overwrittenTasks.length
        }) ${info.overwrittenTasks.join(",")}`;
    };

    const copyingSetup = async () => {
        setLoading(true);
        try {
            const response =
                item.projectId && item.id
                    ? await requestTaskMetadata(item.id, item.projectId)
                    : await requestProjectMetadata(item.projectId);

            const currentLabel = !!response.data.label ? response.data.label : !!item.id ? item.id : item.projectId;
            setLabel(currentLabel);
            setNewLabel(t("common.messages.cloneOf", { item: currentLabel }));
        } catch (ex) {
            // swallow exception, fallback to ID
        } finally {
            setLoading(false);
        }
    };

    const copyTaskOrProject = async (id: string, projectId: string, payload: CopyPayloadProps) => {
        const response = id
            ? await requestCopyTask(projectId, id, payload)
            : await requestCopyProject(projectId, payload);
        return response;
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
            await copyTaskOrProject(id, projectId, payload);
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

    const handleSearch = debounce(async (textQuery: string) => {
        try {
            const payload = {
                limit: 10,
                offset: 0,
                itemType: "project",
                textQuery,
            };
            setResults(removeFromList(await (await requestSearchList(payload)).results));
        } catch (err) {
            console.warn({ err });
        }
    }, 500);

    if (loading) {
        return <Loading />;
    }

    const modalTitle = item.id ? t("common.action.CopyItems") : t("common.action.CopyProject");
    return (
        <SimpleDialog
            size="small"
            title={modalTitle}
            isOpen={true}
            onClose={onDiscard}
            actions={[
                <Button
                    key="copy"
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
            {/** will be added in subsequent iterations */}
            {/* <FieldItem
                key={"label"}
                labelAttributes={{
                    htmlFor: "label",
                    text: t("common.messages.copyModalTitle", {
                        item: item.id ? t("common.dataTypes.task") : t("common.dataTypes.project"),
                    }),
                }}
            >
                <TextField onChange={(e) => setNewLabel(e.target.value)} value={newLabel} />
            </FieldItem> */}
            <FieldItem
                key={"copy-label"}
                labelAttributes={{
                    htmlFor: "copy-label",
                    text: t("common.messages.copyModalProjectSelect"),
                }}
            >
                <AutoCompleteField
                    onSearch={(textQuery: string) => {
                        handleSearch(textQuery);
                        return results;
                    }}
                    onChange={async (value) => {
                        setTargetProject(value);
                        const { projectId, id } = item;
                        const payload = {
                            targetProject: value,
                            dryRun: true,
                        };
                        const response = await copyTaskOrProject(id, projectId, payload);
                        setInfo(compressInfoToReadableText(response?.data));
                    }}
                    itemValueRenderer={(item) => item.label}
                    itemValueSelector={(item: any) => item.id}
                    itemRenderer={(item) => item.label}
                    noResultText={t("common.messages.noItems", {
                        items: t("common.dataTypes.project"),
                    })}
                />
            </FieldItem>

            {info && (
                <>
                    <Spacing />
                    <Notification message={info} warning />
                </>
            )}
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
