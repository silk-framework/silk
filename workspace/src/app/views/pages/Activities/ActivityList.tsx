import React from "react";
import { useTranslation } from "react-i18next";
import {
    Card,
    Tag,
    TagList,
    Highlighter,
    Spacing,
    OverflowText,
    Notification,
    Icon,
    SilkActivityControlTranslationKeys,
    SilkActivityControlAction,
    SilkActivityStatusProps,
    Markdown,
    SilkActivityControl,
    ElapsedDateTimeDisplayUnits,
} from "@eccenca/gui-elements";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import Datalist from "../../../views/shared/Datalist";
import EmptyList from "../../../views/shared/SearchList/EmptyList";
import AppliedFacets from "../Workspace/AppliedFacets";
import Pagination from "../../shared/Pagination";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { connectWebSocket } from "../../../services/websocketUtils";
import {
    activityActionCreator,
    activityStartPrioritized,
} from "../../../views/shared/TaskActivityOverview/taskActivityOverviewRequests";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { activityErrorReportFactory } from "../../../views/shared/TaskActivityOverview/taskActivityUtils";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { ResourceLink } from "../../shared/ResourceLink/ResourceLink";
import { routerOp } from "@ducks/router";
import { batch } from "react-redux";
import { SERVE_PATH } from "../../../constants/path";
import { DIErrorTypes } from "@ducks/error/typings";
import { AppDispatch } from "store/configureStore";

interface IActivity extends ISearchResultsServer {
    isCacheActivity: boolean;
    parentType: string;
    project: string;
    task: string;
    taskLabel: string;
}

/** ACtivities listed here, will not have a Start or Restart button. */
export const nonStartableActivitiesBlacklist = {
    ExecuteWorkflowWithPayload: true,
    GenerateSparkView: true,
    ExecuteSparkOperator: true,
    DeploySparkWorkflow: true,
    SupervisedLearning: true,
    ActiveLearning: true,
};

