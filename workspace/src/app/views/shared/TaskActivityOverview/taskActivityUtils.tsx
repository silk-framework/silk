import { DIErrorTypes } from "@ducks/error/typings";
import { IActivityExecutionReport } from "@eccenca/gui-elements/src/cmem/ActivityControl/SilkActivityControl";

import { fetchActivityErrorReport } from "./taskActivityOverviewRequests";

/** Returns the error report function that should be given to the activity control as parameter. */
export const activityErrorReportFactory = (
    activityName: string,
    project: string | undefined,
    task: string | undefined,
    handleError: (error: DIErrorTypes) => IActivityExecutionReport | undefined | void
) => {
    return async (markdown: boolean) => {
        try {
            const response = await fetchActivityErrorReport(activityName, project, task, markdown);
            return response.data;
        } catch (ex) {
            handleError(ex);
        }
    };
};

type StringOrUndefined = string | undefined;

/** Generates the query string for activity related requests. */
export const activityQueryString = (
    projectId: StringOrUndefined,
    taskId: StringOrUndefined,
    activityName: StringOrUndefined
): string => {
    const start = projectId || taskId || activityName ? "?" : "";
    const project = projectId ? "project=" + projectId : undefined;
    const task = taskId ? "task=" + taskId : undefined;
    const activity = activityName ? "activity=" + activityName : undefined;
    const params = [project, task, activity].filter((param) => param).join("&");
    return start + params;
};
