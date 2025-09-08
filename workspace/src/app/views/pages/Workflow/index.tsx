import React, { useState } from "react";
import { useParams } from "react-router";
import { Section, Spacing, WorkspaceContent, WorkspaceMain, WorkspaceSide } from "@eccenca/gui-elements";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { ProjectTaskParams } from "../../shared/typings";
import VariablesWidget from "../../../views/shared/VariablesWidget/VariablesWidget";

export default function WorkflowPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>() as ProjectTaskParams;
    const [notFound, setNotFound] = useState(false);

    const { pageHeader, updateActionsMenu } = usePageHeader({
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

    return notFound ? (
        <NotFound />
    ) : (
        <WorkspaceContent className="eccapp-di__workflow">
            {pageHeader}
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={DATA_TYPES.WORKFLOW}
                updateActionsMenu={updateActionsMenu}
                notFoundCallback={setNotFound}
            />
            <WorkspaceMain>
                <Section>
                    <Metadata />
                    <Spacing />
                    <ProjectTaskTabView
                        iFrameName={"detail-page-iframe"}
                        taskViewConfig={{ pluginId: "workflow", projectId, taskId }}
                        viewActions={{
                            onSave,
                        }}
                    />
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <RelatedItems messageEventReloadTrigger={(messageId) => messageId === "workflowSaved"} />
                    <Spacing />
                    <VariablesWidget projectId={projectId} taskId={taskId} />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
