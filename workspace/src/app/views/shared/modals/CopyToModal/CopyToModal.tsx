import React from "react";

//components
import {
    Accordion,
    AccordionItem,
    SuggestField,
    Button,
    Checkbox,
    FieldItem,
    Link,
    Notification,
    OverviewItem,
    OverviewItemList,
    OverviewItemDescription,
    OverviewItemDepiction,
    OverviewItemLine,
    SimpleDialog,
    Spacing,
    TitleSubsection,
    Tooltip,
} from "@eccenca/gui-elements";
import { ICloneOptions } from "../CloneModal";
import { useTranslation } from "react-i18next";
import { requestProjectMetadata, requestTaskMetadata } from "@ducks/shared/requests";
import { requestCopyProject, requestCopyTask, requestSearchList } from "@ducks/workspace/requests";
import ItemDepiction from "../../ItemDepiction";
import { ErrorResponse, FetchError } from "../../../../services/fetch/responseInterceptor";
import { useModalError } from "../../../../hooks/useModalError";

//Component Interface
interface CopyToModalProps extends ICloneOptions {
    onConfirmed: () => void;
}

interface CopyPayloadProps {
    targetProject: string;
    dryRun: boolean;
    overwriteTasks?: boolean;
}

interface ItemResponseType {
    id: string;
    label: string;
    originalTaskLink: string;
    overwrittenTaskLink?: string;
    taskType: string;
    pluginId: string;
}

interface CopyResponsePayload {
    overwrittenTasks: Array<ItemResponseType>;
    copiedTasks: Array<ItemResponseType>;
}

