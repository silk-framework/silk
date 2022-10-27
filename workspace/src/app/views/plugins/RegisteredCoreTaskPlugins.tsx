import { LinkingRuleEditor, LinkingRuleEditorOptionalContext } from "../taskViews/linking/LinkingRuleEditor";
import React from "react";
import { IViewActions, pluginRegistry } from "./PluginRegistry";
import HierarchicalMapping from "../pages/MappingEditor/HierarchicalMapping/HierarchicalMapping.jsx";
import LinkingExecutionTab from "../../views/taskViews/linking/editorTabsComponents/LinkingExecutionTab";
import TransformExecutionTab from "../../views/taskViews/transform/editorTabsComponents/TransformExecutionTab";

let registered = false;
export const registerCorePlugins = () => {
    if (!registered) {
        /** Linking plugins */

        // Linking editor
        pluginRegistry.registerTaskView("linking", {
            id: "linkingEditor",
            label: "Linking editor",
            render(projectId: string, taskId: string, viewActions: IViewActions | undefined): JSX.Element {
                return (
                    <LinkingRuleEditorOptionalContext.Provider
                        value={{
                            initialFitToViewZoomLevel: 0.75,
                        }}
                    >
                        <LinkingRuleEditor
                            projectId={projectId}
                            linkingTaskId={taskId}
                            viewActions={viewActions}
                            instanceId={"tab-instance"}
                        />
                    </LinkingRuleEditorOptionalContext.Provider>
                );
            },
        });

        pluginRegistry.registerTaskView("linking", {
            id: "LinkingExecution",
            label: "Linking execution",
            render(projectId: string, taskId: string, viewActions: IViewActions | undefined): JSX.Element {
                return <LinkingExecutionTab taskId={taskId} projectId={projectId} />;
            },
        });

        pluginRegistry.registerTaskView("transform", {
            id: "TransformExecution",
            label: "Transform execution",
            render(projectId: string, taskId: string, viewActions: IViewActions | undefined): JSX.Element {
                return <TransformExecutionTab taskId={taskId} projectId={projectId} />;
            },
        });

        /** Transform plugins. FIXME: CMEM-4266: Find solution for opening mapping rules in the rule editor without redirecting. */
        // Hierarchical mapping editor
        pluginRegistry.registerTaskView("transform", {
            id: "hierarchicalMappingEditor",
            label: "Mapping editor",
            render(projectId: string, taskId: string, _: IViewActions, startFullScreen: boolean): JSX.Element {
                return (
                    <HierarchicalMapping project={projectId} transformTask={taskId} startFullScreen={startFullScreen} />
                );
            },
        });

        // Mapping evaluation // FIXME: Does not render well when not in i-frame
        // pluginRegistry.registerTaskView("transform", {
        //     id: "hierarchicalMappingEvaluation",
        //     label: "Mapping evaluation",
        //     render(projectId: string, taskId: string): JSX.Element {
        //         return <EvaluateMapping project={projectId} transformTask={taskId} initialRule={"root"}  limit={50} offset={0}/>;
        //     },
        // });
    }
    registered = true;
};
