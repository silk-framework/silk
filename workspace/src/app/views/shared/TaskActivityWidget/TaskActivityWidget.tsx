import { DIErrorTypes } from "@ducks/error/typings";
import { useTranslation } from "react-i18next";
import useErrorHandler from "../../../hooks/useErrorHandler";
import React, { useCallback, useEffect, useState } from "react";
import { IActivityStatus } from "@gui-elements/src/cmem/ActivityControl/ActivityControlTypes";
import { activityErrorReportFactory, activityQueryString } from "../TaskActivityOverview/taskActivityUtils.";
import { connectWebSocket } from "../../../services/websocketUtils";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { activityActionCreator } from "../TaskActivityOverview/taskActivityOverviewRequests";
import {
    ActivityAction,
    DataIntegrationActivityControl,
} from "@gui-elements/src/cmem/ActivityControl/DataIntegrationActivityControl";
import ReactMarkdown from "react-markdown";

interface IProps {
    projectId: string;
    taskId: string;
    // Activity name/ID this control is rendered for
    activityName: string;
    // Label that should be displayed above the progress bar
    label?: string;
}

/** Task activity widget to show the activity status and start / stop task activities. */
export const TaskActivityWidget = ({ projectId, taskId, activityName, label = "" }: IProps) => {
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
    const executeActions = activityActionCreator(activityName, projectId, taskId, handleError);
    const translate = useCallback((key: string) => t("widget.TaskActivityOverview.activityControl." + key), [t]);

    // TODO: Fix size issues with activity control and tooltip
    return (
        <div style={{ minWidth: "400px", maxWidth: "400px" }}>
            <DataIntegrationActivityControl
                label={label}
                data-test-id={`activity-control-workflow-editor`}
                executeActivityAction={executeActions}
                registerForUpdates={registerForUpdate}
                unregisterFromUpdates={() => {}}
                translate={translate}
                failureReportAction={{
                    title: "", // The title is already repeated in the markdown
                    allowDownload: true,
                    closeButtonValue: t("common.action.close"),
                    downloadButtonValue: t("common.action.download"),
                    renderMarkdown: true,
                    renderReport: (markdown) => <ReactMarkdown source={markdown as string} />,
                    fetchErrorReport: activityErrorReport,
                }}
                showProgress={true}
                showStartAction={true}
                showStopAction={true}
                showReloadAction={false}
            />
        </div>
    );
};
