import React, { useState } from "react";
import { useParams } from "react-router";
import { Section, Spacing, WorkspaceContent, WorkspaceMain, WorkspaceSide } from "@eccenca/gui-elements";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { TaskActivityOverview } from "../../shared/TaskActivityOverview/TaskActivityOverview";
import { ProjectTaskParams } from "../../shared/typings";
import { IPluginDetails } from "@ducks/common/typings";

export default function TaskPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>() as ProjectTaskParams;
    const [notFound, setNotFound] = useState(false);
    const [pluginDetails, setPluginDetails] = React.useState<IPluginDetails | undefined>();

    const { pageHeader, updateActionsMenu, updateType } = usePageHeader({
        type: DATA_TYPES.TASK,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    const pluginDataCallback = React.useCallback((details: IPluginDetails) => {
        setPluginDetails(details);
    }, []);

    React.useEffect(() => {
        if (pluginDetails) {
            updateType(pluginDetails.taskType, pluginDetails.pluginId);
        } else {
            updateType(DATA_TYPES.TASK);
        }
    }, [pluginDetails]);

    return notFound ? (
        <NotFound />
    ) : (
        <WorkspaceContent className="eccapp-di__task">
            {pageHeader}
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={DATA_TYPES.TASK}
                updateActionsMenu={updateActionsMenu}
                notFoundCallback={setNotFound}
            />
            <WorkspaceMain>
                <Section>
                    <Metadata />
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <RelatedItems projectId={projectId} taskId={taskId} />
                    <Spacing />
                    <TaskConfig projectId={projectId} taskId={taskId} pluginDataCallback={pluginDataCallback} />
                    <Spacing />
                    <TaskActivityOverview projectId={projectId} taskId={taskId} />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
