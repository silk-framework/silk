import { projectApi } from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { IItemLink } from "@ducks/shared/typings";

/** One thing a change changed: what, and the value before and after where there is one to show. */
export interface IChangeDetail {
    /** What changed, e.g. the label of a parameter; without values the whole statement, e.g. "Password changed". */
    label: string;
    /** The value before the change; absent for an addition, or when there is no value to show. */
    before?: string;
    /** The value after the change; absent for a removal, or when there is no value to show. */
    after?: string;
}

/** A recorded change of a project, see ChangeJournalApi. */
export interface IChangeEntry {
    /** Sequence number of the change, ascending in the order the changes were made. */
    seq: number;
    /** When the change was made, as ISO-8601 timestamp. */
    timestamp: string;
    /** URI of the user who made the change. */
    user?: string;
    /** The client the change came from, e.g. 'mcp:<user agent>'. */
    origin?: string;
    /** The kind of change, e.g. 'AddMapping' or 'ReplaceTask'. */
    type: string;
    /** What has been changed, in one line: the summary with the details. */
    description: string;
    /** What has been changed, without the details. */
    summary: string;
    /** What the change changed in detail, where the summary does not tell: the parameters of a task update with their values. */
    details: IChangeDetail[];
    /** Links to what the change concerns, labelled by the server: the page of the task, as long as it exists, and for a workflow run its execution report. */
    links: IItemLink[];
    /** Whether the change can be reverted at all. */
    revertible: boolean;
    /** The change this one reverted, if it was made by reverting one. */
    reverts?: number;
    /** The change that reverted this one, if it has been reverted. */
    revertedBy?: number;
    /** True for an agent change after the reviewed watermark. */
    unreviewed?: boolean;
}

export interface IChangeList {
    /** The seq up to which the user has reviewed the changes; 0 if never set. */
    reviewedUpTo: number;
    changes: IChangeEntry[];
}

/** What happened to one change of a revert batch. */
export interface IRevertOutcome {
    seq: number;
    outcome: "reverted" | "skipped" | "unchanged" | "conflict" | "notAttempted";
    /** Why the change was skipped, left unchanged or conflicted. */
    message?: string;
    /** The change that records the revert, for outcome 'reverted'. */
    entry?: IChangeEntry;
}

/** The changes made to a project, newest first. */
export const requestProjectChanges = (projectId: string): Promise<FetchResponse<IChangeList>> =>
    fetch({ url: projectApi(`/${projectId}/changes`) });

/** Reverts a change. Answers with the change that records the revert; 409 on a conflict. */
export const requestRevertChange = (projectId: string, seq: number): Promise<FetchResponse<IChangeEntry>> =>
    fetch({ url: projectApi(`/${projectId}/changes/${seq}/revert`), method: "post" });

/** Reverts the given changes newest-first; skips what cannot be reverted and stops at the first conflict. */
export const requestRevertChanges = (
    projectId: string,
    seqs: number[],
): Promise<FetchResponse<{ results: IRevertOutcome[] }>> =>
    fetch({ url: projectApi(`/${projectId}/changes/revert`), method: "post", body: { seqs } });

/** Marks the changes up to the given seq as reviewed. */
export const requestMarkReviewed = (
    projectId: string,
    upTo: number,
): Promise<FetchResponse<{ reviewedUpTo: number }>> =>
    fetch({ url: projectApi(`/${projectId}/changes/reviewed`), method: "put", body: { upTo } });
