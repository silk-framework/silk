import { workspaceApi } from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { ISearchListRequest, ISearchListResponse } from "@ducks/workspace/requests";
import { IMetadata } from "@ducks/shared/typings";

const searchActivities = (searchPayload: ISearchListRequest): Promise<FetchResponse<ISearchListResponse>> =>
    fetch({ url: workspaceApi("/searchActivities"), method: "post", body: searchPayload });

const getProjectInfo = (projectId: string): Promise<FetchResponse<IMetadata>> =>
    fetch({ url: workspaceApi(`/projects/${projectId}/metaData`) });

const utils = {
    searchActivities,
    getProjectInfo,
};

export default utils;
