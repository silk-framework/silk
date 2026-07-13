import { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { commonOp, commonSel } from "@ducks/common";
import { requestTaskData } from "@ducks/shared/requests";
import { requestArtefactProperties } from "@ducks/common/requests";
import { TemplateValueType } from "@ducks/shared/typings";
import { commonSlice } from "@ducks/common/commonSlice";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { AppDispatch } from "store/configureStore";

/** Opens the task configuration (update) modal with freshly fetched task data.
 * Extracted from the former TaskConfig widget so the page header actions menu
 * can offer "Configure" without a widget being mounted. */
export const useTaskConfigModal = (projectId?: string, taskId?: string) => {
    const { registerError } = useErrorHandler();
    const dispatch = useDispatch<AppDispatch>();
    const [loading, setLoading] = useState(false);
    const { cachedArtefactProperties } = useSelector(commonSel.artefactModalSelector);
    const [t] = useTranslation();

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

    // Open the update modal for the task
    const openConfigModal = async () => {
        if (!projectId || !taskId) {
            return;
        }
        setLoading(true);
        try {
            // Config dialog is always opened with fresh data
            const taskData = (await requestTaskData(projectId, taskId, true)).data;
            const taskPluginDetails = await artefactProperties(taskData.data.type);
            const dataParameters: Record<string, string> | undefined =
                taskPluginDetails.taskType === "Dataset"
                    ? {
                          readOnly: `${taskData.data.readOnly === true}`,
                      }
                    : undefined;
            if (dataParameters && taskData.data.uriProperty) {
                dataParameters.uriProperty = taskData.data.uriProperty;
            }
            const templates: TemplateValueType = taskData.data.templates ?? {};
            dispatch(
                commonOp.updateProjectTask({
                    projectId: taskData.project,
                    taskId: taskData.id,
                    metaData: taskData.metadata,
                    taskPluginDetails: taskPluginDetails,
                    currentParameterValues: taskData.data.parameters,
                    dataParameters: dataParameters,
                    currentTemplateValues: templates,
                }),
            );
        } catch (e) {
            registerError(
                "useTaskConfigModal-openConfigModal",
                t("widget.TaskConfigWidget.openError", "Cannot open edit dialog."),
                e,
            );
        } finally {
            setLoading(false);
        }
    };

    return { openConfigModal, loading } as const;
};
