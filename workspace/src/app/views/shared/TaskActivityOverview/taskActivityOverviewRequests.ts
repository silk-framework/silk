import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { fetch } from "../../../services/fetch/fetch";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { IActivityListEntry } from "./taskActivityOverviewTypings";
import { DIErrorTypes } from "@ducks/error/typings";
import { ActivityAction } from "@gui-elements/src/components/dataIntegrationComponents/ActivityControl/DataIntegrationActivityControl";

/** Fetch available activities for the workspace, project or task with optional infos, e.g. characteristics. */
export const fetchActivityInfos = async (
    projectId?: string,
    taskId?: string
): Promise<FetchResponse<IActivityListEntry[]>> => {
    return fetch({
        url: legacyApiEndpoint(`/activities/list`),
        body: {
            project: projectId,
            task: taskId,
        },
    });
};

/** Creates the activity action function that fires requests to the backend. */
export const activityActionCreator = (
    projectId: string,
    taskId: string,
    activityName: string,
    handleError: (activityName: string, action: ActivityAction, error: DIErrorTypes) => any
) => {
    return async (action: ActivityAction) => {
        try {
            await fetch({
                url: legacyApiEndpoint("/activities/" + action),
                method: "POST",
                query: {
                    project: projectId,
                    task: taskId,
                    activity: activityName,
                },
            });
        } catch (ex) {
            handleError(activityName, action, ex);
        }
    };
};
