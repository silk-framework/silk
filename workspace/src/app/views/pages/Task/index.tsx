import React, { useState } from "react";
import { useParams } from "react-router";
import { useTranslation } from "react-i18next";
import { WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { DATA_TYPES } from "../../../constants";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ProjectTaskParams } from "../../shared/typings";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { IPluginDetails } from "@ducks/common/typings";
import { GridBoard } from "../../shared/GridBoard";
import { buildTaskTiles } from "../../shared/GridBoard/taskPageTiles";
import { useTaskPageGuards } from "../../shared/GridBoard/useTaskPageGuards";

export default function TaskPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [t] = useTranslation();
    const { guardElement, notFoundCallback, forbiddenCallback } = useTaskPageGuards();
    // Shared by the always-mounted TaskConfig tile, so the task data is only fetched once.
    const [pluginDetails, setPluginDetails] = useState<IPluginDetails | undefined>(undefined);

    const { pageHeader, updateType } = usePageHeader({
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

    const items = buildTaskTiles({
        t,
        projectId,
        taskId,
        notFoundCallback,
        forbiddenCallback,
        pluginDataCallback: setPluginDetails,
    });

    if (guardElement) {
        return guardElement;
    }
    return (
        <WorkspaceContent className="eccapp-di__task">
            {pageHeader}
            <WorkspaceMain>
                <DeprecatedPluginsBanner projectId={projectId} taskId={taskId} />
                <GridBoard items={items} storageKey="task" />
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
