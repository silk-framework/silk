import { projectApi } from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";

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
    /** What has been changed, for display. */
    description: string;
    /** Whether the change can be reverted at all. */
    revertible: boolean;
    /** The change this one reverted, if it was made by reverting one. */
    reverts?: number;
    /** The change that reverted this one, if it has been reverted. */
    revertedBy?: number;
}

export interface IChangeList {
    changes: IChangeEntry[];
}

/** The changes made to a project, newest first. */
export const requestProjectChanges = (projectId: string): Promise<FetchResponse<IChangeList>> =>
    fetch({ url: projectApi(`/${projectId}/changes`) });

/** Reverts a change. Answers with the change that records the revert; 409 on a conflict. */
export const requestRevertChange = (projectId: string, seq: number): Promise<FetchResponse<IChangeEntry>> =>
    fetch({ url: projectApi(`/${projectId}/changes/${seq}/revert`), method: "post" });
