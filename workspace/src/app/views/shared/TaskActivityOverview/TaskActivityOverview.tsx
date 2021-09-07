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

    const fetchTaskActivityInfos = async () => {
        setLoading(true);
        const updateActivityStatus = (activityStatus: IActivityStatus) => {
            const oldValue = activityStatusMap.get(activityStatus.activity);
            activityStatusMap.set(activityStatus.activity, activityStatus);
            activityUpdateCallback.get(activityStatus.activity)?.(activityStatus);
            if (!oldValue || oldValue.isRunning !== activityStatus.isRunning) {
                triggerUpdate();
            }
        };
        try {
            const activityRequest = fetchActivityInfos(projectId, taskId);
            const updateCleanUpFunction = connectWebSocket(
                legacyApiEndpoint(`/activities/updatesWebSocket?project=${projectId}&task=${taskId}`),
                legacyApiEndpoint(`/activities/updates?project=${projectId}&task=${taskId}`),
                updateActivityStatus
            );
            const activityListResponse = await activityRequest;
            setActivities(activityListResponse.data);
            return await updateCleanUpFunction;
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
    const createRegisterForUpdatesFn = (activityName: string) => (callback: (status: IActivityStatus) => any) => {
        activityUpdateCallback.set(activityName, callback);
        // Send current value
        const currentStatus = activityStatusMap.get(activityName);
        currentStatus && callback(currentStatus);
    };

    const createUnregisterFromUpdateFn = (activityName: string) => () => {
        activityUpdateCallback.delete(activityName);
    };

    // Creates and/or returns the memoized activity action function
    const activityFunctionsCreator = (activityName: string): IActivityControlFunctions => {
        if (activityFunctionsMap.has(activityName)) {
            return activityFunctionsMap.get(activityName) as IActivityControlFunctions;
        } else {
            const activityControlFunctions: IActivityControlFunctions = {
                executeActivityAction: activityActionCreator(
                    projectId,
                    taskId,
                    activityName,
                    handleActivityActionError
                ),
                registerForUpdates: createRegisterForUpdatesFn(activityName),
                unregisterFromUpdates: createUnregisterFromUpdateFn(activityName),
            };
            activityFunctionsMap.set(activityName, activityControlFunctions);
            return activityControlFunctions;
        }
    };

    // Fetch activity infos and get updates
    useEffect(() => {
        fetchTaskActivityInfos();
    }, []);

    const activitiesWithStatus = activities.filter((a) => activityStatusMap.get(a.name));
    const mainActivities = activitiesWithStatus.filter((a) => a.activityCharacteristics.isMainActivity);
    const nonMainActivities = activitiesWithStatus.filter((a) => !a.activityCharacteristics.isMainActivity);
    const cacheActivities = nonMainActivities.filter((a) => a.activityCharacteristics.isCacheActivity);
    const runningNonMainActivities = nonMainActivities.filter((a) => activityStatusMap.get(a.name)?.isRunning);
    const failedNonMainActivities = nonMainActivities.filter((a) => activityStatusMap.get(a.name)?.failed);
    const nrActivitiesToShow =
        mainActivities.length +
        runningNonMainActivities.length +
        failedNonMainActivities.length +
        cacheActivities.length;

    const activityControl = (activity: IActivityListEntry): JSX.Element => {
        const activityFunctions = activityFunctionsCreator(activity.name);
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
                {/*<CardOptions>*/}
                {/*    <ContextMenu*/}
                {/*        data-test-id={"related-item-context-menu"}*/}
                {/*        togglerText={t("common.action.moreOptions", "Show more options")}*/}
                {/*    >*/}
                {/*        <MenuItem*/}
                {/*            text={t("widget.TaskActivityOverview.reloadAllCaches")}*/}
                {/*            icon={"item-reload"}*/}
                {/*            onClick={*/}
                {/*                (e) => {*/}
                {/*                    e.preventDefault();*/}
                {/*                    e.stopPropagation();*/}
                {/*                    alert("TODO") // TODO*/}
                {/*                }*/}
                {/*            }*/}
                {/*        />*/}
                {/*    </ContextMenu>*/}
                {/*</CardOptions>*/}
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
