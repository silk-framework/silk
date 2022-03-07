import { workspaceApi } from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { ISearchListRequest, ISearchListResponse } from "@ducks/workspace/requests";

const searchActivities = (searchPayload: ISearchListRequest): Promise<FetchResponse<ISearchListResponse>> =>
    fetch({ url: workspaceApi("/searchActivities"), method: "post", body: searchPayload });

const utils = {
    searchActivities,
};

export default utils;
