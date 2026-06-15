import React, { useState } from "react";
import { useSelector } from "react-redux";
import { IDetailedProjectPrefixes, IPrefixDefinition } from "@ducks/workspace/typings";
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
import { requestChangePrefixes, requestRemoveProjectPrefix } from "@ducks/workspace/requests";
import { ErrorResponse } from "../../../../services/fetch/responseInterceptor";
import { useModalError } from "../../../../hooks/useModalError";
import Loading from "../../../shared/Loading";
import styles from "./index.module.scss";

interface IProps {
    projectId: string;
    onCloseModal: () => any;
    isOpen: boolean;
    projectPrefixes: IPrefixDefinition[];
    workspacePrefixes: IPrefixDefinition[];
    refreshPrefixes: () => Promise<IDetailedProjectPrefixes>;
}

const projectPrefixRowId = (prefixName: string): string => `project-prefix-${encodeURIComponent(prefixName)}`;

/** Manages project prefix definitions. */
const PrefixesDialog = ({
    onCloseModal,
    isOpen,
    projectId,
    projectPrefixes,
    workspacePrefixes,
    refreshPrefixes,
}: IProps) => {
    const { dmBaseUrl } = useSelector(commonSel.initialSettingsSelector);
    const [loading, setLoading] = React.useState<boolean>(false);
    const [error, setError] = React.useState<ErrorResponse | undefined>();
    const checkAndDisplayPrefixError = useModalError({ setError });

    const [isOpenRemove, setIsOpenRemove] = useState<boolean>(false);
    const [selectedPrefix, setSelectedPrefix] = useState<IPrefixDefinition | undefined>(undefined);
    const [highlightedProjectPrefix, setHighlightedProjectPrefix] = useState<string | undefined>(undefined);

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

    React.useEffect(() => {
        if (!highlightedProjectPrefix) {
            return undefined;
        }
        const timeoutId = window.setTimeout(() => setHighlightedProjectPrefix(undefined), 1800);
        return () => window.clearTimeout(timeoutId);
    }, [highlightedProjectPrefix]);

    const handleConfirmRemove = React.useCallback(async () => {
        try {
            setLoading(true);
            if (selectedPrefix) {
                setError(undefined);
                await requestRemoveProjectPrefix(selectedPrefix.prefixName, projectId);
                await refreshPrefixes();
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
    }, [checkAndDisplayPrefixError, projectId, refreshPrefixes, selectedPrefix, t]);

    const handleAddOrUpdatePrefix = React.useCallback(
        async (prefix: IPrefixDefinition) => {
            try {
                setLoading(true);
                setError(undefined);
                const { prefixName, prefixUri } = prefix;
                await requestChangePrefixes(prefixName, JSON.stringify(prefixUri), projectId);
                await refreshPrefixes();
            } catch (err) {
                checkAndDisplayPrefixError(
                    err,
                    t("widget.ConfigWidget.modal.errors.prefixChangeFailure", "Prefix change failed"),
                );
            } finally {
                setLoading(false);
            }
        },
        [checkAndDisplayPrefixError, projectId, refreshPrefixes, t],
    );

    const existingProjectPrefixes = React.useMemo(
        () => new Set(projectPrefixes.map((prefix) => prefix.prefixName)),
        [projectPrefixes],
    );
    const existingWorkspacePrefixes = React.useMemo(
        () => new Set(workspacePrefixes.map((prefix) => prefix.prefixName)),
        [workspacePrefixes],
    );
    const overriddenWorkspacePrefixes = React.useMemo(
        () =>
            new Set(
                workspacePrefixes
                    .map((prefix) => prefix.prefixName)
                    .filter((prefixName) => existingProjectPrefixes.has(prefixName)),
            ),
        [existingProjectPrefixes, workspacePrefixes],
    );

    const workspaceVocabUrl = React.useMemo(() => {
        if (!dmBaseUrl) {
            return undefined;
        }
        return `${dmBaseUrl.replace(/\/+$/, "")}/vocab`;
    }, [dmBaseUrl]);

    const jumpToProjectPrefix = React.useCallback((prefixName: string) => {
        const targetRow = document.getElementById(projectPrefixRowId(prefixName));
        setHighlightedProjectPrefix(prefixName);
        targetRow?.scrollIntoView({
            behavior: "smooth",
            block: "center",
        });
    }, []);

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
                                    rowId={projectPrefixRowId(prefix.prefixName)}
                                    rowClassName={
                                        highlightedProjectPrefix === prefix.prefixName
                                            ? styles.highlightedPrefixRow
                                            : undefined
                                    }
                                    prefix={prefix}
                                    ownership="project"
                                    overridesWorkspacePrefix={existingWorkspacePrefixes.has(prefix.prefixName)}
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
                                    <PrefixRow
                                        key={i}
                                        prefix={prefix}
                                        ownership="workspace"
                                        overriddenInProject={overriddenWorkspacePrefixes.has(prefix.prefixName)}
                                        onJumpToProjectPrefix={
                                            overriddenWorkspacePrefixes.has(prefix.prefixName)
                                                ? () => jumpToProjectPrefix(prefix.prefixName)
                                                : undefined
                                        }
                                    />
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
