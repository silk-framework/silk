import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { fetch } from "../../../services/fetch/fetch";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { IActivityListEntry } from "./taskActivityOverviewTypings";
import { DIErrorTypes } from "@ducks/error/typings";
import {
    ActivityAction,
    IActivityExecutionReport,
} from "@gui-elements/src/components/dataIntegrationComponents/ActivityControl/DataIntegrationActivityControl";

/** Fetch available activities for the workspace, project or task with optional infos, e.g. characteristics. */
export const fetchActivityInfos = async (
    projectId?: string,
    taskId?: string
): Promise<FetchResponse<IActivityListEntry[]>> => {
    return fetch({
        url: legacyApiEndpoint(`/activities/list`),
        query: {
            project: projectId,
            task: taskId,
            addDependentActivities: true,
        },
    });
};

/** Creates the activity action function that fires requests to the backend. */
export const activityActionCreator = (
    activityName: string,
    projectId: string | undefined,
    taskId: string | undefined,
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

/** Fetches the activity error report if available. */
export const fetchActivityErrorReport = async (
    activity: string,
    projectId?: string,
    taskId?: string,
    markdown: boolean = true
): Promise<FetchResponse<string | IActivityExecutionReport | undefined>> => {
    return fetch({
        url: legacyApiEndpoint(`/activities/errorReport`),
        headers: {
            accept: markdown ? "text/markdown" : "application/json",
        },
        query: {
            project: projectId,
            task: taskId,
            activity: activity,
        },
    });
};
