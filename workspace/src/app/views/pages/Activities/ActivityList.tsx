import React from "react";
import { useTranslation } from "react-i18next";
import { ActivityAction, IActivityStatus, Markdown, SilkActivityControl } from "gui-elements/cmem";
import { Card } from "gui-elements";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import Datalist from "../../../views/shared/Datalist";
import AppliedFacets from "../Workspace/AppliedFacets";
import Pagination from "../../shared/Pagination";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { connectWebSocket } from "../../../services/websocketUtils";
import { activityActionCreator } from "../../../views/shared/TaskActivityOverview/taskActivityOverviewRequests";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { activityErrorReportFactory } from "../../../views/shared/TaskActivityOverview/taskActivityUtils";
import useErrorHandler from "../../../hooks/useErrorHandler";

interface IActivity extends ISearchResultsServer {
    isCacheActivity: boolean;
    parentType: string;
    project: string;
    task: string;
    taskLabel: string;
}

const ActivityList = () => {
    const dispatch = useDispatch();
    const pageSizes = [25, 50, 100];
    const { registerError } = useErrorHandler();

    const data = useSelector(workspaceSel.resultsSelector);
    const pagination = useSelector(workspaceSel.paginationSelector);
    // const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
    const isLoading = useSelector(workspaceSel.isLoadingSelector);
    // const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);

    // Stores the current status for each activity
    const [activityStatusMap] = React.useState<Map<string, IActivityStatus>>(new Map());

    // Contains the callback function from a specific activity control that needs to be called every time the status changes, so only the affected activity is re-rendered.
    const [activityUpdateCallback] = React.useState<Map<string, (status: IActivityStatus) => any>>(new Map());
    // Contains the memoized activity control execution functions for each activity

    const [t] = useTranslation();

    const isEmpty = !isLoading && !data.length;

    const translateActions = React.useCallback(
        (key: string) => t("widget.TaskActivityOverview.activityControl." + key),
        [t]
    );

    const updateActivityStatus = (status: IActivityStatus) => {
        activityStatusMap.set(status.activity, status);
        activityUpdateCallback.get(status.activity)?.(status);
    };

    const handlePaginationOnChange = (n: number, pageSize: number) => {
        dispatch(workspaceOp.changePageOp(n));
        dispatch(workspaceOp.changeLimitOp(pageSize));
    };

    // Register for activity status updates in backend
    const registerForUpdates = () => {
        return connectWebSocket(
            legacyApiEndpoint("/activities/updatesWebSocket"),
            legacyApiEndpoint("/activities/updates"),
            updateActivityStatus
        );
    };

    // Register an observer from the activity widget
    const createRegisterForUpdatesFn = (activityKey: string) => (callback: (status: IActivityStatus) => any) => {
        activityUpdateCallback.set(activityKey, callback);
        // Send current value if it exists
        const currentStatus = activityStatusMap.get(activityKey);
        currentStatus && callback(currentStatus);
    };

    // Unregister from updates when an activity control is not shown anymore
    const createUnregisterFromUpdateFn = (activityKey: string) => () => {
        activityUpdateCallback.delete(activityKey);
    };

    const executeAction = (activityName: string, action: ActivityAction, project, task) => {
        const mainAction = activityActionCreator(activityName, project, task, () => {});
        return mainAction(action);
    };

    // Returns a function that fetches the error report for a particular activity
    const fetchErrorReportFactory = (activity: IActivity) => {
        console.log("error report...", activity.project, activity.task);
        return activityErrorReportFactory(activity.id, activity.project, activity.task, (ex) => {
            registerError(
                `taskActivityOverview-fetchErrorReport`,
                t("widget.TaskActivityOverview.errorMessages.errorReport.fetchReport"),
                ex
            );
        });
    };

    // Query string for an activity related backend request
    const queryString = (activity: IActivity): string => {
        const projectParameter = activity.project ? `&project=${activity.project}` : "";
        const taskParameter = activity.task ? `&task=${activity.task}` : "";
        return `?activity=${activity.id}${projectParameter}${taskParameter}`;
    };

    React.useEffect(() => {
        return registerForUpdates();
    }, []);

    return (
        <>
            <AppliedFacets />
            <Datalist
                data-test-id="search-result-list"
                isEmpty={isEmpty}
                isLoading={isLoading}
                hasSpacing
                emptyContainer={<></>}
            >
                {data.map((activity: IActivity, index) => {
                    return (
                        <Card isOnlyLayout key={index}>
                            <SilkActivityControl
                                label={activity.label}
                                registerForUpdates={createRegisterForUpdatesFn(activity.id)}
                                unregisterFromUpdates={createUnregisterFromUpdateFn(activity.id)}
                                showReloadAction={activity.isCacheActivity}
                                showStartAction={!activity.isCacheActivity}
                                showStopAction
                                executeActivityAction={(action: ActivityAction) =>
                                    executeAction(activity.id, action, activity.project, activity.task)
                                }
                                failureReportAction={{
                                    title: "", // The title is already repeated in the markdown
                                    allowDownload: true,
                                    closeButtonValue: t("common.action.close"),
                                    downloadButtonValue: t("common.action.download"),
                                    renderMarkdown: true,
                                    renderReport: (markdown) => <Markdown children={markdown as string} />,
                                    fetchErrorReport: fetchErrorReportFactory(activity),
                                }}
                                viewValueAction={
                                    activity.isCacheActivity
                                        ? {
                                              tooltip: t("widget.TaskActivityOverview.activityControl.viewCachedData"),
                                              action: legacyApiEndpoint("/activities/value") + queryString(activity),
                                          }
                                        : undefined
                                }
                                translate={translateActions}
                            />
                        </Card>
                    );
                })}
            </Datalist>
            {!isEmpty ? (
                <Pagination pagination={pagination} pageSizes={pageSizes} onChangeSelect={handlePaginationOnChange} />
            ) : null}
        </>
    );
};

export default ActivityList;
