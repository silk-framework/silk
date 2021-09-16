import { Card, CardContent, CardHeader, CardTitle } from "@gui-elements/src/components/Card";
import {
    Divider,
    Icon,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Spinner,
    WhiteSpaceContainer,
} from "@gui-elements/index";
import React, { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
    ActivityAction,
    DataIntegrationActivityControl,
} from "@gui-elements/src/components/dataIntegrationComponents/ActivityControl/DataIntegrationActivityControl";
import {
    IActivityCachesOverallStatus,
    IActivityControlFunctions,
    IActivityListEntry,
} from "./taskActivityOverviewTypings";
import { activityActionCreator, fetchActivityErrorReport, fetchActivityInfos } from "./taskActivityOverviewRequests";
import { IActivityStatus } from "@gui-elements/src/components/dataIntegrationComponents/ActivityControl/ActivityControlTypes";
import Loading from "../Loading";
import { connectWebSocket } from "../../../services/websocketUtils";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { DIErrorTypes } from "@ducks/error/typings";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import ReactMarkdown from "react-markdown";
import ContentBlobToggler from "../ContentBlobToggler";
import { ElapsedDateTimeDisplay } from "@gui-elements/src/components/dataIntegrationComponents/DateTimeDisplay/ElapsedDateTimeDisplay";

interface IProps {
    projectId: string;
    taskId: string;
}

type StringOrUndefined = string | undefined;

