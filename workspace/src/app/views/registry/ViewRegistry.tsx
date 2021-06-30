/** A view / UI of a project task.
 * Each task can have multiple views.
 **/
import React from "react";
import FlowEditor from "../pages/FlowEditor";

/** A project task view that is meant to be displayed for a specific project task. */
export interface IProjectTaskView {
    // The ID of the view to make the views distinguishable from each other
    id: string;
    // The label that should be shown to the user
    label: string;
    // Function that renders the view
    render: (projectId: string, taskId: string) => JSX.Element;
}

class ViewRegistry {
    // Stores all views for a specific plugin
    private pluginViewRegistry: Map<string, IProjectTaskView[]>;

    private registerView(pluginId: string, view: IProjectTaskView) {
        let views: IProjectTaskView[] = this.pluginViewRegistry.get(pluginId);
        if (!views) {
            views = [];
            this.pluginViewRegistry.set(pluginId, views);
        }
        if (views.every((v) => v.id !== view.id)) {
            views.push(view);
        } else {
            console.warn(
                `Trying to register project task plugin view '${view.id}' that already exists in the registry for plugin '${pluginId}'!`
            );
        }
    }

    constructor() {
        this.pluginViewRegistry = new Map<string, IProjectTaskView[]>();
        // Add default views
        this.registerView("workflow", {
            id: "editor",
            label: "Workflow editor v2",
            render: (projectId, taskId) => <FlowEditor projectId={projectId} workflowId={taskId} />,
        });
    }

    public projectTaskViews(taskPluginId: string): IProjectTaskView[] {
        const views = this.pluginViewRegistry.get(taskPluginId);
        return views ? [...views] : [];
    }
}

export const viewRegistry = new ViewRegistry();