const ActivityList = () => {
    const dispatch = useDispatch<AppDispatch>();
    const pageSizes = [10, 25, 50, 100];
    const { registerError } = useErrorHandler();

    const data = useSelector(workspaceSel.resultsSelector);
    const pagination = useSelector(workspaceSel.paginationSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);

    const isLoading = useSelector(workspaceSel.isLoadingSelector);

    // Stores the current status for each activity
    const [activityStatusMap] = React.useState<Map<string, SilkActivityStatusProps>>(new Map());

    // Contains the callback function from a specific activity control that needs to be called every time the status changes, so only the affected activity is re-rendered.
    const [activityUpdateCallback] = React.useState<Map<string, (status: SilkActivityStatusProps) => any>>(new Map());

    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);

    const [t] = useTranslation();

    const isEmpty = !isLoading && !data.length;

    const translateActions = React.useCallback(
        (key: SilkActivityControlTranslationKeys) => t("widget.TaskActivityOverview.activityControl." + key),
        [t],
    );
    const emptyListWithoutFilters: boolean = isEmpty && !textQuery && !appliedFacets.length;

    // Used for keys in activity->value maps
    const activityKey = (activity: string, projectId?: string, taskId?: string): string =>
        `${activity}|${projectId ?? ""}|${taskId ?? ""}`;

    const updateActivityStatus = (status: SilkActivityStatusProps) => {
        const key = activityKey(status.activity, status.project, status.task);
        activityStatusMap.set(key, status);
        activityUpdateCallback.get(key)?.(status);
    };

    const handlePaginationOnChange = (n: number, pageSize: number) => {
        batch(() => {
            dispatch(workspaceOp.changeLimitOp(pageSize));
            dispatch(workspaceOp.changePageOp(n));
        });
    };

    // Register for activity status updates in backend
    const registerForUpdates = () => {
        return connectWebSocket(
            legacyApiEndpoint("/activities/updatesWebSocket"),
            legacyApiEndpoint("/activities/updates"),
            updateActivityStatus,
        );
    };

    // Register an observer from the activity widget
    const createRegisterForUpdatesFn =
        (activityKey: string) => (callback: (status: SilkActivityStatusProps) => any) => {
            activityUpdateCallback.set(activityKey, callback);
            const currentStatus = activityStatusMap.get(activityKey);
            currentStatus && callback(currentStatus);
        };

    // Unregister from updates when an activity control is not shown anymore
    const createUnregisterFromUpdateFn = (activityKey: string) => () => {
        activityUpdateCallback.delete(activityKey);
    };

    const executeAction = (activityName: string, action: SilkActivityControlAction, project, task) => {
        const mainAction = activityActionCreator(activityName, project, task, () => {});
        return mainAction(action);
    };

    const registerActivityError = (action: string) => (activityName: string, error: DIErrorTypes) => {
        registerError(
            `ActivityList.${action}`,
            `Activity action '${action}' against activity '${activityName}' could not be executed.`,
            error,
        );
    };

    const startPrioritized = async (activityName: string, project?: string, task?: string) => {
        await activityStartPrioritized(activityName, project, task, registerActivityError("Run prioritized"));
    };

    // Returns a function that fetches the error report for a particular activity
    const fetchErrorReportFactory = (activity: IActivity) => {
        return activityErrorReportFactory(activity.id, activity.project, activity.task, (ex) => {
            registerError(
                `taskActivityOverview-fetchErrorReport`,
                t("widget.TaskActivityOverview.errorMessages.errorReport.fetchReport"),
                ex,
            );
        });
    };

    // Query string for an activity related backend request
    const queryString = (activity: IActivity): string => {
        const projectParameter = activity.project ? `&project=${activity.project}` : "";
        const taskParameter = activity.task ? `&task=${activity.task}` : "";
        return `?activity=${activity.id}${projectParameter}${taskParameter}`;
    };

    const ActivityTags = ({ activity }: any) => {
        return (
            <TagList>
                {activity.projectLabel && (
                    <Tag>
                        <Highlighter label={activity.projectLabel} searchValue={textQuery} />
                    </Tag>
                )}
                {activity.parentType && (
                    <Tag>
                        <Highlighter
                            label={t(
                                "widget.Filterbar.subsections.valueLabels.itemType." + activity.parentType,
                                activity.parentType[0].toUpperCase() + activity.parentType.substr(1),
                            )}
                            searchValue={textQuery}
                        />
                    </Tag>
                )}
            </TagList>
        );
    };

    React.useEffect(() => {
        return registerForUpdates();
    }, []);

    const EmptyContainer = emptyListWithoutFilters ? (
        <EmptyList
            depiction={<Icon name={["artefact-uncategorized"]} large />}
            textInfo={<p>{t("pages.activities.noActivities")}</p>}
            textCallout={null}
            actionButtons={[]}
        />
    ) : (
        <Notification>{t("pages.activities.noActivities", { items: "items" })}</Notification>
    );

    const translateUnits = (unit: ElapsedDateTimeDisplayUnits) => t("common.units." + unit, unit);

    return (
        <>
            <AppliedFacets />
            <Datalist
                data-test-id="search-result-list"
                isEmpty={isEmpty}
                isLoading={isLoading}
                hasSpacing
                emptyContainer={EmptyContainer}
            >
                {data.map((activity: IActivity, index) => {
                    const link = `${SERVE_PATH}/projects/${activity.project}${
                        activity.task ? `/${activity.parentType}/${activity.task}` : ""
                    }`;

                    const ActivityLabel = () => (
                        <>
                            <Highlighter label={activity.label} searchValue={textQuery} />
                            {activity.parentType !== "global" ? "  of" : ""}
                            <Spacing vertical size="tiny" />
                            <ResourceLink
                                url={link}
                                handlerResourcePageLoader={(e) => {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    dispatch(routerOp.goToPage(link));
                                }}
                            >
                                <Highlighter
                                    label={activity.task ? activity.taskLabel : activity.projectLabel}
                                    searchValue={textQuery}
                                />
                            </ResourceLink>
                        </>
                    );

                    const key = activityKey(activity.id, activity.project, activity.task);
                    const startableActivity = !nonStartableActivitiesBlacklist[activity.id];

                    return (
                        <Card isOnlyLayout key={index}>
                            <SilkActivityControl
                                layoutConfig={{
                                    small: false,
                                    border: false,
                                    hasSpacing: true,
                                    canShrink: false,
                                    visualization: "spinner",
                                    labelWrapper: <OverflowText passDown inline />,
                                }}
                                label={<ActivityLabel />}
                                tags={<ActivityTags activity={activity} />}
                                registerForUpdates={createRegisterForUpdatesFn(key)}
                                unregisterFromUpdates={createUnregisterFromUpdateFn(key)}
                                showReloadAction={activity.isCacheActivity && startableActivity}
                                showStartAction={!activity.isCacheActivity && startableActivity}
                                translateUnits={translateUnits}
                                showStopAction
                                executeActivityAction={(action: SilkActivityControlAction) =>
                                    executeAction(activity.id, action, activity.project, activity.task)
                                }
                                failureReportAction={{
                                    title: t("widget.WarningWidget.title"),
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
                                executePrioritized={() =>
                                    startPrioritized(activity.id, activity.project, activity.task)
                                }
                            />
                        </Card>
                    );
                })}
            </Datalist>
            <Pagination pagination={pagination} pageSizes={pageSizes} onChangeSelect={handlePaginationOnChange} />
        </>
    );
};

export default ActivityList;
