/** A view / UI of a project task.
 * Each task can have multiple views.
 **/

// Generic actions and callbacks on views
export interface IViewActions {
    // A callback that is executed every time the workflow is saved
    onSave?: () => any;
}
/** A project task view that is meant to be displayed for a specific project task.
 * A view only receives that project and task ID and should be self-contained otherwise. */
export interface IProjectTaskView {
    // The ID of the view to make the views distinguishable from each other
    id: string;
    // The label that should be shown to the user
    label: string;
    // Function that renders the view
    render: (projectId: string, taskId: string, viewActions?: IViewActions) => JSX.Element;
}

/** A plugin component that can receive arbitrary parameters. */
export interface IPluginComponent {
    // The ID of the plugin component that must be globally unique
    id: string;
    // The label that should be shown to the user
    label: string;
    // Function that renders the view
    Component: (params: { [key: string]: any }) => JSX.Element;
}

class ViewRegistry {
    // Stores all views for a specific plugin
    private pluginViewRegistry: Map<string, IProjectTaskView[]>;
    private pluginComponents: Map<string, IPluginComponent>;

    public registerView(pluginId: string, view: IProjectTaskView) {
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

    public registerPluginComponent(pluginComponent: IPluginComponent) {
        if (this.pluginComponents.has(pluginComponent.id)) {
            console.warn(
                `Trying to register a plugin component with ID '${pluginComponent.id}' that already exists in the plugin component registry!`
            );
        } else {
            this.pluginComponents.set(pluginComponent.id, pluginComponent);
        }
    }

    constructor() {
        this.pluginViewRegistry = new Map<string, IProjectTaskView[]>();
        this.pluginComponents = new Map<string, IPluginComponent>();
    }

    public projectTaskViews(taskPluginId: string): IProjectTaskView[] {
        const views = this.pluginViewRegistry.get(taskPluginId);
        return views ? [...views] : [];
    }

    public pluginComponent(pluginId: string): IPluginComponent | undefined {
        return this.pluginComponents.get(pluginId);
    }
}

export const viewRegistry = new ViewRegistry();

export const SUPPORTED_PLUGINS = {
    DATA_PREVIEW: "di:dataPreview",
    DI_USER_MENU_ITEMS: "di:userMenuItems",
    DI_USER_MENU_FOOTER: "di:userMenuFooter",
};
