import React from "react";
import { useTranslation } from "react-i18next";
import {
    Button,
    ElapsedDateTimeDisplay,
    ElapsedDateTimeDisplayUnits,
    Icon,
    IconButton,
    NotAvailable,
    Notification,
    SimpleDialog,
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
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import { usePagination } from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import { ValidIconName } from "@eccenca/gui-elements/src/components/Icon/canonicalIconNames";
import { getDateData } from "../../shared/Metadata/Metadata";
import Loading from "../../shared/Loading";
import DeleteModal from "../../shared/modals/DeleteModal";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useModalError } from "../../../hooks/useModalError";
import { ErrorResponse } from "../../../services/fetch/responseInterceptor";
import {
    IChangeDetail,
    IChangeEntry,
    IRevertOutcome,
    requestMarkReviewed,
    requestProjectChanges,
    requestRevertChange,
    requestRevertChanges,
} from "./changesRequests";

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

type ChangeKind = "added" | "updated" | "removed" | "run";

/** The kind of change by its type name, e.g. 'AddMapping' adds, 'ResourceDeleted' removes, 'WorkflowExecuted' is a run. */
const changeKind = (type: string): ChangeKind => {
    if (type === "WorkflowExecuted" || type.endsWith("WorkflowRun")) {
        return "run";
    } else if (type.startsWith("Add") || type === "ResourceCreated") {
        return "added";
    } else if (type.startsWith("Remove") || type === "ResourceDeleted" || type === "DisconnectWorkflowNodes") {
        return "removed";
    } else {
        return "updated";
    }
};

const kindIntent: Record<ChangeKind, "success" | "danger" | "info" | undefined> = {
    added: "success",
    removed: "danger",
    run: "info",
    updated: undefined,
};

/** The icon of a link by its id, as handed out by the server. */
const linkIcon = (id: string): ValidIconName => {
    switch (id) {
        case "rule":
            return "application-mapping";
        case "report":
            return "artefact-report";
        case "download":
            return "item-download";
        default:
            return "item-viewdetails";
    }
};

