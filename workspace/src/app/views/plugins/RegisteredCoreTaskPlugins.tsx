import {LinkingRuleEditor} from "../taskViews/linking/LinkingRuleEditor";
import React from "react";
import {IViewActions, pluginRegistry} from "./PluginRegistry";
import HierarchicalMapping from "../pages/MappingEditor/HierarchicalMapping/HierarchicalMapping";

let registered = false
export const registerCorePlugins = () => {
    if (!registered) {
        /** Linking plugins */

// Linking editor
        pluginRegistry.registerTaskView("linking", {
            id: "linkingEditor",
            label: "Linking editor",
            render(projectId: string, taskId: string, viewActions: IViewActions | undefined): JSX.Element {
                return <LinkingRuleEditor projectId={projectId} linkingTaskId={taskId} viewActions={viewActions}/>;
            },
        });

        /** Transform plugins */
        pluginRegistry.registerTaskView("transform", {
            id: "hierarchicalMappingEditor",
            label: "Mapping editor",
            render(projectId: string, taskId: string): JSX.Element {
                return <HierarchicalMapping project={projectId} transformTask={taskId} initialRule={"root"} />;
            },
        });
    }
    registered = true
}
