import React from "react";
import { useTranslation } from "react-i18next";
import { batch, useDispatch, useSelector } from "react-redux";
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
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { ISearchListRequest, ISearchListResponse } from "@ducks/workspace/requests";
import { AppDispatch } from "store/configureStore";
import { GlobalTableContext } from "../../../GlobalContextsWrapper";
import Loading from "../../shared/Loading";
import Pagination from "../../shared/Pagination";
import DeleteModal from "../../shared/modals/DeleteModal";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useModalError } from "../../../hooks/useModalError";
import { ErrorResponse, FetchResponse } from "../../../services/fetch/responseInterceptor";
import {
    IChangeEntry,
    IRevertOutcome,
    requestMarkReviewed,
    requestProjectChanges,
    requestRevertChange,
    requestRevertConflicts,
    searchChanges,
    STATE_FACET,
    UNREVIEWED_STATE,
} from "./changesRequests";
import { entriesBackTo, hasInverse } from "./changeListUtils";
import ChangeRow from "./ChangeRow";
import BatchRevertModal from "./BatchRevertModal";

interface IProps {
    /** Must not change while the list is mounted, as the list keeps the state of its project: key the list by it. */
    projectId: string;
    /** Whether the page has set the shared search store up for this project; the list fetches only then. */
    ready?: boolean;
    /** Increment to reload the list from the outside. */
    refreshKey?: number;
}

/** A batch revert awaiting confirmation: the entries in scope, of which the revertible ones are attempted. */
interface IBatchRevert {
    title: string;
    confirmText: string;
    entries: IChangeEntry[];
}

/** The journal's numbers that come with every page: the latest seq, for marking reviewed, and the unreviewed count. */
interface IJournalNumbers {
    latestSeq: number;
    unreviewed: number;
}

const PAGE_SIZES = [25, 50, 100];

/** The sort properties of the change search; a property persisted on another page is not sent. */
const CHANGE_SORT_PROPERTIES = ["time", "user", "task", "kind"];

/**
 * The changes of a project that match the search store's query, facets and sort, one page at a time, with revert actions
 * per entry and review actions for the agent changes.
 */
