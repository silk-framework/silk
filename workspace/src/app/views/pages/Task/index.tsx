import React, { useState } from "react";
import { useParams } from "react-router";
import { Section, Spacing, WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";
import { TaskActivityOverview } from "../../shared/TaskActivityOverview/TaskActivityOverview";
import { ProjectTaskParams } from "../../shared/typings";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { RelatedItemsMenu } from "../../shared/PageHeaderMenus/RelatedItemsMenu";
import { useTaskPluginDetails } from "../../shared/TaskConfig/useTaskPluginDetails";

export default function TaskPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [notFound, setNotFound] = useState(false);
    const [forbidden, setForbidden] = useState(false);
    const pluginDetails = useTaskPluginDetails(projectId, taskId);

    const { pageHeader, updateActionsMenu, updateType } = usePageHeader({
        type: DATA_TYPES.TASK,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    React.useEffect(() => {
        if (pluginDetails) {
            updateType(pluginDetails.taskType, pluginDetails.pluginId);
        } else {
            updateType(DATA_TYPES.TASK);
        }
    }, [pluginDetails]);

    // Must be referentially stable, since a change re-triggers the page header actions update.
    const headerMenus = React.useMemo(
        () => <RelatedItemsMenu projectId={projectId} taskId={taskId} />,
        [projectId, taskId],
    );

    if (forbidden) {
        return <ProjectForbiddenNotification />;
    } else if (notFound) {
        return <NotFound />;
    }
    return (
        <WorkspaceContent className="eccapp-di__task">
            {pageHeader}
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={DATA_TYPES.TASK}
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
                    <TaskConfig projectId={projectId} taskId={taskId} />
                    <Spacing />
                    <TaskActivityOverview projectId={projectId} taskId={taskId} />
                </Section>
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
