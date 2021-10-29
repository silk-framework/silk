import React, { useState } from "react";
import { useParams } from "react-router";
import { Section, Spacing, WorkspaceContent, WorkspaceMain, WorkspaceSide } from "@gui-elements/index";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { TaskActivityOverview } from "../../shared/TaskActivityOverview/TaskActivityOverview";

export default function () {
    const { taskId, projectId } = useParams();
    const [notFound, setNotFound] = useState(false);

    const { pageHeader, updateActionsMenu } = usePageHeader({
        type: DATA_TYPES.LINKING,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    return notFound ? (
        <NotFound />
    ) : (
        <WorkspaceContent className="eccapp-di__linking">
            {pageHeader}
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={DATA_TYPES.LINKING}
                updateActionsMenu={updateActionsMenu}
                notFoundCallback={setNotFound}
            />
            <WorkspaceMain>
                <Section>
                    <Metadata />
                    <Spacing />
                    <ProjectTaskTabView iFrameName={"detail-page-iframe"} />
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <RelatedItems />
                    <Spacing />
                    <TaskConfig projectId={projectId} taskId={taskId} />
                    <Spacing />
                    <TaskActivityOverview projectId={projectId} taskId={taskId} />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
