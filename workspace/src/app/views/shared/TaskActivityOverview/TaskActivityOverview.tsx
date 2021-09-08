import { Card, CardContent, CardHeader, CardTitle } from "@gui-elements/src/components/Card";
import { Divider, Spacing } from "@gui-elements/index";
import React, { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
    ActivityAction,
    DataIntegrationActivityControl,
} from "@gui-elements/src/components/dataIntegrationComponents/ActivityControl/DataIntegrationActivityControl";
import { IActivityListEntry } from "./taskActivityOverviewTypings";
import { activityActionCreator, fetchActivityInfos } from "./taskActivityOverviewRequests";
import { IActivityStatus } from "@gui-elements/src/components/dataIntegrationComponents/ActivityControl/ActivityControlTypes";
import Loading from "../Loading";
import { connectWebSocket } from "../../../services/websocketUtils";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { DIErrorTypes } from "@ducks/error/typings";
import useErrorHandler from "../../../hooks/useErrorHandler";

interface IProps {
    projectId: string;
    taskId: string;
}

interface IActivityControlFunctions {
    registerForUpdates: (callback: (status: IActivityStatus) => any) => any;
    unregisterFromUpdates: () => any;
    executeActivityAction: (action: ActivityAction) => void;
}

type StringOrUndefined = string | undefined;

