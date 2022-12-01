import React from "react";
import { useParams } from "react-router";

import { pluginRegistry } from "../../plugins/PluginRegistry";
import NotFound from "../NotFound";

/** Renders a task's plugin view independent from the task's detail page. */
export const TaskPluginView = () => {
    const { taskId, projectId, pluginId, viewId } = useParams<TaskViewParams>();

    const taskViewPlugins = pluginRegistry.taskViews(pluginId ?? "");
    const taskView = taskViewPlugins.find((plugin) => plugin.id === viewId);

    return !projectId || !taskId || !taskView ? (
        <NotFound />
    ) : (
        taskView.render(projectId, taskId, { integratedView: true })
    );
};

interface TaskViewParams {
    projectId?: string;
    taskId?: string;
    pluginId?: string;
    viewId?: string;
}

export default TaskPluginView;