/** The changes of a project, newest first, with a revert action per entry and review actions for the agent changes. */
const ChangeList = ({ projectId, refreshKey = 0 }: IProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [entries, setEntries] = React.useState<IChangeEntry[]>([]);
    const [loading, setLoading] = React.useState<boolean>(true);
    const [revertEntry, setRevertEntry] = React.useState<IChangeEntry | undefined>(undefined);
    const [revertLoading, setRevertLoading] = React.useState<boolean>(false);
    const [revertError, setRevertError] = React.useState<ErrorResponse | undefined>(undefined);
    const [markReviewedOpen, setMarkReviewedOpen] = React.useState<boolean>(false);
    const [revertUnreviewedOpen, setRevertUnreviewedOpen] = React.useState<boolean>(false);
    const [reviewLoading, setReviewLoading] = React.useState<boolean>(false);
    const [revertAllError, setRevertAllError] = React.useState<ErrorResponse | undefined>(undefined);
    const [revertAllSummary, setRevertAllSummary] = React.useState<
        { intent: "success" | "warning"; text: string } | undefined
    >(undefined);
    const displayRevertError = useModalError({ setError: setRevertError });
    const displayRevertAllError = useModalError({ setError: setRevertAllError });
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

    const unreviewedEntries = entries.filter((entry) => entry.unreviewed);
    // The batch attempts these; the server skips the rest of the unreviewed entries as not revertible.
    const revertableUnreviewed = unreviewedEntries.filter((entry) => entry.revertible);

    const markReviewed = async () => {
        setReviewLoading(true);
        try {
            // The latest fetched seq, so that entries that arrived after the page rendered are never approved unseen.
            await requestMarkReviewed(projectId, Math.max(...entries.map((entry) => entry.seq)));
            setMarkReviewedOpen(false);
            await loadChanges();
        } catch (ex) {
            registerError("ChangeList.markReviewed", t("pages.changes.errors.markReviewed"), ex);
            setMarkReviewedOpen(false);
        } finally {
            setReviewLoading(false);
        }
    };

    const revertAllSummaryText = (results: IRevertOutcome[]): { intent: "success" | "warning"; text: string } => {
        const reverted = results.filter((result) => result.outcome === "reverted").length;
        const skipped = results.filter((result) => result.outcome === "skipped").length;
        // Attempted but changed nothing, which is unexpected, so each one is reported with its reason
        const unchanged = results.filter((result) => result.outcome === "unchanged");
        const conflict = results.find((result) => result.outcome === "conflict");
        const parts = [t("pages.changes.revertAll.resultReverted", { count: reverted })];
        if (skipped > 0) {
            parts.push(t("pages.changes.revertAll.resultSkipped", { count: skipped }));
        }
        unchanged.forEach((result) =>
            parts.push(t("pages.changes.revertAll.resultUnchanged", { seq: result.seq, message: result.message })),
        );
        if (conflict) {
            parts.push(t("pages.changes.revertAll.resultConflict", { seq: conflict.seq, message: conflict.message }));
        }
        return { intent: conflict || unchanged.length > 0 ? "warning" : "success", text: parts.join(" ") };
    };

    const revertUnreviewed = async () => {
        setReviewLoading(true);
        try {
            const response = await requestRevertChanges(
                projectId,
                unreviewedEntries.map((entry) => entry.seq),
            );
            setRevertUnreviewedOpen(false);
            await loadChanges();
            setRevertAllSummary(revertAllSummaryText(response.data.results));
        } catch (ex) {
            displayRevertAllError(ex, t("pages.changes.errors.revertAll"));
        } finally {
            setReviewLoading(false);
        }
    };

    /** A value of a detail: the previous one marked as gone, the new one as current; an empty value spelled out. */
    const detailValue = (text: string, intent: "danger" | "success"): React.ReactNode =>
        text === "" ? (
            <NotAvailable label={t("pages.changes.emptyValue")} tooltip={t("pages.changes.emptyValueTooltip")} />
        ) : (
            <Tag small emphasis="weak" intent={intent}>
                {text}
            </Tag>
        );

    /** A detail as one line: the label, then before and after, one of them for an addition or removal, nothing when the label says it all. */
    const detailLine = (detail: IChangeDetail): React.ReactNode => {
        if (detail.before != null && detail.after != null) {
            return (
                <>
                    {detail.label}: {detailValue(detail.before, "danger")} → {detailValue(detail.after, "success")}
                </>
            );
        } else if (detail.after != null) {
            return (
                <>
                    {detail.label}: {detailValue(detail.after, "success")} {t("pages.changes.detailAdded")}
                </>
            );
        } else if (detail.before != null) {
            return (
                <>
                    {detail.label}: {detailValue(detail.before, "danger")} {t("pages.changes.detailRemoved")}
                </>
            );
        } else {
            return detail.label;
        }
    };

    const translateUnits = (unit: ElapsedDateTimeDisplayUnits) => t("common.units." + unit, unit);

    /** Relative within the last week, the date beyond, as the metadata panel shows it; the exact time on hover. */
    const timestamp = (isoDate: string): React.ReactNode => {
        const days = (Date.now() - new Date(isoDate).getTime()) / 1000 / 60 / 60 / 24;
        return days < 7 ? (
            <ElapsedDateTimeDisplay
                dateTime={isoDate}
                prefix={t("Metadata.prefixAgo")}
                suffix={t("Metadata.suffixAgo")}
                translateUnits={translateUnits}
            />
        ) : (
            <span title={new Date(isoDate).toLocaleString()}>{t("Metadata.dateFormat", getDateData(isoDate))}</span>
        );
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
            {revertAllSummary && (
                <>
                    <Notification data-test-id={"changes-revert-all-summary"} intent={revertAllSummary.intent}>
                        {revertAllSummary.text}
                    </Notification>
                    <Spacing size="small" />
                </>
            )}
            {unreviewedEntries.length > 0 && (
                <>
                    <Toolbar noWrap>
                        <ToolbarSection canGrow canShrink>
                            {t("pages.changes.unreviewedInfo", { count: unreviewedEntries.length })}
                        </ToolbarSection>
                        <ToolbarSection>
                            <Button
                                data-test-id={"changes-revert-unreviewed-btn"}
                                disruptive
                                text={t("pages.changes.revertAll.button")}
                                onClick={() => {
                                    setRevertAllError(undefined);
                                    setRevertUnreviewedOpen(true);
                                }}
                            />
                            <Spacing vertical size="small" />
                            <Button
                                data-test-id={"changes-mark-reviewed-btn"}
                                text={t("pages.changes.markReviewed.button")}
                                onClick={() => setMarkReviewedOpen(true)}
                            />
                        </ToolbarSection>
                    </Toolbar>
                    <Spacing size="small" />
                </>
            )}
            <TableContainer>
                <Table columnWidths={["50px", "14%", "18%", "56%", "130px"]}>
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
                            <TableRow
                                key={entry.seq}
                                className={entry.unreviewed ? "diapp-changes__row--unreviewed" : undefined}
                            >
                                <TableCell alignVertical="middle">{entry.seq}</TableCell>
                                <TableCell alignVertical="middle">{timestamp(entry.timestamp)}</TableCell>
                                <TableCell alignVertical="middle">
                                    {entry.user && <span title={entry.user}>{userDisplayName(entry.user)}</span>}
                                    {entry.origin && (
                                        <span
                                            data-test-id={`change-agent-${entry.seq}`}
                                            title={`${t("pages.changes.originTooltip")}: ${entry.origin}`}
                                        >
                                            {" "}
                                            <Icon name="operation-ai-generate" small />
                                        </span>
                                    )}
                                </TableCell>
                                <TableCell alignVertical="middle">
                                    <div title={entry.type}>{entry.summary}</div>
                                    {entry.details.map((detail, index) => (
                                        <div key={index} data-test-id={`change-detail-${entry.seq}-${index}`}>
                                            <small>{detailLine(detail)}</small>
                                        </div>
                                    ))}
                                    <Spacing size="tiny" />
                                    <div>
                                        <TagList>
                                            <Tag
                                                small
                                                intent={kindIntent[changeKind(entry.type)]}
                                                htmlTitle={entry.type}
                                            >
                                                {t(`pages.changes.kind.${changeKind(entry.type)}`)}
                                            </Tag>
                                            {entry.unreviewed && (
                                                <Tag
                                                    small
                                                    intent="warning"
                                                    htmlTitle={t("pages.changes.unreviewedTooltip")}
                                                >
                                                    {t("pages.changes.unreviewed")}
                                                </Tag>
                                            )}
                                            {entry.reverts != null && (
                                                <Tag small>{t("pages.changes.revertsTag", { seq: entry.reverts })}</Tag>
                                            )}
                                            {entry.revertedBy != null && (
                                                <Tag small>
                                                    {t("pages.changes.revertedByTag", {
                                                        seq: entry.revertedBy,
                                                    })}
                                                </Tag>
                                            )}
                                        </TagList>
                                    </div>
                                </TableCell>
                                <TableCell alignVertical="middle">
                                    {/* Every link opens in a new tab, so the review keeps its place */}
                                    {entry.links.map((link) => (
                                        <IconButton
                                            key={link.id}
                                            data-test-id={`change-link-${entry.seq}-${link.id}`}
                                            name={linkIcon(link.id)}
                                            small
                                            text={link.label}
                                            href={link.path}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                        />
                                    ))}
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
            {revertUnreviewedOpen && (
                <DeleteModal
                    data-test-id={"changes-revert-unreviewed-modal"}
                    isOpen={true}
                    title={t("pages.changes.revertAll.title")}
                    alternativeDeleteButtonText={t("common.action.revert")}
                    removeLoading={reviewLoading}
                    errorMessage={revertAllError?.detail}
                    onConfirm={revertUnreviewed}
                    onDiscard={() => setRevertUnreviewedOpen(false)}
                    render={() => (
                        <div>
                            <p>{t("pages.changes.revertAll.confirmText")}</p>
                            <ul>
                                {revertableUnreviewed.map((entry) => (
                                    <li key={entry.seq}>{entry.description}</li>
                                ))}
                            </ul>
                            {unreviewedEntries.length > revertableUnreviewed.length && (
                                <p>
                                    {t("pages.changes.revertAll.skippedNote", {
                                        count: unreviewedEntries.length - revertableUnreviewed.length,
                                    })}
                                </p>
                            )}
                        </div>
                    )}
                />
            )}
            {markReviewedOpen && (
                <SimpleDialog
                    data-test-id={"changes-mark-reviewed-modal"}
                    size="small"
                    title={t("pages.changes.markReviewed.title")}
                    isOpen={true}
                    onClose={() => setMarkReviewedOpen(false)}
                    actions={[
                        <Button
                            key="confirm"
                            affirmative
                            loading={reviewLoading}
                            onClick={markReviewed}
                            data-test-id={"changes-mark-reviewed-confirm-btn"}
                        >
                            {t("common.action.confirm")}
                        </Button>,
                        <Button key="cancel" onClick={() => setMarkReviewedOpen(false)} disabled={reviewLoading}>
                            {t("common.action.cancel")}
                        </Button>,
                    ]}
                >
                    <p>{t("pages.changes.markReviewed.confirmText")}</p>
                </SimpleDialog>
            )}
        </>
    );
};

export default ChangeList;
