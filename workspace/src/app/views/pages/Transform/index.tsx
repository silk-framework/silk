import React, { useState } from "react";
import { useParams } from "react-router";
import { Section, Spacing, WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { ProjectTaskParams } from "../../shared/typings";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";
import MappingCreatorBanner from "./MappingCreatorBanner";
import { RelatedItemsMenu } from "../../shared/PageHeaderMenus/RelatedItemsMenu";
import { TaskActivitiesMenu } from "../../shared/PageHeaderMenus/TaskActivitiesMenu";

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

    // Must be referentially stable, since a change re-triggers the page header actions update.
    const headerMenus = React.useMemo(
        () => (
            <>
                <RelatedItemsMenu projectId={projectId} taskId={taskId} />
                <TaskActivitiesMenu projectId={projectId} taskId={taskId} />
            </>
        ),
        [projectId, taskId],
    );

    if (forbidden) {
        return <ProjectForbiddenNotification />;
    } else if (notFound) {
        return <NotFound />;
    }
    return (
        <WorkspaceContent className="eccapp-di__transformation">
            {pageHeader}
            <MappingCreatorBanner projectId={projectId} taskId={taskId} />
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={DATA_TYPES.TRANSFORM}
                updateActionsMenu={updateActionsMenu}
                notFoundCallback={setNotFound}
                forbiddenCallback={setForbidden}
                headerMenus={headerMenus}
            />
            <WorkspaceMain>
                <Section>
                    <DeprecatedPluginsBanner projectId={projectId} taskId={taskId} />
                    <Metadata />
                    <Spacing />
                    <ProjectTaskTabView
                        taskViewConfig={{ pluginId: "transform", projectId: projectId, taskId: taskId }}
                        iFrameName={"detail-page-iframe"}
                        viewActions={{ addLocalBreadcrumbs: updateBreadcrumbsExtensions }}
                    />
                </Section>
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
