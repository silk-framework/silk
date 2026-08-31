import React from "react";
import { useTranslation } from "react-i18next";
import {
    IconButton,
    Notification,
    Spacing,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableHeader,
    TableRow,
    Tag,
    TagList,
} from "@eccenca/gui-elements";
import { usePagination } from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import Loading from "../../shared/Loading";
import DeleteModal from "../../shared/modals/DeleteModal";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useModalError } from "../../../hooks/useModalError";
import { ErrorResponse } from "../../../services/fetch/responseInterceptor";
import { IChangeEntry, requestProjectChanges, requestRevertChange } from "./changesRequests";

interface IProps {
    projectId: string;
    /** Increment to reload the list from the outside. */
    refreshKey?: number;
}

/** The last segment of a user URI, e.g. 'alice' for 'urn:user:alice'. */
const userDisplayName = (uri: string): string => {
    const idx = Math.max(uri.lastIndexOf("/"), uri.lastIndexOf(":"), uri.lastIndexOf("#"));
    return idx >= 0 && idx < uri.length - 1 ? uri.substring(idx + 1) : uri;
};

/** The changes of a project, newest first, with a revert action per entry. */
const ChangeList = ({ projectId, refreshKey = 0 }: IProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [entries, setEntries] = React.useState<IChangeEntry[]>([]);
    const [loading, setLoading] = React.useState<boolean>(true);
    const [revertEntry, setRevertEntry] = React.useState<IChangeEntry | undefined>(undefined);
    const [revertLoading, setRevertLoading] = React.useState<boolean>(false);
    const [revertError, setRevertError] = React.useState<ErrorResponse | undefined>(undefined);
    const displayRevertError = useModalError({ setError: setRevertError });
    const [pagination, paginationElement, onTotalChange] = usePagination({
        pageSizes: [25, 50, 100],
        initialPageSize: 25,
    });

    const loadChanges = React.useCallback(async () => {
        setLoading(true);
        try {
            const response = await requestProjectChanges(projectId);
            setEntries(response.data.changes);
            onTotalChange(response.data.changes.length);
        } catch (ex) {
            registerError("ChangeList.loadChanges", t("pages.changes.errors.fetchChanges"), ex);
        } finally {
            setLoading(false);
        }
    }, [projectId, refreshKey]);

    React.useEffect(() => {
        loadChanges();
    }, [loadChanges]);

    const openRevertDialog = (entry: IChangeEntry) => {
        setRevertError(undefined);
        setRevertEntry(entry);
    };

    const revertChange = async () => {
        if (!revertEntry) {
            return;
        }
        setRevertLoading(true);
        try {
            await requestRevertChange(projectId, revertEntry.seq);
            setRevertEntry(undefined);
            await loadChanges();
        } catch (ex) {
            displayRevertError(ex, t("pages.changes.errors.revertChange"));
        } finally {
            setRevertLoading(false);
        }
    };

    const revertTooltip = (entry: IChangeEntry): string => {
        if (entry.revertedBy != null) {
            return t("pages.changes.revert.alreadyReverted", { seq: entry.revertedBy });
        } else if (!entry.revertible) {
            return t("pages.changes.revert.notRevertible");
        } else {
            return t("pages.changes.revert.action");
        }
    };

    if (loading && !entries.length) {
        return <Loading description={t("pages.changes.loading")} />;
    }

    if (!entries.length) {
        return <Notification message={t("pages.changes.noChanges")} />;
    }

    const pageEntries = entries.slice(
        (pagination.current - 1) * pagination.limit,
        pagination.current * pagination.limit,
    );

    return (
        <>
            <TableContainer>
                <Table columnWidths={["60px", "15%", "20%", "55%", "60px"]}>
                    <TableHead>
                        <TableRow>
                            <TableHeader>{t("pages.changes.column.seq")}</TableHeader>
                            <TableHeader>{t("pages.changes.column.date")}</TableHeader>
                            <TableHeader>{t("pages.changes.column.user")}</TableHeader>
                            <TableHeader>{t("pages.changes.column.change")}</TableHeader>
                            <TableHeader>{""}</TableHeader>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {pageEntries.map((entry) => (
                            <TableRow key={entry.seq}>
                                <TableCell alignVertical="middle">{entry.seq}</TableCell>
                                <TableCell alignVertical="middle">
                                    {new Date(entry.timestamp).toLocaleString()}
                                </TableCell>
                                <TableCell alignVertical="middle">
                                    {entry.user && <span title={entry.user}>{userDisplayName(entry.user)}</span>}
                                    {entry.origin && (
                                        <TagList>
                                            <Tag small htmlTitle={t("pages.changes.originTooltip")}>
                                                {entry.origin}
                                            </Tag>
                                        </TagList>
                                    )}
                                </TableCell>
                                <TableCell alignVertical="middle">
                                    <span title={entry.type}>{entry.description}</span>
                                    {(entry.reverts != null || entry.revertedBy != null) && (
                                        <TagList>
                                            {entry.reverts != null && (
                                                <Tag small>{t("pages.changes.revertsTag", { seq: entry.reverts })}</Tag>
                                            )}
                                            {entry.revertedBy != null && (
                                                <Tag small>
                                                    {t("pages.changes.revertedByTag", { seq: entry.revertedBy })}
                                                </Tag>
                                            )}
                                        </TagList>
                                    )}
                                </TableCell>
                                <TableCell alignVertical="middle">
                                    <IconButton
                                        data-test-id={`change-revert-btn-${entry.seq}`}
                                        name="operation-undo"
                                        small
                                        disruptive
                                        disabled={!entry.revertible || entry.revertedBy != null}
                                        text={revertTooltip(entry)}
                                        onClick={() => openRevertDialog(entry)}
                                    />
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
            {entries.length > Math.min(pagination.total, pagination.minPageSize) && (
                <>
                    <Spacing size="small" />
                    {paginationElement}
                </>
            )}
            {revertEntry && (
                <DeleteModal
                    data-test-id={"change-revert-modal"}
                    isOpen={true}
                    title={t("pages.changes.revert.title")}
                    alternativeDeleteButtonText={t("common.action.revert")}
                    removeLoading={revertLoading}
                    errorMessage={revertError?.detail}
                    onConfirm={revertChange}
                    onDiscard={() => setRevertEntry(undefined)}
                    render={() => (
                        <div>
                            <p>{t("pages.changes.revert.confirmText", { description: revertEntry.description })}</p>
                            <p>{t("pages.changes.revert.note")}</p>
                        </div>
                    )}
                />
            )}
        </>
    );
};

export default ChangeList;
