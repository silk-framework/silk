import React from "react";
import { useTranslation } from "react-i18next";
import { ActivityAction, IActivityStatus, SilkActivityControl } from "gui-elements/cmem";
import { Card } from "gui-elements";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import Datalist from "../../../views/shared/Datalist";
import AppliedFacets from "../Workspace/AppliedFacets";
import Pagination from "../../shared/Pagination";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { connectWebSocket } from "../../../services/websocketUtils";
import { activityActionCreator } from "../../../views/shared/TaskActivityOverview/taskActivityOverviewRequests";

const ActivityList = () => {
    const dispatch = useDispatch();
    const pageSizes = [10, 25, 50, 100];
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
                {data.map((activity: any, index) => {
                    return (
                        <Card isOnlyLayout key={index}>
                            <SilkActivityControl
                                label={activity.label}
                                registerForUpdates={createRegisterForUpdatesFn(activity.id)}
                                unregisterFromUpdates={createUnregisterFromUpdateFn(activity.id)}
                                showStartAction
                                showStopAction
                                showReloadAction
                                executeActivityAction={(action: ActivityAction) =>
                                    executeAction(activity.id, action, activity.project, activity.task)
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
