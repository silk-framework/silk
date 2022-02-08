import React, { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
    IActivityStatus,
    ActivityAction,
    SilkActivityControl,
    IActivityControlLayoutProps,
    Markdown,
} from "gui-elements/cmem";
import { DIErrorTypes } from "@ducks/error/typings";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { activityErrorReportFactory, activityQueryString } from "../TaskActivityOverview/taskActivityUtils";
import { connectWebSocket } from "../../../services/websocketUtils";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { activityActionCreator } from "../TaskActivityOverview/taskActivityOverviewRequests";

interface IProps {
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
        // key is typed as string but should be ActivityAction, which is not allowed for index signature parameter types
        [key: string]: (mainAction: () => Promise<void>) => Promise<void>;
    };
}

/** Task activity widget to show the activity status and start / stop task activities. */
export const TaskActivityWidget = ({
    projectId,
    taskId,
    activityName,
    label = "",
    layoutConfig,
    activityActionPreAction = {},
}: IProps) => {
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
    }, []);

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
    const translate = useCallback((key: string) => t("widget.TaskActivityOverview.activityControl." + key), [t]);

    // TODO: Fix size issues with activity control and tooltip
    return (
        <SilkActivityControl
            label={label}
            data-test-id={`activity-control-workflow-editor`}
            executeActivityAction={executeAction}
            registerForUpdates={registerForUpdate}
            unregisterFromUpdates={() => {}}
            translate={translate}
            failureReportAction={{
                title: "", // The title is already repeated in the markdown
                allowDownload: true,
                closeButtonValue: t("common.action.close"),
                downloadButtonValue: t("common.action.download"),
                renderMarkdown: true,
                renderReport: (markdown) => <Markdown children={markdown as string} />,
                fetchErrorReport: activityErrorReport,
            }}
            showStartAction={true}
            showStopAction={true}
            showReloadAction={false}
            layoutConfig={layoutConfig}
        />
    );
};
