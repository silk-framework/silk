import React from "react";
import { useParams } from "react-router";
import NotFound from "../NotFound";
import { pluginRegistry } from "../../plugins/PluginRegistry";

export const TaskPluginView = () => {
    const { taskId, projectId, pluginId, viewId } = useParams<TaskViewParams>();

    const taskViewPlugins = pluginRegistry.taskViews(pluginId ?? "");
    const taskView = taskViewPlugins.find((plugin) => plugin.id === viewId);

    return !projectId || !taskId || !taskView ? <NotFound /> : taskView.render(projectId, taskId);
};

interface TaskViewParams {
    projectId?: string;
    taskId?: string;
    pluginId?: string;
    viewId?: string;
}

export default TaskPluginView;
