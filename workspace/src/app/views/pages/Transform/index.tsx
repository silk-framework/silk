import { Section, Spacing, WorkspaceContent, WorkspaceMain, WorkspaceSide } from "@eccenca/gui-elements";
import React, { useState } from "react";
import { useParams } from "react-router";

import { DATA_TYPES } from "../../../constants";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import Metadata from "../../shared/Metadata";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { TaskActivityOverview } from "../../shared/TaskActivityOverview/TaskActivityOverview";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { ProjectTaskParams } from "../../shared/typings";
import NotFound from "../NotFound";

export default function TransformPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [notFound, setNotFound] = useState(false);

    const { pageHeader, updateActionsMenu } = usePageHeader({
        type: DATA_TYPES.TRANSFORM,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    return notFound ? (
        <NotFound />
    ) : (
        <WorkspaceContent className="eccapp-di__transformation">
            {pageHeader}
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={DATA_TYPES.TRANSFORM}
                updateActionsMenu={updateActionsMenu}
                notFoundCallback={setNotFound}
            />
            <WorkspaceMain>
                <Section>
                    <Metadata />
                    <Spacing />
                    <ProjectTaskTabView
                        taskViewConfig={{ pluginId: "transform", projectId: projectId, taskId: taskId }}
                        iFrameName={"detail-page-iframe"}
                    />
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <RelatedItems projectId={projectId} taskId={taskId} />
                    <Spacing />
                    <TaskConfig projectId={projectId} taskId={taskId} />
                    <Spacing />
                    <TaskActivityOverview projectId={projectId} taskId={taskId} />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