export function TaskActivityOverview({ projectId, taskId }: IProps) {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [activities, setActivities] = useState<IActivityListEntry[]>([]);
    // Map from activity to current status
    const [activityStatusMap] = useState<Map<string, IActivityStatus>>(new Map());
    const [activityFunctionsMap] = useState<Map<string, IActivityControlFunctions>>(new Map());
    const [activityUpdateCallback] = useState<Map<string, (status: IActivityStatus) => any>>(new Map());
    const [loading, setLoading] = useState<boolean>(true);
    const setUpdateSwitch = useState<boolean>(false)[1];

    const triggerUpdate = () => {
        setUpdateSwitch((old) => !old);
    };

    const updateQuery = (
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

    const activityKey = (activity: string, projectId: StringOrUndefined, taskId: StringOrUndefined) =>
        `${activity}|${projectId ?? ""}|${taskId ?? ""}`;

    const activityKeyOfEntry = (entry: IActivityListEntry): string => {
        // Task activities do not have a metaData object, fill in with parameters from props.
        const metaData = entry.metaData || { projectId, taskId };
        return activityKey(entry.name, metaData?.projectId, metaData?.taskId);
    };

    const requestUpdates = (
        updateActivityStatus: (activityStatus: IActivityStatus) => any,
        project: StringOrUndefined,
        task: StringOrUndefined,
        activityName: StringOrUndefined
    ) => {
        const query = updateQuery(project, task, activityName);
        return connectWebSocket(
            legacyApiEndpoint(`/activities/updatesWebSocket${query}`),
            legacyApiEndpoint(`/activities/updates${query}`),
            updateActivityStatus
        );
    };

    const fetchTaskActivityInfos = async () => {
        setLoading(true);
        const updateActivityStatus = (activityStatus: IActivityStatus) => {
            const key = activityKey(activityStatus.activity, activityStatus.project, activityStatus.task);
            const oldValue = activityStatusMap.get(key);
            activityStatusMap.set(key, activityStatus);
            activityUpdateCallback.get(key)?.(activityStatus);
            if (!oldValue || oldValue.isRunning !== activityStatus.isRunning) {
                triggerUpdate();
            }
        };
        try {
            const activityRequest = fetchActivityInfos(projectId, taskId);
            const updateCleanUpFunction = requestUpdates(updateActivityStatus, projectId, taskId, undefined);
            const activityList = (await activityRequest).data;
            const additionCleanUpFunctions = activityList
                .filter((a) => a.metaData)
                .map((a) => {
                    // These are additional activities that we need to get updates for
                    return requestUpdates(updateActivityStatus, a.metaData?.projectId, a.metaData?.taskId, a.name);
                });
            setActivities(activityList);
            const cleanUpFunctions = await Promise.all([updateCleanUpFunction, ...additionCleanUpFunctions]);
            return () => {
                cleanUpFunctions.forEach((fn) => fn());
            };
        } catch {
            // TODO: Handle error
        } finally {
            setLoading(false);
        }
    };

    const translate = useCallback((key: string) => t("widget.TaskActivityOverview.activityControl." + key), [t]);

    const handleActivityActionError = (activityName: string, action: ActivityAction, error: DIErrorTypes) => {
        registerError(
            `taskActivityOverview-${activityName}-action`,
            t("widget.TaskActivityOverview.actions.errorMessages." + action, { activityName: activityName }),
            error
        );
    };

    // Register an observer from the activity widget
    const createRegisterForUpdatesFn = (activityKey: string) => (callback: (status: IActivityStatus) => any) => {
        activityUpdateCallback.set(activityKey, callback);
        // Send current value
        const currentStatus = activityStatusMap.get(activityKey);
        currentStatus && callback(currentStatus);
    };

    const createUnregisterFromUpdateFn = (activityKey: string) => () => {
        activityUpdateCallback.delete(activityKey);
    };

    // Creates and/or returns the memoized activity action function
    const activityFunctionsCreator = (activity: IActivityListEntry): IActivityControlFunctions => {
        const key = activityKeyOfEntry(activity);
        const metaData = activity.metaData || { projectId, taskId };
        if (activityFunctionsMap.has(key)) {
            return activityFunctionsMap.get(key) as IActivityControlFunctions;
        } else {
            const activityControlFunctions: IActivityControlFunctions = {
                executeActivityAction: activityActionCreator(
                    activity.name,
                    metaData.projectId,
                    metaData.taskId,
                    handleActivityActionError
                ),
                registerForUpdates: createRegisterForUpdatesFn(key),
                unregisterFromUpdates: createUnregisterFromUpdateFn(key),
            };
            activityFunctionsMap.set(key, activityControlFunctions);
            return activityControlFunctions;
        }
    };

    // Fetch activity infos and get updates
    useEffect(() => {
        fetchTaskActivityInfos();
    }, []);

    const activitiesWithStatus = activities.filter((a) => activityStatusMap.get(activityKeyOfEntry(a)));
    const mainActivities = activitiesWithStatus.filter((a) => a.activityCharacteristics.isMainActivity);
    const nonMainActivities = activitiesWithStatus.filter((a) => !a.activityCharacteristics.isMainActivity);
    const cacheActivities = nonMainActivities.filter((a) => a.activityCharacteristics.isCacheActivity);
    // TODO: Caches are duplicated when showing them as running and in caches section
    const runningNonMainActivities = nonMainActivities.filter(
        (a) => activityStatusMap.get(activityKeyOfEntry(a))?.isRunning && !a.activityCharacteristics.isCacheActivity
    );
    const failedNonMainActivities = nonMainActivities.filter(
        (a) => activityStatusMap.get(activityKeyOfEntry(a))?.failed
    );
    const nrActivitiesToShow =
        mainActivities.length +
        runningNonMainActivities.length +
        failedNonMainActivities.length +
        cacheActivities.length;

    const activityControl = (activity: IActivityListEntry): JSX.Element => {
        const activityFunctions = activityFunctionsCreator(activity);
        return (
            <>
                <DataIntegrationActivityControl
                    label={t(`widget.TaskActivityOverview.activities.${activity.name}.title`, activity.name)}
                    data-test-id={`activity-control-${projectId}-${taskId}-${activity.name}`}
                    executeActivityAction={activityFunctions.executeActivityAction}
                    registerForUpdates={activityFunctions.registerForUpdates}
                    unregisterFromUpdates={activityFunctions.unregisterFromUpdates}
                    translate={translate}
                    showFailureReportAction={true}
                    showProgress={true}
                    showReloadAction={activity.activityCharacteristics.isCacheActivity}
                    showStartAction={!activity.activityCharacteristics.isCacheActivity}
                    showStopAction={true}
                    key={activity.name}
                    showViewValueAction={activity.activityCharacteristics.isCacheActivity}
                />
                <Spacing size={"small"} />
            </>
        );
    };

    return nrActivitiesToShow > 0 ? (
        <Card data-test-id={"taskActivityOverview"}>
            <CardHeader>
                <CardTitle>
                    <h3>{t("widget.TaskActivityOverview.title")}</h3>
                </CardTitle>
            </CardHeader>
            <Divider />
            <CardContent>
                {loading ? (
                    <Loading />
                ) : (
                    <>
                        {mainActivities.map((a) => activityControl(a))}
                        {failedNonMainActivities.map((a) => activityControl(a))}
                        {runningNonMainActivities.map((a) => activityControl(a))}
                        {/* TODO: cache activity widget */}
                        {cacheActivities.map((a) => activityControl(a))}
                    </>
                )}
            </CardContent>
        </Card>
    ) : null;
}
