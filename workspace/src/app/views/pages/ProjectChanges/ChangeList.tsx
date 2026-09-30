import React from "react";
import { useTranslation } from "react-i18next";
import {
    Button,
    Notification,
    SimpleDialog,
    Spacing,
    Table,
    TableBody,
    TableContainer,
    TableHead,
    TableHeader,
    TableRow,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import { usePagination } from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import Loading from "../../shared/Loading";
import DeleteModal from "../../shared/modals/DeleteModal";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useModalError } from "../../../hooks/useModalError";
import { ErrorResponse } from "../../../services/fetch/responseInterceptor";
import {
    IChangeEntry,
    IRevertOutcome,
    requestMarkReviewed,
    requestProjectChanges,
    requestRevertChange,
    requestRevertConflicts,
} from "./changesRequests";
import { entriesBackTo, hasInverse } from "./changeListUtils";
import ChangeRow from "./ChangeRow";
import BatchRevertModal from "./BatchRevertModal";

interface IProps {
    /** Must not change while the list is mounted, as the list keeps the state of its project: key the list by it. */
    projectId: string;
    /** Increment to reload the list from the outside. */
    refreshKey?: number;
}

/** A batch revert awaiting confirmation: the entries in scope, of which the revertible ones are attempted. */
interface IBatchRevert {
    title: string;
    confirmText: string;
    entries: IChangeEntry[];
}

/** The changes of a project, newest first, with revert actions per entry and review actions for the agent changes. */
const ChangeList = ({ projectId, refreshKey = 0 }: IProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [entries, setEntries] = React.useState<IChangeEntry[]>([]);
    const [loading, setLoading] = React.useState<boolean>(true);
    const [revertEntry, setRevertEntry] = React.useState<IChangeEntry | undefined>(undefined);
    const [revertLoading, setRevertLoading] = React.useState<boolean>(false);
    const [revertError, setRevertError] = React.useState<ErrorResponse | undefined>(undefined);
    const [markReviewedOpen, setMarkReviewedOpen] = React.useState<boolean>(false);
    const [batchRevert, setBatchRevert] = React.useState<IBatchRevert | undefined>(undefined);
    // Why the entries of the shown page cannot be reverted now, by seq; asked per page, not for the whole journal
    const [conflicts, setConflicts] = React.useState<Map<number, string>>(new Map());
    const conflictsRequest = React.useRef(0);
    const [reviewLoading, setReviewLoading] = React.useState<boolean>(false);
    const [revertAllSummary, setRevertAllSummary] = React.useState<
        { intent: "success" | "warning"; text: string } | undefined
    >(undefined);
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

    /** The entries of the current page. */
    const pageOf = (all: IChangeEntry[]): IChangeEntry[] =>
        all.slice((pagination.current - 1) * pagination.limit, pagination.current * pagination.limit);

    // Asked again whenever the list or the page changes; an answer that a later request has overtaken is dropped
    React.useEffect(() => {
        const request = ++conflictsRequest.current;
        const seqs = pageOf(entries)
            .filter(hasInverse)
            .map((entry) => entry.seq);
        if (seqs.length === 0) {
            setConflicts(new Map());
            return;
        }
        (async () => {
            try {
                const answer = await requestRevertConflicts(projectId, seqs);
                if (request === conflictsRequest.current) {
                    setConflicts(new Map(answer.map((conflict) => [conflict.seq, conflict.reason])));
                }
            } catch (ex) {
                // Not knowing does not block a revert, which answers with the conflict itself, so no earlier answer stays either
                if (request === conflictsRequest.current) {
                    setConflicts(new Map());
                    registerError("ChangeList.loadConflicts", t("pages.changes.errors.fetchConflicts"), ex);
                }
            }
        })();
    }, [entries, pagination.current, pagination.limit]);

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
    const latestSeq = Math.max(...entries.map((entry) => entry.seq));

    const markReviewed = async () => {
        setReviewLoading(true);
        try {
            // The latest fetched seq, so that entries that arrived after the page rendered are never approved unseen.
            await requestMarkReviewed(projectId, latestSeq);
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

    /** Closes the batch dialog, reloads the list and reports the outcomes. */
    const batchReverted = async (results: IRevertOutcome[]) => {
        setBatchRevert(undefined);
        // The review actions stay busy until the list is current again
        setReviewLoading(true);
        try {
            await loadChanges();
        } finally {
            setReviewLoading(false);
        }
        setRevertAllSummary(revertAllSummaryText(results));
    };

    /** The row of an entry; the latest entry has nothing newer, so no state before it to return to. */
    const changeRow = (entry: IChangeEntry): React.ReactNode => {
        const backTo = entry.seq !== latestSeq ? entriesBackTo(entries, entry.seq) : undefined;
        return (
            <ChangeRow
                key={entry.seq}
                entry={entry}
                conflict={conflicts.get(entry.seq)}
                revertBackEntries={backTo}
                onRevert={() => openRevertDialog(entry)}
                onRevertBack={() =>
                    backTo &&
                    setBatchRevert({
                        title: t("pages.changes.revertBack.title", { seq: entry.seq }),
                        confirmText: t("pages.changes.revertBack.confirmText"),
                        entries: backTo,
                    })
                }
            />
        );
    };

    if (loading && !entries.length) {
        return <Loading description={t("pages.changes.loading")} />;
    }

    if (!entries.length) {
        return <Notification message={t("pages.changes.noChanges")} />;
    }

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
                                onClick={() =>
                                    setBatchRevert({
                                        title: t("pages.changes.revertAll.title"),
                                        confirmText: t("pages.changes.revertAll.confirmText"),
                                        entries: unreviewedEntries,
                                    })
                                }
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
                    <TableBody>{pageOf(entries).map(changeRow)}</TableBody>
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
            {batchRevert && (
                <BatchRevertModal
                    // Keyed by its entries, so that another batch gets a dialog and a check of its own
                    key={batchRevert.entries.map((entry) => entry.seq).join()}
                    projectId={projectId}
                    title={batchRevert.title}
                    confirmText={batchRevert.confirmText}
                    entries={batchRevert.entries}
                    onClose={() => setBatchRevert(undefined)}
                    onReverted={batchReverted}
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