export function TaskActivityOverview({ projectId, taskId }: IProps) {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [activities, setActivities] = useState<IActivityListEntry[]>([]);
    // So the update functions can access the current activities
    const [unmanagedState] = useState<{ activities: IActivityListEntry[] }>({ activities: [] });
    // Map from activity to current status
    const [activityStatusMap] = useState<Map<string, IActivityStatus>>(new Map());
    const [activityFunctionsMap] = useState<Map<string, IActivityControlFunctions>>(new Map());
    const [activityUpdateCallback] = useState<Map<string, (status: IActivityStatus) => any>>(new Map());
    const { isOpen } = useSelector(commonSel.artefactModalSelector);
    const [loading, setLoading] = useState<boolean>(true);
    const setUpdateSwitch = useState<boolean>(false)[1];
    const [cachesOverallStatus] = useState<IActivityCachesOverallStatus>({
        failedActivities: 0,
        oldestStartTime: undefined,
        currentlyExecuting: false,
    });

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

    const activityKey = (activity: string, projectId: StringOrUndefined, taskId: StringOrUndefined): string =>
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

    const updateOverallCacheState = (): boolean => {
        let triggerUpdate = false;
        const cacheActivityKeys = unmanagedState.activities
            .filter((a) => a.activityCharacteristics.isCacheActivity)
            .map((a) => activityKeyOfEntry(a));
        const cacheActivityStatus = cacheActivityKeys
            .map((activityKey) => activityStatusMap.get(activityKey))
            .filter((a) => a) as IActivityStatus[];
        // Update currently-executing state
        const executing = cacheActivityStatus.some((status) => status.isRunning);
        if (cachesOverallStatus.currentlyExecuting !== executing) {
            cachesOverallStatus.currentlyExecuting = executing;
            triggerUpdate = true;
        }
        // Update failed activities
        const failedActivities = cacheActivityStatus.filter((a) => a.failed).length;
        if (cachesOverallStatus.failedActivities !== failedActivities) {
            cachesOverallStatus.failedActivities = failedActivities;
            triggerUpdate = true;
        }
        // Update oldest start time
        const oldestStartTime = cacheActivityStatus
            .map((a) => a.startTime as string)
            .filter((a) => a)
            .sort((a, b) => (a < b ? -1 : 1))[0];
        if (cachesOverallStatus.oldestStartTime !== oldestStartTime) {
            cachesOverallStatus.oldestStartTime = oldestStartTime;
            triggerUpdate = true;
        }
        return triggerUpdate;
    };

    const fetchTaskActivityInfos = async () => {
        setLoading(true);
        const updateActivityStatus = (activityStatus: IActivityStatus) => {
            const key = activityKey(activityStatus.activity, activityStatus.project, activityStatus.task);
            const oldValue = activityStatusMap.get(key);
            activityStatusMap.set(key, activityStatus);
            activityUpdateCallback.get(key)?.(activityStatus);
            const shouldTriggerUpdate = updateOverallCacheState();
            if (!oldValue || oldValue.isRunning !== activityStatus.isRunning || shouldTriggerUpdate) {
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
            unmanagedState.activities = activityList;
            const cleanUpFunctions = await Promise.all([updateCleanUpFunction, ...additionCleanUpFunctions]);
            return () => {
                cleanUpFunctions.forEach((fn) => fn());
            };
        } catch (ex) {
            registerError(
                `taskActivityOverview-fetchTaskActivityInfos`,
                t("widget.TaskActivityOverview.errorMessages.errorReport.fetchReport"),
                ex
            );
        } finally {
            setLoading(false);
        }
    };

    // Returns a function that fetches the error report for a particular activity
    const fetchErrorReportFactory = (activity: IActivityListEntry) => {
        const project = activity.metaData ? activity.metaData.projectId : projectId;
        const task = activity.metaData ? activity.metaData.taskId : taskId;
        return async (markdown: boolean) => {
            try {
                const response = await fetchActivityErrorReport(activity.name, project, task, markdown);
                return response.data;
            } catch (ex) {
                registerError(
                    `taskActivityOverview-fetchErrorReport`,
                    t("widget.TaskActivityOverview.errorMessages.errorReport.fetchReport"),
                    ex
                );
            }
        };
    };

    const translate = useCallback((key: string) => t("widget.TaskActivityOverview.activityControl." + key), [t]);

    const handleActivityActionError = (activityName: string, action: ActivityAction, error: DIErrorTypes) => {
        registerError(
            `taskActivityOverview-${activityName}-action`,
            t("widget.TaskActivityOverview.errorMessages.actions." + action, { activityName: activityName }),
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
        if (!isOpen) {
            fetchTaskActivityInfos();
        }
    }, [isOpen]);

    const activitiesWithStatus = activities.filter((a) => activityStatusMap.get(activityKeyOfEntry(a)));
    const mainActivities = activitiesWithStatus.filter((a) => a.activityCharacteristics.isMainActivity);
    const nonMainActivities = activitiesWithStatus.filter((a) => !a.activityCharacteristics.isMainActivity);
    const cacheActivities = nonMainActivities.filter((a) => a.activityCharacteristics.isCacheActivity);
    const runningNonMainActivities = nonMainActivities.filter(
        (a) => activityStatusMap.get(activityKeyOfEntry(a))?.isRunning && !a.activityCharacteristics.isCacheActivity
    );
    const failedNonMainActivities = nonMainActivities.filter(
        (a) => activityStatusMap.get(activityKeyOfEntry(a))?.failed && !a.activityCharacteristics.isCacheActivity
    );
    const nrActivitiesToShow =
        mainActivities.length +
        runningNonMainActivities.length +
        failedNonMainActivities.length +
        cacheActivities.length;

    const queryString = (activity: IActivityListEntry): string => {
        const project = activity.metaData ? activity.metaData.projectId : projectId;
        const task = activity.metaData ? activity.metaData.taskId : taskId;
        const projectParameter = project ? `&project=${project}` : "";
        const taskParameter = task ? `&task=${task}` : "";
        return `?activity=${activity.name}${projectParameter}${taskParameter}`;
    };

    const activityControl = (activity: IActivityListEntry): JSX.Element => {
        const activityFunctions = activityFunctionsCreator(activity);
        const activityLabel = t(`widget.TaskActivityOverview.activities.${activity.name}.title`, activity.name);
        const elapsedTime = activity.activityCharacteristics.isCacheActivity
            ? {
                  prefix: ` (${t("widget.TaskActivityOverview.cacheGroup.cacheAgePrefixIndividual")}`,
                  suffix: ")",
              }
            : undefined;
        return (
            <span key={activity.name}>
                <DataIntegrationActivityControl
                    label={activityLabel}
                    data-test-id={`activity-control-${projectId}-${taskId}-${activity.name}`}
                    executeActivityAction={activityFunctions.executeActivityAction}
                    registerForUpdates={activityFunctions.registerForUpdates}
                    unregisterFromUpdates={activityFunctions.unregisterFromUpdates}
                    translate={translate}
                    elapsedTimeOfLastStart={elapsedTime}
                    failureReportAction={{
                        title: "", // The title is already repeated in the markdown
                        allowDownload: true,
                        closeButtonValue: t("common.action.close"),
                        downloadButtonValue: t("common.action.download"),
                        renderMarkdown: true,
                        renderReport: (markdown) => <ReactMarkdown source={markdown as string} />,
                        fetchErrorReport: fetchErrorReportFactory(activity),
                    }}
                    showProgress={true}
                    showReloadAction={activity.activityCharacteristics.isCacheActivity}
                    showStartAction={!activity.activityCharacteristics.isCacheActivity}
                    showStopAction={true}
                    viewValueAction={
                        activity.activityCharacteristics.isCacheActivity
                            ? {
                                  tooltip: t("widget.TaskActivityOverview.activityControl.viewCachedData"),
                                  action: legacyApiEndpoint("/activities/value") + queryString(activity),
                              }
                            : undefined
                    }
                />
                <Spacing size={"small"} />
            </span>
        );
    };

    const cacheWidget = () => {
        return (
            <Card>
                <OverviewItem>
                    <OverviewItemDescription>
                        <OverviewItemLine>
                            {t("widget.TaskActivityOverview.cacheGroup.title", "Caches")}
                        </OverviewItemLine>
                        <OverviewItemLine>
                            {cachesOverallStatus.oldestStartTime ? (
                                <>
                                    <ElapsedDateTimeDisplay
                                        prefix={t("widget.TaskActivityOverview.cacheGroup.cacheAgePrefix")}
                                        dateTime={cachesOverallStatus.oldestStartTime}
                                    />
                                    <Spacing vertical={true} />
                                </>
                            ) : (
                                ""
                            )}
                            {cachesOverallStatus.failedActivities ? (
                                <>
                                    <Icon
                                        name={"state-warning"}
                                        tooltipText={`${cachesOverallStatus.failedActivities} failed activities`}
                                    />
                                    <Spacing vertical={true} />
                                </>
                            ) : (
                                ""
                            )}
                            {cachesOverallStatus.currentlyExecuting ? (
                                <Spinner position={"inline"} size={"tiny"} />
                            ) : (
                                ""
                            )}
                        </OverviewItemLine>
                    </OverviewItemDescription>
                </OverviewItem>
            </Card>
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
                        {cacheActivities.length ? (
                            <ContentBlobToggler
                                textToggleReduce={"Hide"}
                                textToggleExtend={"Open"}
                                contentPreview={cacheWidget()}
                                contentFullview={
                                    <>
                                        {cacheWidget()}
                                        <WhiteSpaceContainer marginBottom="small" marginLeft="regular">
                                            <Spacing size={"small"} />
                                            {cacheActivities.map((a) => activityControl(a))}
                                        </WhiteSpaceContainer>
                                    </>
                                }
                            />
                        ) : null}
                        {failedNonMainActivities.map((a) => activityControl(a))}
                        {runningNonMainActivities.map((a) => activityControl(a))}
                    </>
                )}
            </CardContent>
        </Card>
    ) : null;
}
