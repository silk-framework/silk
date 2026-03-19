import React, { useState } from "react";
import { useParams } from "react-router";
import { Section, Spacing, WorkspaceContent, WorkspaceMain, WorkspaceSide } from "@eccenca/gui-elements";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { TaskActivityOverview } from "../../shared/TaskActivityOverview/TaskActivityOverview";
import { ProjectTaskParams } from "../../shared/typings";
import { DeprecatedPluginsWidget } from "../Project/DeprecatedPlugins/DeprecatedPluginsWidget";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";

export default function TransformPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [notFound, setNotFound] = useState(false);
    const [forbidden, setForbidden] = useState(false);

    const { pageHeader, updateActionsMenu, updateBreadcrumbsExtensions } = usePageHeader({
        type: DATA_TYPES.TRANSFORM,
        breadcrumbsExtensions: [],
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    if (forbidden) {
        return <ProjectForbiddenNotification />;
    } else if (notFound) {
        return <NotFound />;
    }
    return (
        <WorkspaceContent className="eccapp-di__transformation">
            {pageHeader}
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={DATA_TYPES.TRANSFORM}
                updateActionsMenu={updateActionsMenu}
                notFoundCallback={setNotFound}
                forbiddenCallback={setForbidden}
            />
            <WorkspaceMain>
                <Section>
                    <Metadata />
                    <Spacing />
                    <ProjectTaskTabView
                        taskViewConfig={{ pluginId: "transform", projectId: projectId, taskId: taskId }}
                        iFrameName={"detail-page-iframe"}
                        viewActions={{ addLocalBreadcrumbs: updateBreadcrumbsExtensions }}
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
                    <Spacing />
                    <DeprecatedPluginsWidget projectId={projectId} taskId={taskId} />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
