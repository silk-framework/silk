import React, { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Divider,
    Icon,
    IconButton,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Spinner,
    WhiteSpaceContainer,
} from "@eccenca/gui-elements";
import { Definitions as IntentTypes } from "@eccenca/gui-elements/src/common/Intent";
import {
    ActivityAction,
    IActivityControlLayoutProps,
    SilkActivityControl,
    IActivityStatus,
    Markdown,
    ElapsedDateTimeDisplay,
    TimeUnits,
} from "@eccenca/gui-elements";
import {
    IActivityCachesOverallStatus,
    IActivityControlFunctions,
    IActivityListEntry,
} from "./taskActivityOverviewTypings";
import { activityActionCreator, activityStartPrioritized, fetchActivityInfos } from "./taskActivityOverviewRequests";
import Loading from "../Loading";
import { connectWebSocket } from "../../../services/websocketUtils";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { DIErrorTypes } from "@ducks/error/typings";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { activityErrorReportFactory, activityQueryString } from "./taskActivityUtils";

interface IProps {
    projectId: string;
    taskId: string;
}

type StringOrUndefined = string | undefined;

/** Displays some activities of a task, usually the main activity, cache activities and other failed or running activities. */
export function TaskActivityOverview({ projectId, taskId }: IProps) {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [activities, setActivities] = useState<IActivityListEntry[]>([]);
    /** Setup some values whose update should not trigger re-renders. Re-renders are explicitly triggered only when necessary via triggerUpdate().*/
    // Basically a clone of activities, so the update functions can access the current activities
    const [unmanagedState] = useState<{ activities: IActivityListEntry[] }>({ activities: [] });
    // Stores the current status for each activity
    const [activityStatusMap] = useState<Map<string, IActivityStatus>>(new Map());
    // Contains the memoized activity control execution functions for each activity
    const [activityFunctionsMap] = useState<Map<string, IActivityControlFunctions>>(new Map());
    // Contains the callback function from a specific activity control that needs to be called every time the status changes, so only the affected activity is re-rendered.
    const [activityUpdateCallback] = useState<Map<string, (status: IActivityStatus) => any>>(new Map());
    // The aggregated status of all cache activities
    const [cachesOverallStatus] = useState<IActivityCachesOverallStatus>({
        failedActivities: 0,
        oldestStartTime: undefined,
        currentlyExecuting: false,
    });
    // Need to re-render task activities when a potential configuration change has occurred, e.g. different input data source.
    const { isOpen } = useSelector(commonSel.artefactModalSelector);
    const [loading, setLoading] = useState<boolean>(true);
    const [displayCacheList, setDisplayCacheList] = useState<boolean>(false);

    // Used for explicit re-render trigger
    const setUpdateSwitch = useState<boolean>(false)[1];
    const triggerUpdate = () => {
        setUpdateSwitch((old) => !old);
    };

    // Used for keys in activity->value maps
    const activityKey = (activity: string, projectId: StringOrUndefined, taskId: StringOrUndefined): string =>
        `${activity}|${projectId ?? ""}|${taskId ?? ""}`;

    const activityKeyOfEntry = (entry: IActivityListEntry): string => {
        // Task activities do not have a metaData object, fill in with parameters from props.
        const metaData = entry.metaData || { projectId, taskId };
        return activityKey(entry.name, metaData?.projectId, metaData?.taskId);
    };

    // Request activity updates via websocket or polling. If a task is depending on other activities than its own, this is executed multiple times.
    const requestUpdates = (
        updateActivityStatus: (activityStatus: IActivityStatus) => any,
        project: StringOrUndefined,
        task: StringOrUndefined,
        activityName: StringOrUndefined
    ) => {
        const query = activityQueryString(project, task, activityName);
        return connectWebSocket(
            legacyApiEndpoint(`/activities/updatesWebSocket${query}`),
            legacyApiEndpoint(`/activities/updates${query}`),
            updateActivityStatus
        );
    };

    // Updates the overall cache state corresponding to the current activity states
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

    // Fetch list of activities for the task
    const fetchActivityList = async () => {
        setLoading(true);
        try {
            const activityRequest = fetchActivityInfos(projectId, taskId);
            const activityList = (await activityRequest).data;
            setActivities(activityList);
            unmanagedState.activities = activityList;
        } catch (ex) {
            registerError(
                `taskActivityOverview-fetchTaskActivityInfos`,
                t("widget.TaskActivityOverview.errorMessages.fetchActivityInfo"),
                ex
            );
            setLoading(false);
        }
    };

    // Request all relevant activity updates
    const requestActivityUpdates = () => {
        setLoading(true);
        // Callback function when an update comes in
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
            // For task activities
            const updateCleanUpFunction = requestUpdates(updateActivityStatus, projectId, taskId, undefined);
            // For additional activities the task depends on
            const additionCleanUpFunctions = activities
                .filter((a) => a.metaData)
                .map((a) => {
                    // These are additional activities that we need to get updates for
                    return requestUpdates(updateActivityStatus, a.metaData?.projectId, a.metaData?.taskId, a.name);
                });
            const cleanUpFunctions = [updateCleanUpFunction, ...additionCleanUpFunctions];
            return () => {
                cleanUpFunctions.forEach((fn) => fn());
            };
        } catch (ex) {
            registerError(
                `taskActivityOverview-fetchTaskActivityInfos`,
                t("widget.TaskActivityOverview.errorMessages.fetchActivityInfo"),
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
        return activityErrorReportFactory(activity.name, project, task, (ex) => {
            registerError(
                `taskActivityOverview-fetchErrorReport`,
                t("widget.TaskActivityOverview.errorMessages.errorReport.fetchReport"),
                ex
            );
        });
    };

    const translateActions = useCallback((key: string) => t("widget.TaskActivityOverview.activityControl." + key), [t]);

    const handleActivityActionError = (activityName: string, action: ActivityAction, error: DIErrorTypes) => {
        registerError(
            `taskActivityOverview-${activityName}-${action}`,
            t("widget.TaskActivityOverview.errorMessages.actions." + action, { activityName: activityName }),
            error
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

    // Fetch activity list
    useEffect(() => {
        if (!isOpen) {
            setActivities([]);
            unmanagedState.activities = [];
            fetchActivityList();
        }
    }, [isOpen, projectId, taskId]);

    // Request updates for activities
    useEffect(() => {
        if (activities.length > 0) {
            cachesOverallStatus.oldestStartTime = undefined;
            activityStatusMap.clear();
            activityFunctionsMap.clear();
            activityUpdateCallback.clear();
            return requestActivityUpdates();
        }
    }, [activities]);

    // Categorize activities
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
    // to decide if the widget should be shown at all
    const nrActivitiesToShow =
        mainActivities.length +
        runningNonMainActivities.length +
        failedNonMainActivities.length +
        cacheActivities.length;

    // Query string for an activity related backend request
    const queryString = (activity: IActivityListEntry): string => {
        const project = activity.metaData ? activity.metaData.projectId : projectId;
        const task = activity.metaData ? activity.metaData.taskId : taskId;
        const projectParameter = project ? `&project=${project}` : "";
        const taskParameter = task ? `&task=${task}` : "";
        return `?activity=${activity.name}${projectParameter}${taskParameter}`;
    };

    // For the elapsed time component, showing when a cache was last updated
    const translateUnits = (unit: TimeUnits) => t("common.units." + unit, unit);
    const startPrioritized = React.useCallback(async (activityName: string, project: string, task: string) => {
        const registerActivityError = (activityName: string, error: DIErrorTypes) => {
            registerError(
                `TaskActivityOverview.startPrioritized`,
                `Activity action 'Run prioritized' against activity '${activityName}' has failed, see details.`,
                error
            );
        };
        await activityStartPrioritized(activityName, project, task, registerActivityError);
    }, []);

    // A single activity control
    const activityControl = (activity: IActivityListEntry, layoutConfig: IActivityControlLayoutProps): JSX.Element => {
        const activityFunctions = activityFunctionsCreator(activity);
        const activityLabel = t(`widget.TaskActivityOverview.activities.${activity.name}.title`, activity.label);
        const elapsedTime = activity.activityCharacteristics.isCacheActivity
            ? {
                  prefix: ` (${t("widget.TaskActivityOverview.cacheGroup.cacheAgePrefixIndividual")}`,
                  suffix: `${t("widget.TaskActivityOverview.cacheGroup.cacheAgeSuffix")})`,
                  translate: translateUnits,
              }
            : undefined;
        return (
            <span key={`${activity.metaData?.taskId ?? "noTask"}_${activity.name}`}>
                <SilkActivityControl
                    label={activityLabel}
                    data-test-id={`activity-control-${projectId}-${taskId}-${activity.name}`}
                    executeActivityAction={activityFunctions.executeActivityAction}
                    registerForUpdates={activityFunctions.registerForUpdates}
                    unregisterFromUpdates={activityFunctions.unregisterFromUpdates}
                    translate={translateActions}
                    elapsedTimeOfLastStart={elapsedTime}
                    translateUnits={translateUnits}
                    failureReportAction={{
                        title: "", // The title is already repeated in the markdown
                        allowDownload: true,
                        closeButtonValue: t("common.action.close"),
                        downloadButtonValue: t("common.action.download"),
                        renderMarkdown: true,
                        renderReport: (markdown) => <Markdown children={markdown as string} />,
                        fetchErrorReport: fetchErrorReportFactory(activity),
                    }}
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
                    layoutConfig={layoutConfig}
                    executePrioritized={() => startPrioritized(activity.name, projectId, taskId)}
                />
                <Spacing size={"small"} />
            </span>
        );
    };

    let cacheState = cachesOverallStatus.failedActivities ? (
        <Icon
            name={"state-warning"}
            large
            intent={IntentTypes.WARNING}
            tooltipText={`${cachesOverallStatus.failedActivities} failed activities`}
        />
    ) : (
        <Icon name={"state-success"} large intent={IntentTypes.SUCCESS} />
    );

    // Widget that wraps and summarizes the cache activities
    const CacheGroupWidget = () => {
        const executeRestart = (cursor = 0) => {
            const activity = cacheActivities[cursor]
            if(activity){
                const activityFunctions = activityFunctionsCreator(activity);
                activityFunctions.executeActivityAction("restart")
                activityFunctions.registerForUpdates((status) => {
                    if(!status.isRunning){
                        activityFunctions.unregisterFromUpdates();
                        setTimeout(() => {
                            return executeRestart(cursor + 1)
                        },50)
                    }
                })
            }
        }
        return (
            <OverviewItem hasSpacing>
                <OverviewItemDepiction keepColors>
                    {cachesOverallStatus.currentlyExecuting ? (
                        <Spinner position={"inline"} size={"small"} stroke={"medium"} />
                    ) : (
                        cacheState
                    )}
                </OverviewItemDepiction>
                <OverviewItemDescription>
                    <OverviewItemLine>{t("widget.TaskActivityOverview.cacheGroup.title", "Caches")}</OverviewItemLine>
                    {cachesOverallStatus.oldestStartTime ? (
                        <OverviewItemLine small>
                            <ElapsedDateTimeDisplay
                                data-test-id={"cacheGroup-cache-age"}
                                prefix={t("widget.TaskActivityOverview.cacheGroup.cacheAgePrefix")}
                                suffix={t("widget.TaskActivityOverview.cacheGroup.cacheAgeSuffix")}
                                dateTime={cachesOverallStatus.oldestStartTime}
                                translateUnits={translateUnits}
                            />
                        </OverviewItemLine>
                    ) : (
                        ""
                    )}
                </OverviewItemDescription>
                <OverviewItemActions>
                    <IconButton name="item-reload" text={t("widget.TaskActivityOverview.reloadAllCaches")} onClick={() => executeRestart()} />
                    <IconButton
                        onClick={() =>  setDisplayCacheList(!displayCacheList)}
                        data-test-id={displayCacheList ? "cache-group-show-less-btn" : "cache-group-show-more-btn"}
                        name={displayCacheList ? "toggler-showless" : "toggler-showmore"}
                        text={displayCacheList ? "Hide single caches" : "Show all single caches"}
                    />
                </OverviewItemActions>
            </OverviewItem>
        );
    };

    // Returns the sorted list of activity controls
    function activityWidgets(
        activities: IActivityListEntry[],
        layoutConfig: IActivityControlLayoutProps
    ): JSX.Element[] {
        const activitiesWithLabels = activities
            .map((activity) => {
                const activityLabel = t(
                    `widget.TaskActivityOverview.activities.${activity.name}.title`,
                    activity.label
                );
                return [activityLabel, activityControl(activity, layoutConfig)];
            })
            .sort(([aLabel], [bLabel]) => (aLabel < bLabel ? -1 : 1));
        return activitiesWithLabels.map(([label, activityControl]) => activityControl) as JSX.Element[];
    }

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
                        {mainActivities.map((a) => activityControl(a, { border: true, visualization: "spinner" }))}
                        {cacheActivities.length ? (
                            <Card isOnlyLayout elevation={0} data-test-id={"taskActivityOverview-cacheActivityGroup"}>
                                <CacheGroupWidget />
                                {displayCacheList && (
                                    <>
                                        <Divider />
                                        <WhiteSpaceContainer paddingTop="small" paddingRight="tiny" paddingLeft="tiny">
                                            {activityWidgets(cacheActivities, {
                                                small: true,
                                                visualization: "spinner",
                                            })}
                                        </WhiteSpaceContainer>
                                    </>
                                )}
                            </Card>
                        ) : null}
                        {activityWidgets(failedNonMainActivities, { border: true, visualization: "spinner" })}
                        {activityWidgets(runningNonMainActivities, { border: true, visualization: "spinner" })}
                    </>
                )}
            </CardContent>
        </Card>
    ) : null;
}
