import React, { useState } from "react";
import { batch, useDispatch, useSelector } from "react-redux";
import { IPrefixDefinition } from "@ducks/workspace/typings";
import { workspaceSel } from "@ducks/workspace";
import { commonSel } from "@ducks/common";
import {
    Button,
    Divider,
    HtmlContentBlock,
    Notification,
    Section,
    SectionHeader,
    SimpleDialog,
    Spacing,
    TitleSubsection,
} from "@eccenca/gui-elements";
import PrefixRow from "./PrefixRow";
import DeleteModal from "../../../shared/modals/DeleteModal";
import PrefixNew from "./PrefixNew";
import DataList from "../../../shared/Datalist";
import { useTranslation } from "react-i18next";
import { updatePrefixLists } from "@ducks/workspace/widgets/configuration.thunk";
import {
    requestChangePrefixes,
    requestDetailedProjectPrefixes,
    requestRemoveProjectPrefix,
} from "@ducks/workspace/requests";
import { widgetsSlice } from "@ducks/workspace/widgetsSlice";
import { ErrorResponse } from "../../../../services/fetch/responseInterceptor";
import { useModalError } from "../../../../hooks/useModalError";
import { AppDispatch } from "store/configureStore";
import Loading from "../../../shared/Loading";

interface IProps {
    projectId: string;
    onCloseModal: () => any;
    isOpen: boolean;
}