const CopyToModal: React.FC<CopyToModalProps> = ({ item, onDiscard, onConfirmed }) => {
    const [newLabel, setNewLabel] = React.useState<string>(item.label || item.id || "");
    const [error, setError] = React.useState<ErrorResponse | null>(null);
    const checkAndDisplayError = useModalError({ setError });
    const [loading, setLoading] = React.useState<boolean>(false);
    const [label, setLabel] = React.useState<string | undefined>(item.label);
    const [targetProject, setTargetProject] = React.useState<string | undefined>(undefined);
    const [info, setInfo] = React.useState<CopyResponsePayload | undefined>();
    const [overWrittenAcknowledgement, setOverWrittenAcknowledgement] = React.useState(false);

    const [t] = useTranslation();

    React.useEffect(() => {
        copyingSetup();
    }, [item]);

    /** remove the same project from possible project targets in the selection menu */
    const removeFromList = (list: Array<any>): Array<any> => {
        /** if task filter using project label */
        if (item.id && item.projectLabel) {
            return list.filter((l) => l.label !== item.projectLabel);
        } else {
            // else if project artefact
            return list.filter((l) => l.label !== label);
        }
    };

    const copyingSetup = async () => {
        setLoading(true);
        try {
            const response =
                item.projectId && item.id
                    ? await requestTaskMetadata(item.id, item.projectId)
                    : await requestProjectMetadata(item.projectId as string);

            const currentLabel = !!response.data.label ? response.data.label : item.id ? item.id : item.projectId;
            setLabel(currentLabel as string);
            setNewLabel(t("common.messages.cloneOf", { item: currentLabel }));
        } catch (ex) {
            // swallow exception, fallback to ID
        } finally {
            setLoading(false);
        }
    };

    const copyTaskOrProject = async (projectId: string, payload: CopyPayloadProps, id?: string) => {
        const response = id
            ? await requestCopyTask(projectId, id, payload)
            : await requestCopyProject(projectId, payload);
        return response;
    };

    const handleCopyingAction = async () => {
        if (!targetProject) {
            return;
        }
        const { projectId, id } = item;
        setError(null);
        try {
            setLoading(true);
            const payload: CopyPayloadProps = {
                targetProject: targetProject,
                dryRun: false,
                overwriteTasks: overWrittenAcknowledgement,
            };
            await copyTaskOrProject(projectId, payload, id);
            onConfirmed();
        } catch (e) {
            checkAndDisplayError(e, t("copyModal.errorMessages.searchFailure", "Retrieving item list failed"));
        } finally {
            setLoading(false);
        }
    };

    const handleSearch: (value: string) => Promise<any[]> = async (textQuery: string) => {
        setError(null); //reset modal error here
        try {
            const payload = {
                limit: 50,
                offset: 0,
                itemType: "project",
                textQuery,
                sortBy: "label",
            };
            const results = (await requestSearchList(payload)).results;
            return removeFromList(results);
        } catch (err) {
            checkAndDisplayError(err, t("copyModal.errorMessages.copyFailure", "Failed to copy"));
            return [];
        }
    };

    /** this orders the tasks in the accordion by the default/typical order in the DI project space
     *  1.Project
     *  2. Workflow
     *  3. Dataset
     *  4. Transform
     *  5. Linking
     *  6. Task
     */
    const orderTasksByLabel = (items: Array<ItemResponseType>) => {
        const order = {
            project: 1,
            workflow: 2,
            dataset: 3,
            transform: 4,
            linking: 5,
            task: 6,
        };
        return items.sort((a, b) => order[a.taskType.toLowerCase()] - order[b.taskType.toLowerCase()]);
    };

    const modalTitle = item.id ? t("common.action.CopyItems") : t("common.action.CopyProject");
    const [copiedTasks, overwrittenTasks] = [info?.copiedTasks.length ?? 0, info?.overwrittenTasks.length ?? 0];
    const buttonDisabled = !newLabel || (info?.overwrittenTasks.length && !overWrittenAcknowledgement);
    return (
        <SimpleDialog
            data-test-id={"copy-item-to-modal"}
            size="small"
            title={modalTitle}
            isOpen={true}
            canEscapeKeyClose={true}
            onClose={onDiscard}
            actions={[
                <Button
                    key="copy"
                    affirmative
                    onClick={handleCopyingAction}
                    disabled={!!buttonDisabled || loading || !targetProject}
                    loading={loading}
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
                key={"copy-label"}
                labelProps={{
                    htmlFor: "copy-label",
                    text: t("common.messages.copyModalProjectSelect"),
                }}
            >
                <SuggestField
                    autoFocus={true}
                    onSearch={handleSearch}
                    onChange={async (value) => {
                        setTargetProject(value);
                        const { projectId, id } = item;
                        const payload = {
                            targetProject: value,
                            dryRun: true,
                        };
                        const response = await copyTaskOrProject(projectId, payload, id);
                        setInfo((prevInfo) => ({ ...prevInfo, ...response?.data }));
                    }}
                    itemValueRenderer={(item) => item.label}
                    itemValueSelector={(item: any) => item.id}
                    itemValueString={(item) => item.id}
                    itemRenderer={(item) => item.label}
                    noResultText={t("common.messages.noItems", {
                        items: t("common.dataTypes.project"),
                    })}
                />
            </FieldItem>

            {info && (
                <Accordion>
                    <AccordionItem
                        label={
                            <TitleSubsection>
                                {t("common.messages.copyModalOverwrittenTasks", {
                                    tasks: overwrittenTasks,
                                })}
                            </TitleSubsection>
                        }
                        fullWidth
                        elevated
                        open
                    >
                        <OverviewItemList hasSpacing densityHigh>
                            {orderTasksByLabel(info.overwrittenTasks)?.map((t) => (
                                <OverviewItem key={t.id} className="copy-modal-item">
                                    <OverviewItemDepiction>
                                        <ItemDepiction itemType={t.taskType} pluginId={t.pluginId} />
                                    </OverviewItemDepiction>
                                    <OverviewItemDescription>
                                        <OverviewItemLine>
                                            <Tooltip content={`Open ${t.taskType} "${t.label}" in a new window`}>
                                                <Link href={t.originalTaskLink} target="_blank">
                                                    {t.label}
                                                </Link>
                                            </Tooltip>
                                        </OverviewItemLine>
                                        <OverviewItemLine>
                                            <Tooltip
                                                content={`Open to-be-replaced ${t.taskType} "${t.label}" in a new window`}
                                            >
                                                <Link href={t.overwrittenTaskLink} target="_blank">
                                                    To be overwritten: {t.label}
                                                </Link>
                                            </Tooltip>
                                        </OverviewItemLine>
                                    </OverviewItemDescription>
                                </OverviewItem>
                            ))}
                        </OverviewItemList>
                    </AccordionItem>
                    <AccordionItem
                        label={
                            <TitleSubsection>
                                {t("common.messages.copyModalCopiedTasks", {
                                    tasks: copiedTasks,
                                })}
                            </TitleSubsection>
                        }
                        fullWidth
                        open={false}
                    >
                        <OverviewItemList hasSpacing densityHigh>
                            {orderTasksByLabel(info.copiedTasks)?.map((item) => (
                                <OverviewItem key={item.id} className="copy-modal-item">
                                    <OverviewItemDepiction>
                                        <ItemDepiction itemType={item.taskType} pluginId={item.pluginId} />
                                    </OverviewItemDepiction>
                                    <OverviewItemDescription>
                                        <OverviewItemLine>
                                            <Link href={item.originalTaskLink} target="_blank">
                                                <Tooltip content={t("common.action.openInNewTab")}>
                                                    {item.label}
                                                </Tooltip>
                                            </Link>
                                        </OverviewItemLine>
                                    </OverviewItemDescription>
                                </OverviewItem>
                            ))}
                        </OverviewItemList>
                    </AccordionItem>
                </Accordion>
            )}
            <Spacing size="large" />
            {info?.overwrittenTasks.length ? (
                <Checkbox
                    data-test-id={"overwrite-tasks-checkbox"}
                    checked={overWrittenAcknowledgement}
                    onChange={() => setOverWrittenAcknowledgement(!overWrittenAcknowledgement)}
                >
                    {t("common.messages.taskOverwrittenPrompt")}
                </Checkbox>
            ) : null}
            {error && (
                <>
                    <Spacing />
                    <Notification message={error.detail} danger />
                </>
            )}
        </SimpleDialog>
    );
};

export default CopyToModal;
