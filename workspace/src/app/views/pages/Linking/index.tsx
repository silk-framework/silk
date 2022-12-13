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
import { LinkageRuleConfig } from "./config/LinkageRuleConfig";

export default function LinkingPage() {
    const { projectId, taskId } = useParams<ProjectTaskParams>();
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
                    <ProjectTaskTabView
                        taskViewConfig={{ pluginId: "linking", projectId: projectId, taskId: taskId }}
                        iFrameName={"detail-page-iframe"}
                    />
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <RelatedItems />
                    <Spacing />
                    <TaskConfig projectId={projectId} taskId={taskId} />
                    <Spacing />
                    <LinkageRuleConfig projectId={projectId} linkingTaskId={taskId} />
                    <Spacing />
                    <TaskActivityOverview projectId={projectId} taskId={taskId} />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
