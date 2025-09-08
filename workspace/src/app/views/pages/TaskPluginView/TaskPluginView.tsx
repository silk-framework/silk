import React from "react";
import { useParams } from "react-router";
import NotFound from "../NotFound";
import { pluginRegistry } from "../../plugins/PluginRegistry";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";

/** Renders a task's plugin view independent from the task's detail page. */
export const TaskPluginView = () => {
    const { taskId, projectId, pluginId, viewId } = useParams<TaskViewParams>();
    const initialSettings = useSelector(commonSel.initialSettingsSelector);

    const taskViewPlugins = pluginRegistry.taskViews(pluginId ?? "");
    const taskView = taskViewPlugins.find(
        (plugin) => (!plugin.available || plugin.available(initialSettings)) && plugin.id === viewId,
    );

    return !projectId || !taskId || !taskView ? (
        <NotFound />
    ) : (
        taskView.render(projectId, taskId, { integratedView: true })
    );
};

type TaskViewParams = {
    projectId?: string;
    taskId?: string;
    pluginId?: string;
    viewId?: string;
};

export default TaskPluginView;