const ChangeList = ({ projectId, ready = true, refreshKey = 0 }: IProps) => {
    const [t] = useTranslation();
    const dispatch = useDispatch<AppDispatch>();
    const { registerError } = useErrorHandler();
    const entries = useSelector(workspaceSel.resultsSelector) as unknown as IChangeEntry[];
    const isLoading = useSelector(workspaceSel.isLoadingSelector);
    const error = useSelector(workspaceSel.errorSelector);
    const pagination = useSelector(workspaceSel.paginationSelector);
    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);
    const facets = useSelector(workspaceSel.facetsSelector);
    const { globalTableSettings } = React.useContext(GlobalTableContext);
    // Settings stored before this page existed lack its key
    const tableSettings = globalTableSettings["changes"] ?? {};
    const sortBy =
        tableSettings.sortBy && CHANGE_SORT_PROPERTIES.includes(tableSettings.sortBy)
            ? tableSettings.sortBy
            : undefined;
    const { sortOrder, pageSize } = tableSettings;
    const [journal, setJournal] = React.useState<IJournalNumbers>({ latestSeq: 0, unreviewed: 0 });
    const [revertEntry, setRevertEntry] = React.useState<IChangeEntry | undefined>(undefined);
    const [revertLoading, setRevertLoading] = React.useState<boolean>(false);
    const [revertError, setRevertError] = React.useState<ErrorResponse | undefined>(undefined);
    const [markReviewedOpen, setMarkReviewedOpen] = React.useState<boolean>(false);
    const [batchRevert, setBatchRevert] = React.useState<IBatchRevert | undefined>(undefined);
    // Why the entries of the shown page cannot be reverted now, by seq; asked per page
    const [conflicts, setConflicts] = React.useState<Map<number, string>>(new Map());
    const conflictsRequest = React.useRef(0);
    const [reviewLoading, setReviewLoading] = React.useState<boolean>(false);
    const [revertAllSummary, setRevertAllSummary] = React.useState<
        { intent: "success" | "warning"; text: string } | undefined
    >(undefined);
    const displayRevertError = useModalError({ setError: setRevertError });

    /** Answers the store's search with the journal's page, keeping the numbers that come with it. */
    const fetcher = React.useCallback(
        async (payload: ISearchListRequest): Promise<FetchResponse<ISearchListResponse>> => {
            try {
                const response = await searchChanges(projectId, payload);
                setJournal({ latestSeq: response.data.latestSeq, unreviewed: response.data.unreviewed });
                // The entries take the place of the search items in the store
                return response as unknown as FetchResponse<ISearchListResponse>;
            } catch (ex) {
                registerError("ChangeList.loadChanges", t("pages.changes.errors.fetchChanges"), ex);
                // The store shows the detail of an error, which a fetch error holds in its response
                throw ex?.errorResponse ?? ex;
            }
        },
        [projectId],
    );

    const loadChanges = () => dispatch(workspaceOp.fetchListAsync({ sortBy, sortOrder, pageSize }, fetcher));

    // Fetched again whenever the query, a facet, the sort or the page changes
    React.useEffect(() => {
        if (ready) {
            loadChanges();
        }
    }, [
        ready,
        fetcher,
        pagination.current,
        pagination.limit,
        appliedFilters,
        appliedFacets,
        sortBy,
        sortOrder,
        pageSize,
        refreshKey,
    ]);

    // Asked again whenever the page changes; an answer that a later request has overtaken is dropped
    React.useEffect(() => {
        const request = ++conflictsRequest.current;
        const seqs = entries.filter(hasInverse).map((entry) => entry.seq);
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
    }, [entries]);

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

    /** The filters are on: what the page shows is a selection, not the journal. */
    const filtered = !!appliedFilters.textQuery || appliedFacets.length > 0;

    /**
     * The unreviewed entries the current query and facets match, as the state facet counts them. The facet counts its
     * own values as if it were not applied, so a state facet applied without the unreviewed value hides them all.
     */
    const stateFacet = appliedFacets.find((facet) => facet.facetId === STATE_FACET);
    const shownUnreviewed =
        stateFacet && !stateFacet.keywordIds.includes(UNREVIEWED_STATE)
            ? 0
            : (facets.find((facet) => facet.id === STATE_FACET)?.values.find((value) => value.id === UNREVIEWED_STATE)
                  ?.count ?? 0);
    /** The unreviewed entries a filter hides: marking all as reviewed would approve them unseen. */
    const hiddenUnreviewed = filtered ? Math.max(0, journal.unreviewed - shownUnreviewed) : 0;

    const markReviewed = async () => {
        setReviewLoading(true);
        try {
            // The latest fetched seq, so that entries that arrived after the page rendered are never approved unseen.
            await requestMarkReviewed(projectId, journal.latestSeq);
            setMarkReviewedOpen(false);
            await loadChanges();
        } catch (ex) {
            registerError("ChangeList.markReviewed", t("pages.changes.errors.markReviewed"), ex);
            setMarkReviewedOpen(false);
        } finally {
            setReviewLoading(false);
        }
    };

    /** The unreviewed entries the current query and facets match, all of them, newest first. */
    const unreviewedEntries = async (): Promise<IChangeEntry[]> => {
        const stateFacet = { facetId: STATE_FACET, type: "keyword", keywordIds: [UNREVIEWED_STATE] };
        const response = await searchChanges(projectId, {
            textQuery: appliedFilters.textQuery,
            facets: [...appliedFacets.filter((facet) => facet.facetId !== STATE_FACET), stateFacet],
            limit: 0,
        });
        return response.data.results;
    };

    const openRevertUnreviewed = async () => {
        try {
            setBatchRevert({
                title: t("pages.changes.revertAll.title"),
                confirmText: t("pages.changes.revertAll.confirmText"),
                entries: await unreviewedEntries(),
            });
        } catch (ex) {
            registerError("ChangeList.loadUnreviewed", t("pages.changes.errors.fetchChanges"), ex);
        }
    };

    /** Back to before an entry: the entries newer than it, which the page may not hold, come from the whole journal. */
    const openRevertBack = async (entry: IChangeEntry) => {
        try {
            const all = (await requestProjectChanges(projectId)).data.changes;
            setBatchRevert({
                title: t("pages.changes.revertBack.title", { seq: entry.seq }),
                confirmText: t("pages.changes.revertBack.confirmText"),
                entries: entriesBackTo(all, entry.seq),
            });
        } catch (ex) {
            registerError("ChangeList.loadNewer", t("pages.changes.errors.fetchChanges"), ex);
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

    const handlePaginationOnChange = (page: number, pageSize: number) => {
        batch(() => {
            dispatch(workspaceOp.changeLimitOp(pageSize));
            dispatch(workspaceOp.changePageOp(page));
        });
    };

    /** The row of an entry; the latest entry has nothing newer, so no state before it to return to. */
    const changeRow = (entry: IChangeEntry): React.ReactNode => (
        <ChangeRow
            key={entry.seq}
            entry={entry}
            conflict={conflicts.get(entry.seq)}
            searchValue={appliedFilters.textQuery}
            onRevert={() => openRevertDialog(entry)}
            onRevertBack={entry.seq !== journal.latestSeq ? () => openRevertBack(entry) : undefined}
        />
    );

    if (error?.detail) {
        return <Notification intent="danger" message={error.detail} />;
    }
    if (isLoading && !entries.length) {
        return <Loading description={t("pages.changes.loading")} />;
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
            {journal.unreviewed > 0 && (
                <>
                    <Toolbar noWrap>
                        <ToolbarSection canGrow canShrink>
                            {t("pages.changes.unreviewedInfo", { count: journal.unreviewed })}
                        </ToolbarSection>
                        <ToolbarSection>
                            <Button
                                data-test-id={"changes-revert-unreviewed-btn"}
                                disruptive
                                text={t("pages.changes.revertAll.button")}
                                onClick={openRevertUnreviewed}
                            />
                            <Spacing vertical size="small" />
                            <Button
                                data-test-id={"changes-mark-reviewed-btn"}
                                text={t("pages.changes.markReviewed.button")}
                                disabled={hiddenUnreviewed > 0}
                                tooltip={
                                    hiddenUnreviewed > 0
                                        ? t("pages.changes.markReviewed.hidden", { count: hiddenUnreviewed })
                                        : undefined
                                }
                                onClick={() => setMarkReviewedOpen(true)}
                            />
                        </ToolbarSection>
                    </Toolbar>
                    <Spacing size="small" />
                </>
            )}
            {!entries.length ? (
                <Notification message={t(filtered ? "pages.changes.noMatch" : "pages.changes.noChanges")} />
            ) : (
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
                        <TableBody>{entries.map(changeRow)}</TableBody>
                    </Table>
                </TableContainer>
            )}
            {pagination.total > Math.min(...PAGE_SIZES) && (
                <>
                    <Spacing size="small" />
                    <Pagination
                        pagination={pagination}
                        pageSizes={PAGE_SIZES}
                        onChangeSelect={handlePaginationOnChange}
                    />
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