/** Manages project prefix definitions. */
const PrefixesDialog = ({ onCloseModal, isOpen, projectId }: IProps) => {
    const dispatch = useDispatch<AppDispatch>();
    const projectPrefixes = useSelector(workspaceSel.projectPrefixListSelector);
    const workspacePrefixes = useSelector(workspaceSel.workspacePrefixListSelector);
    const { dmBaseUrl } = useSelector(commonSel.initialSettingsSelector);
    const [loading, setLoading] = React.useState<boolean>(false);
    const [error, setError] = React.useState<ErrorResponse | undefined>();
    const checkAndDisplayPrefixError = useModalError({ setError });

    const [isOpenRemove, setIsOpenRemove] = useState<boolean>(false);
    const [selectedPrefix, setSelectedPrefix] = useState<IPrefixDefinition | undefined>(undefined);

    const [t] = useTranslation();

    const toggleRemoveDialog = (prefix?: IPrefixDefinition) => {
        if (!prefix || isOpenRemove) {
            setIsOpenRemove(false);
            setSelectedPrefix(undefined);
        } else {
            setIsOpenRemove(true);
            setSelectedPrefix(prefix);
        }
        setError(undefined);
    };

    React.useEffect(() => {
        setError(undefined);
    }, [isOpen]);

    const refreshPrefixLists = React.useCallback(async () => {
        const { data } = await requestDetailedProjectPrefixes(projectId);
        dispatch(updatePrefixLists(data));
    }, [dispatch, projectId]);

    const handleConfirmRemove = React.useCallback(async () => {
        try {
            setLoading(true);
            if (selectedPrefix) {
                setError(undefined);
                await requestRemoveProjectPrefix(selectedPrefix.prefixName, projectId);
                await refreshPrefixLists();
                toggleRemoveDialog();
            }
        } catch (err) {
            checkAndDisplayPrefixError(
                err,
                t("widget.ConfigWidget.modal.errors.prefixDeletionFailure", "Prefix deletion failed"),
            );
        } finally {
            setLoading(false);
        }
    }, [checkAndDisplayPrefixError, projectId, refreshPrefixLists, selectedPrefix, t]);

    const handleAddOrUpdatePrefix = React.useCallback(
        async (prefix: IPrefixDefinition) => {
            try {
                setLoading(true);
                setError(undefined);
                const { prefixName, prefixUri } = prefix;
                await requestChangePrefixes(prefixName, JSON.stringify(prefixUri), projectId);
                const { data } = await requestDetailedProjectPrefixes(projectId);
                batch(() => {
                    dispatch(widgetsSlice.actions.resetNewPrefix());
                    dispatch(updatePrefixLists(data));
                });
            } catch (err) {
                checkAndDisplayPrefixError(
                    err,
                    t("widget.ConfigWidget.modal.errors.prefixChangeFailure", "Prefix change failed"),
                );
            } finally {
                setLoading(false);
            }
        },
        [checkAndDisplayPrefixError, dispatch, projectId, t],
    );

    const existingProjectPrefixes = React.useMemo(
        () => new Set(projectPrefixes.map((prefix) => prefix.prefixName)),
        [projectPrefixes],
    );
    const existingWorkspacePrefixes = React.useMemo(
        () => new Set(workspacePrefixes.map((prefix) => prefix.prefixName)),
        [workspacePrefixes],
    );

    const workspaceVocabUrl = React.useMemo(() => {
        if (!dmBaseUrl) {
            return undefined;
        }
        return `${dmBaseUrl.replace(/\/+$/, "")}/vocab`;
    }, [dmBaseUrl]);

    const workspaceSectionDescription = workspaceVocabUrl ? (
        <p>
            {t("PrefixDialog.workspacePrefixesDescription", "Workspace prefixes are managed in ")}
            <a href={workspaceVocabUrl} rel="noreferrer" target="_blank">
                {t("navigation.side.dmBrowser", "Explore")}
            </a>
            {t(
                "PrefixDialog.workspacePrefixesDescriptionSuffix",
                " and are automatically registered from vocabularies in Explore's vocabulary module.",
            )}
        </p>
    ) : null;

    const workspaceEmptyMessage = t(
        "PrefixDialog.workspacePrefixesEmpty",
        "No workspace prefixes are currently registered. Manage them in Explore's vocabulary module.",
    );

    return (
        <SimpleDialog
            title={t("widget.ConfigWidget.prefixTitle", "Manage Prefixes")}
            data-test-id={"prefix-dialog"}
            isOpen={isOpen}
            onClose={onCloseModal}
            actions={
                <Button data-test-id={"close-prefix-dialog-btn"} onClick={() => onCloseModal()}>
                    {t("common.action.close")}
                </Button>
            }
            notifications={error ? <Notification intent="danger">{error.detail}</Notification> : null}
        >
            <PrefixNew
                onAdd={(newPrefix: IPrefixDefinition) => handleAddOrUpdatePrefix(newPrefix)}
                existingProjectPrefixes={existingProjectPrefixes}
                existingWorkspacePrefixes={existingWorkspacePrefixes}
            />
            {loading && <Loading description={t("widget.ConfigWidget.loading", "Loading configuration list.")} />}
            {!loading && (
                <>
                    <Section>
                        <SectionHeader>
                            <TitleSubsection>
                                {t("PrefixDialog.projectPrefixesTitle", "Project prefixes")}
                            </TitleSubsection>
                            <HtmlContentBlock small>
                                <p>
                                    {t(
                                        "PrefixDialog.projectPrefixesDescription",
                                        "Project prefixes can be added, updated, and removed here. They are exported with the project.",
                                    )}
                                </p>
                            </HtmlContentBlock>
                        </SectionHeader>
                        <Divider addSpacing="medium" />
                        <DataList
                            isEmpty={!projectPrefixes.length}
                            isLoading={false}
                            hasSpacing
                            hasDivider
                            emptyListMessage={t(
                                "PrefixDialog.projectPrefixesEmpty",
                                "No project prefixes have been defined yet.",
                            )}
                        >
                            {projectPrefixes.map((prefix, i) => (
                                <PrefixRow
                                    key={i}
                                    prefix={prefix}
                                    ownership="project"
                                    onRemove={() => toggleRemoveDialog(prefix)}
                                />
                            ))}
                        </DataList>
                    </Section>
                    <Spacing />
                    {workspaceVocabUrl && (
                        <Section>
                            <SectionHeader>
                                <TitleSubsection>
                                    {t("PrefixDialog.workspacePrefixesTitle", "Workspace prefixes")}
                                </TitleSubsection>
                                <HtmlContentBlock small>{workspaceSectionDescription}</HtmlContentBlock>
                            </SectionHeader>
                            <Divider addSpacing="medium" />
                            <DataList
                                isEmpty={!workspacePrefixes.length}
                                isLoading={false}
                                hasSpacing
                                hasDivider
                                emptyListMessage={workspaceEmptyMessage}
                            >
                                {workspacePrefixes.map((prefix, i) => (
                                    <PrefixRow key={i} prefix={prefix} ownership="workspace" />
                                ))}
                            </DataList>
                        </Section>
                    )}
                </>
            )}
            <DeleteModal
                isOpen={isOpenRemove}
                data-test-id={"update-prefix-dialog"}
                onDiscard={() => toggleRemoveDialog()}
                onConfirm={handleConfirmRemove}
                title={t("common.action.DeleteSmth", { smth: t("widget.ConfigWidget.prefix") })}
                errorMessage={error ? error.detail : undefined}
            >
                <p>{t("PrefixDialog.deletePrefix", { prefixName: selectedPrefix ? selectedPrefix.prefixName : "" })}</p>
            </DeleteModal>
        </SimpleDialog>
    );
};

export default PrefixesDialog;
