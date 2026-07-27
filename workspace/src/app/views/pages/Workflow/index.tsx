import React from "react";
import { useParams } from "react-router";
import { WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { DATA_TYPES } from "../../../constants";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ProjectTaskParams } from "../../shared/typings";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { GridBoard } from "../../shared/GridBoard";
import { buildWorkflowTiles } from "../../shared/GridBoard/taskPageTiles";
import { useTaskPageGuards } from "../../shared/GridBoard/useTaskPageGuards";

export default function WorkflowPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [t] = useTranslation();
    const { guardElement, notFoundCallback, forbiddenCallback } = useTaskPageGuards();

    const { pageHeader } = usePageHeader({
        type: DATA_TYPES.WORKFLOW,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    // View action that should be triggered when a workflow is saved
    const onSave = () => {
        window.top?.postMessage(
            JSON.stringify({
                id: "workflowSaved",
                message: "Workflow updated",
            }),
            "*",
        );
    };

    const items = buildWorkflowTiles({
        t,
        projectId,
        taskId,
        notFoundCallback,
        forbiddenCallback,
        onSave,
        messageEventReloadTrigger: (messageId) => messageId === "workflowSaved",
    });

    if (guardElement) {
        return guardElement;
    }
    return (
        <WorkspaceContent className="eccapp-di__workflow">
            {pageHeader}
            <WorkspaceMain>
                <DeprecatedPluginsBanner projectId={projectId} taskId={taskId} />
                <GridBoard items={items} storageKey="workflow" />
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
