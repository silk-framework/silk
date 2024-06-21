import React, { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
    IActivityStatus,
    ActivityAction,
    useSilkActivityControl,
    IActivityControlLayoutProps,
    Markdown,
    TimeUnits,
} from "@eccenca/gui-elements";
import { DIErrorTypes } from "@ducks/error/typings";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { activityErrorReportFactory, activityQueryString } from "../TaskActivityOverview/taskActivityUtils";
import { connectWebSocket } from "../../../services/websocketUtils";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { activityActionCreator, activityStartPrioritized } from "../TaskActivityOverview/taskActivityOverviewRequests";

interface TaskActivityWidgetProps {
    projectId: string;
    taskId: string;
    // Activity name/ID this control is rendered for
    activityName: string;
    // Label that should be displayed above the progress bar
    label?: string;
    // display config
    layoutConfig?: IActivityControlLayoutProps;
    // Allows to add logic that is executed when an action button has been clicked and may call the actual action (mainAction) at any time.
    // It may abort the execution of the action by not calling 'mainAction'.
    activityActionPreAction?: {
        // key is typed as string but should be an ActivityAction (start, cancel, restart), which is not allowed for index signature parameter types
        [key: string]: (mainAction: () => Promise<boolean>) => Promise<boolean>;
    };
    /** If the activity is a cache activity the presentation will be different, e.g. reload button shown etc. */
    isCacheActivity?: boolean;
    /**
     * callback executed when an update is received.
     */
    updateCallback?: (status: IActivityStatus) => void;
    /** Optional test ID. */
    testId?: string;
}

/** Task activity widget to show the activity status and start / stop task activities. */
export const TaskActivityWidget = (props: TaskActivityWidgetProps) => {
    const { widget } = useTaskActivityWidget(props);
    return widget;
};

export const useTaskActivityWidget = ({
    projectId,
    taskId,
    activityName,
    label = "",
    layoutConfig,
    activityActionPreAction = {},
    updateCallback,
    isCacheActivity = false,
    testId = `activity-control-workflow-editor`,
}: TaskActivityWidgetProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [updatesHandler] = useState<{ updateHandler: ((status: IActivityStatus) => any) | undefined }>({
        updateHandler: undefined,
    });
    const [activityStatus] = useState<{ activityStatus: IActivityStatus | undefined }>({ activityStatus: undefined });

    const updateActivityStatus = (status: IActivityStatus) => {
        activityStatus.activityStatus = status;
        if (updatesHandler.updateHandler) {
            updatesHandler.updateHandler(status);
        }
        if (updateCallback) {
            updateCallback(status);
        }
    };

    const handleError = (activityName: string, action: ActivityAction, error: DIErrorTypes) => {
        registerError(
            `taskActivityWidget-${projectId}-${taskId}-${activityName}-${action}`,
            t("widget.TaskActivityOverview.errorMessages.actions." + action, { activityName: activityName }),
            error
        );
    };

    // Callback for the activity control to register for updates
    const registerForUpdate = (callback: (status: IActivityStatus) => any) => {
        updatesHandler.updateHandler = callback;
    };

    // Register for activity status updates in backend
    const registerForUpdates = () => {
        const query = activityQueryString(projectId, taskId, activityName);
        return connectWebSocket(
            legacyApiEndpoint(`/activities/updatesWebSocket${query}`),
            legacyApiEndpoint(`/activities/updates${query}`),
            updateActivityStatus
        );
    };
    useEffect(() => {
        return registerForUpdates();
    }, [projectId, taskId, activityName]);

    const activityErrorReport = activityErrorReportFactory(activityName, projectId, taskId, (ex) => {
        registerError(
            `taskActivityOverview-fetchErrorReport`,
            t("widget.TaskActivityOverview.errorMessages.errorReport.fetchReport"),
            ex
        );
    });

    const executeAction = (action: ActivityAction) => {
        const preAction = activityActionPreAction[action];
        const originalAction = activityActionCreator(activityName, projectId, taskId, handleError);
        if (preAction !== undefined) {
            return preAction(() => originalAction(action));
        } else {
            return originalAction(action);
        }
    };

    const executePrioritized = React.useCallback(async () => {
        const registerActivityError = (activityName: string, error: DIErrorTypes) => {
            registerError(
                `TaskActivityWidget.${projectId}-${taskId}-${activityName}.startPrioritized`,
                `Activity action 'Run prioritized' against activity '${activityName}' has failed, see details.`,
                error
            );
        };
        await activityStartPrioritized(activityName, projectId, taskId, registerActivityError);
    }, []);

    const translate = useCallback((key: string) => t("widget.TaskActivityOverview.activityControl." + key), [t]);
    // For the elapsed time component, showing when a cache was last updated
    const translateUnits = (unit: TimeUnits) => t("common.units." + unit, unit);

    return useSilkActivityControl({
        label,
        "data-test-id": testId,
        executeActivityAction: executeAction,
        registerForUpdates: registerForUpdate,
        unregisterFromUpdates: () => {},
        translate,
        failureReportAction: {
            title: "", // The title is already repeated in the markdown
            allowDownload: true,
            closeButtonValue: t("common.action.close"),
            downloadButtonValue: t("common.action.download"),
            renderMarkdown: true,
            renderReport: (markdown) => <Markdown children={markdown as string} />,
            fetchErrorReport: activityErrorReport,
        },
        showStartAction: !isCacheActivity,
        showStopAction: true,
        showReloadAction: isCacheActivity,
        layoutConfig: layoutConfig,
        translateUnits,
        elapsedTimeOfLastStart: isCacheActivity
            ? {
                  translate: translateUnits,
                  prefix: ` (${t("widget.TaskActivityOverview.cacheGroup.cacheAgePrefixIndividual")}`,
                  suffix: `${t("widget.TaskActivityOverview.cacheGroup.cacheAgeSuffix")})`,
              }
            : undefined,
        hideMessageOnStatus: isCacheActivity ? (concreteStatus) => concreteStatus === "Successful" : undefined,
        executePrioritized,
    });
};
