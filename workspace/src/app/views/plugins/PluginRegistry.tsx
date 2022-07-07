import {registerCorePlugins} from "./RegisteredCoreTaskPlugins"
/** A view / UI of a project task.
 * Each task can have multiple views.
 **/
// Generic actions and callbacks on views
export interface IViewActions {
    // A callback that is executed every time the project task is saved from the view.
    onSave?: () => any;
    /** If true then the task view is integrated into another view and not on the task details page. Things like the
     * error notifications need to be inside the editor then. */
    integratedView?: boolean;
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
export interface IPluginComponent<I> {
    // The ID of the plugin component that must be globally unique
    id: string;
    // The label that should be shown to the user
    label: string;
    // Function that renders the view
    Component: (params: I) => JSX.Element;
}

class PluginRegistry {
    // Stores all views for a specific plugin
    private taskViewPluginRegistry: Map<string, IProjectTaskView[]>;
    private pluginReactComponents: Map<string, IPluginComponent<any>>;
    private pluginComponents: Map<string, any>;

    /** Register a view component for a specific task plugin. Each task plugin can have multiple views registered. */
    public registerTaskView(pluginId: string, view: IProjectTaskView) {
        let views: IProjectTaskView[] | undefined = this.taskViewPluginRegistry.get(pluginId);
        if (!views) {
            views = [];
            this.taskViewPluginRegistry.set(pluginId, views);
        }
        if (views.every((v) => v.id !== view.id)) {
            views.push(view);
        } else {
            console.warn(
                `Trying to register project task plugin view '${view.id}' that already exists in the registry for plugin '${pluginId}'!`
            );
        }
    }

    /** Registers a plugin component. The type parameter is just for documentation and type checking on the caller-side. */
    public registerReactPluginComponent<I = never>(pluginComponent: IPluginComponent<I>) {
        if (this.pluginReactComponents.has(pluginComponent.id)) {
            console.warn(
                `Trying to register a React plugin component with ID '${pluginComponent.id}' that already exists in the React plugin component registry!`
            );
        } else {
            this.pluginReactComponents.set(pluginComponent.id, pluginComponent);
        }
    }

    /** Registers an arbitrary plugin with a specific interface. The type parameter is just for documentation and type checking on the caller-side. */
    public registerPluginComponent<I extends object = never>(pluginId: string, plugin: I) {
        if (this.pluginComponents.has(pluginId)) {
            console.warn(
                `Trying to register a plugin component with ID '${pluginId}' that already exists in the plugin component registry!`
            );
        } else {
            this.pluginComponents.set(pluginId, plugin);
        }
    }

    constructor() {
        this.taskViewPluginRegistry = new Map<string, IProjectTaskView[]>();
        this.pluginReactComponents = new Map<string, IPluginComponent<any>>();
        this.pluginComponents = new Map<string, any>();
    }

    /** Fetches the task views of a task plugin. */
    public taskViews(taskPluginId: string): IProjectTaskView[] {
        const views = this.taskViewPluginRegistry.get(taskPluginId);
        return views ? [...views] : [];
    }

    /** Fetches a plugin component. The type parameter is just for documentation and type checking on the caller-side. */
    public pluginReactComponent<I = never>(pluginId: string): IPluginComponent<I> | undefined {
        return this.pluginReactComponents.get(pluginId);
    }

    /** Fetches an arbitrary plugin. The type parameter is just for documentation and type checking on the caller-side. */
    public pluginComponent<I = never>(pluginId: string): I {
        return this.pluginComponents.get(pluginId);
    }
}

export const pluginRegistry = new PluginRegistry();

export const SUPPORTED_PLUGINS = {
    DATA_PREVIEW: "di:dataPreview",
    DI_USER_MENU_ITEMS: "di:userMenuItems",
    DI_USER_MENU_FOOTER: "di:userMenuFooter",
    DI_LANGUAGE_SWITCHER: "di:languageSwitcher",
    DI_BRANDING: "di:branding",
};

registerCorePlugins()
