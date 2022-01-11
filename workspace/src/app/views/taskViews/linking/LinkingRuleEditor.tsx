import React from "react";
import { requestTaskData } from "@ducks/shared/requests";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { LinkingTaskParameters } from "./linking.types";
import { useTranslation } from "react-i18next";
import { IViewActions } from "../../plugins/PluginRegistry";
import { RuleEditor } from "../../../views/shared/RuleEditor/RuleEditor";

export interface LinkingRuleEditorProps<TASK_TYPE> {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    linkingTaskId: string;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
}

export const LinkingRuleEditor = ({ projectId, linkingTaskId }: LinkingRuleEditorProps<any>) => {
    // The linking task parameters
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    // Fetches the parameters of the linking task
    const fetchTaskData = async (projectId: string, taskId: string) => {
        try {
            const taskData = (await requestTaskData(projectId, taskId)).data;
            return taskData.data.parameters as LinkingTaskParameters;
        } catch (err) {
            registerError(
                "LinkingRuleEditor_fetchLinkingTask",
                t("taskViews.linkRulesEditor.errors.fetchTaskData.msg"),
                err
            );
        }
    };
    return (
        <RuleEditor<LinkingTaskParameters> projectId={projectId} taskId={linkingTaskId} fetchTaskData={fetchTaskData} />
    );
};
