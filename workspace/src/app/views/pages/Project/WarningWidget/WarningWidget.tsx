import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { Intent } from "@eccenca/gui-elements/blueprint/constants";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Divider,
    IconButton,
    Notification,
    Spacing,
} from "@eccenca/gui-elements";
import MarkdownModal from "../../../shared/modals/MarkdownModal";
import { AppToaster } from "../../../../services/toaster";
import { commonOp, commonSel } from "@ducks/common";
import Loading from "../../../shared/Loading";
import { useTranslation } from "react-i18next";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { FixTaskButton } from "./FixTaskButton";
import { TemplateValueType } from "@ducks/shared/typings";
import { requestArtefactProperties } from "@ducks/common/requests";
import { commonSlice } from "../../../../store/ducks/common/commonSlice";
import { failedTaskParameters, fixFailedTask } from "./TaskLoadingError.requests";
import { AlternativeTaskUpdateFunction } from "@ducks/common/typings";
import { FixTaskDataNotFoundModal } from "./FixTaskDataNotFoundModal";
import { TaskParameterValues } from "./TaskLoadingError.typing";
import { AppDispatch } from "store/configureStore";

interface Props {
    refreshProjectPage: () => any;
}
/** Displays the task loading errors for a project, i.e. tasks that could not be loaded/initialized. */
export const ProjectTaskLoadingErrors = ({ refreshProjectPage }: Props) => {
    const { registerErrorI18N } = useErrorHandler();
    const dispatch = useDispatch<AppDispatch>();
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const warningList = useSelector(workspaceSel.warningListSelector);
    const { cachedArtefactProperties } = useSelector(commonSel.artefactModalSelector);
    /** The modal will be shown if no original task data was found. The modal will offer to reload the task without any adaptions. */
    const [showNotFoundModal, setShowNotFoundModal] = useState<{
        show: boolean;
        notFoundMessage?: string;
        taskId?: string;
    }>({ show: false });

    const warnWidget = useSelector(workspaceSel.widgetsSelector).warnings;
    const { isLoading } = warnWidget;

    const [currentMarkdown, setCurrentMarkdown] = useState("");
    const [isOpen, setIsOpen] = useState<boolean>(false);
    const [t] = useTranslation();

    const fetchWarningList = () => {
        if (projectId) {
            dispatch(workspaceOp.fetchWarningListAsync(projectId));
        }
    };

    useEffect(() => {
        fetchWarningList();
    }, [workspaceOp, projectId]);

    // Fetch artefact description from cache or fetch and update
    const artefactProperties = async (artefactId: string) => {
        if (cachedArtefactProperties[artefactId]) {
            return cachedArtefactProperties[artefactId];
        } else {
            const taskPluginDetails = await requestArtefactProperties(artefactId);
            dispatch(commonSlice.actions.setCachedArtefactProperty(taskPluginDetails));
            return taskPluginDetails;
        }
    };

    const handleOpen = () => setIsOpen(true);
    const handleClose = () => {
        setCurrentMarkdown("");
        setIsOpen(false);
    };

    const handleOpenMarkDown = async (taskId, projectId) => {
        try {
            const markdown: string = await workspaceOp.fetchWarningMarkdownAsync(taskId, projectId);
            handleOpen();
            setCurrentMarkdown(markdown);
        } catch (err) {
            registerErrorI18N("http.error.not.markdown", err);
        }
    };
    const fixTask: AlternativeTaskUpdateFunction = async (
        projectId,
        taskId,
        parameterData,
        variableTemplateData,
        dataParameters,
    ) => {
        await fixFailedTask(
            projectId,
            taskId,
            parameterData as TaskParameterValues,
            variableTemplateData as TaskParameterValues,
            dataParameters,
        );
        // Update warning list and project page
        fetchWarningList();
        refreshProjectPage();
    };

    const handleInitFixTask = async (taskId: string, projectId: string, taskLabel: string) => {
        setShowNotFoundModal({ show: false });
        try {
            // Open the fix modal for the task
            // Config dialog is always opened with fresh data
            const failedTaskData = (await failedTaskParameters(projectId, taskId)).data;
            const taskPluginDetails = await artefactProperties(failedTaskData.pluginId);
            const parameters = failedTaskData.parameterValues.parameters;
            const templateParameters = failedTaskData.parameterValues.templates;
            const dataParameters: Record<string, string> | undefined =
                taskPluginDetails.taskType === "Dataset"
                    ? {
                          readOnly: `${`${parameters.readOnly}` === "true"}`,
                      }
                    : undefined;
            if (dataParameters && typeof parameters.uriProperty === "string") {
                dataParameters.uriProperty = parameters.uriProperty;
            }
            const templates: TemplateValueType = (templateParameters as TemplateValueType) ?? {};
            dispatch(
                commonOp.updateProjectTask({
                    projectId,
                    taskId,
                    // The label of the task is not displayed, since we use a custom modal title, see below
                    metaData: { label: taskId },
                    taskPluginDetails: taskPluginDetails,
                    currentParameterValues: parameters,
                    dataParameters: dataParameters,
                    currentTemplateValues: templates,
                    // FIXME: Add label to failedTaskData in order to display task label
                    alternativeTitle: t("widget.WarningWidget.fixTaskModalTitle", { taskLabel: taskLabel }),
                    alternativeUpdateFunction: fixTask,
                }),
            );
        } catch (ex) {
            if (ex.httpStatus === 404 || ex.status === 404) {
                // Either the task plugin details or the original task data is missing. Show Reload dialog in both cases.
                setShowNotFoundModal({
                    show: true,
                    notFoundMessage: ex.message ? ex.message : ex.detail,
                    taskId,
                });
            } else {
                registerErrorI18N("widget.WarningWidget.fixTaskInitFailed", ex);
            }
        }
    };

    if (isLoading) return <Loading description={t("widget.WarningWidget.loading", "Loading log messages.")} />;

    return warningList.length > 0 ? (
        <>
            <Card>
                {showNotFoundModal.show && projectId && showNotFoundModal.taskId ? (
                    <FixTaskDataNotFoundModal
                        onReload={async () => await fixTask(projectId, showNotFoundModal.taskId!, {}, {}, {})}
                        onClose={() => setShowNotFoundModal({ show: false })}
                    />
                ) : null}
                <CardHeader>
                    <CardTitle>
                        <h2>{t("widget.WarningWidget.title", "Error log")}</h2>
                    </CardTitle>
                </CardHeader>
                <Divider />
                <CardContent>
                    <ul>
                        {warningList.map((warn, id) => {
                            const actions: React.JSX.Element[] = projectId
                                ? [
                                      <FixTaskButton
                                          text={t("widget.WarningWidget.fixTask")}
                                          handleClick={() => handleInitFixTask(warn.taskId, projectId, warn.taskLabel)}
                                      />,
                                      <IconButton
                                          name={"artefact-report"}
                                          data-test-id={"taskLoadingReportBtn"}
                                          minimal
                                          text={t("common.action.ShowSmth", { smth: "report" })}
                                          onClick={() => handleOpenMarkDown(warn.taskId, projectId)}
                                      />,
                                  ]
                                : [];
                            return (
                                <li key={"notification_" + id} data-test-id={"project-task-loading-error-notification"}>
                                    <Notification danger actions={actions}>
                                        {warn.errorSummary}
                                    </Notification>
                                    <Spacing size={"tiny"} />
                                </li>
                            );
                        })}
                    </ul>
                    <MarkdownModal isOpen={isOpen} onDiscard={handleClose} markdown={currentMarkdown} />
                </CardContent>
            </Card>
            <Spacing />
        </>
    ) : null;
};
