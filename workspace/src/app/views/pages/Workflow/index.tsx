import React, { useEffect, useState } from "react";
import { useParams } from "react-router";
import { useSelector } from "react-redux";
import { Intent } from "@gui-elements/blueprint/constants";
import { Section, Spacing, WorkspaceContent, WorkspaceMain, WorkspaceSide } from "@gui-elements/index";
import { datasetSel } from "@ducks/dataset";
import { AppToaster } from "../../../services/toaster";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { ProjectTaskParams } from "views/shared/typings";

export default function () {
    const error = useSelector(datasetSel.errorSelector);
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [notFound, setNotFound] = useState(false);

    useEffect(() => {
        if (error?.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 0,
            });
        }
    }, [error.detail]);

    const { pageHeader, updateActionsMenu } = usePageHeader({
        type: DATA_TYPES.WORKFLOW,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    // View action that should be triggered when a workflow is saved
    const onSave = () => {
        window.top.postMessage(
            JSON.stringify({
                id: "workflowSaved",
                message: "Workflow updated",
            }),
            "*"
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
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
