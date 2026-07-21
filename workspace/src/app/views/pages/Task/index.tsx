import React, { useState } from "react";
import { useParams } from "react-router";
import { useTranslation } from "react-i18next";
import { WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";
import { TaskActivityOverview } from "../../shared/TaskActivityOverview/TaskActivityOverview";
import { ProjectTaskParams } from "../../shared/typings";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { IPluginDetails } from "@ducks/common/typings";
import { GridBoard, GridBoardItem, GridTileCard } from "../../shared/GridBoard";

export default function TaskPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [notFound, setNotFound] = useState(false);
    const [forbidden, setForbidden] = useState(false);
    const [t] = useTranslation();
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

    const items: GridBoardItem[] = [
        {
            id: "summary",
            icon: "item-info",
            title: t("common.words.summary", "Summary"),
            defaultLayout: { x: 0, y: 0, w: 8, h: 3 },
            element: (
                <GridTileCard title={t("common.words.summary", "Summary")}>
                    <Metadata />
                </GridTileCard>
            ),
        },
        {
            id: "actions",
            icon: "item-moremenu",
            title: t("common.words.actions", "Actions"),
            defaultLayout: { x: 8, y: 0, w: 4, h: 5 },
            element: (
                <ArtefactManagementOptions
                    projectId={projectId}
                    taskId={taskId}
                    itemType={DATA_TYPES.TASK}
                    notFoundCallback={setNotFound}
                    forbiddenCallback={setForbidden}
                />
            ),
        },
        {
            id: "taskConfig",
            icon: "item-settings",
            title: t("widget.TaskConfigWidget.title", "Configuration"),
            defaultLayout: { x: 0, y: 3, w: 8, h: 8 },
            element: <TaskConfig projectId={projectId} taskId={taskId} pluginDataCallback={setPluginDetails} />,
        },
        {
            id: "relatedItems",
            icon: "toggler-list",
            title: t("RelatedItems.title", "Related items"),
            defaultLayout: { x: 8, y: 0, w: 4, h: 5 },
            element: <RelatedItems projectId={projectId} taskId={taskId} />,
        },
        {
            id: "activity",
            icon: "application-activities",
            title: t("widget.TaskActivityOverview.title", "Activities"),
            defaultLayout: { x: 8, y: 5, w: 4, h: 6 },
            element: <TaskActivityOverview projectId={projectId} taskId={taskId} />,
        },
    ];

    if (forbidden) {
        return <ProjectForbiddenNotification />;
    } else if (notFound) {
        return <NotFound />;
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
