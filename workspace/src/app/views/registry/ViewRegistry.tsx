/** A view / UI of a project task.
 * Each task can have multiple views.
 **/
import React from "react";
import FlowEditor from "../pages/FlowEditor";

// Generic actions and callbacks on views
export interface IViewActions {
    // A callback that is executed every time the workflow is saved
    onSave?: () => any;
}
/** A project task view that is meant to be displayed for a specific project task. */
export interface IProjectTaskView {
    // The ID of the view to make the views distinguishable from each other
    id: string;
    // The label that should be shown to the user
    label: string;
    // Function that renders the view
    render: (projectId: string, taskId: string, viewActions?: IViewActions) => JSX.Element;
}

class ViewRegistry {
    // Stores all views for a specific plugin
    private pluginViewRegistry: Map<string, IProjectTaskView[]>;

    private registerView(pluginId: string, view: IProjectTaskView) {
        let views: IProjectTaskView[] | undefined = this.pluginViewRegistry.get(pluginId);
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
            render: (projectId, taskId, viewActions) => (
                <FlowEditor projectId={projectId} workflowId={taskId} viewActions={viewActions} />
            ),
        });
    }

    public projectTaskViews(taskPluginId: string): IProjectTaskView[] {
        const views = this.pluginViewRegistry.get(taskPluginId);
        return views ? [...views] : [];
    }
}

export const viewRegistry = new ViewRegistry();
