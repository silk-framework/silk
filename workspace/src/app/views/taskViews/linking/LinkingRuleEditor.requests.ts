import fetch from "../../../services/fetch";
import { legacyLinkingEndpoint } from "../../../utils/getApiEndpoint";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";

/** Fetches the cached paths from the linking paths cache.*/
export const fetchLinkingCachedPaths = (
    projectId: string,
    linkingTaskId: string,
    which: "source" | "target"
): Promise<FetchResponse<string[]>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/pathCacheValue/${which}`),
    });
};

const linkingRuleEditorRequests = {
    fetchLinkingCachedPaths,
};

export default linkingRuleEditorRequests;
