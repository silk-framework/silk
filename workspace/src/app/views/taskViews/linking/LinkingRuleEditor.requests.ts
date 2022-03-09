import fetch from "../../../services/fetch";
import { legacyLinkingEndpoint } from "../../../utils/getApiEndpoint";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { PathWithMetaData } from "../shared/rules/rule.typings";

/** Fetches the cached paths from the linking paths cache.*/
export const fetchLinkingCachedPaths = (
    projectId: string,
    linkingTaskId: string,
    which: "source" | "target",
    withMetaData: boolean = true,
    langPref: string = "en"
): Promise<FetchResponse<string[] | PathWithMetaData[]>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/pathCacheValue/${which}`),
        query: {
            langPref,
            withMetaData,
        },
    });
};

const linkingRuleEditorRequests = {
    fetchLinkingCachedPaths,
};

export default linkingRuleEditorRequests;
