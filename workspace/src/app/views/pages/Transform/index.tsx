import React, { useState } from "react";
import { useParams } from "react-router";
import { WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { TaskActivityOverview } from "../../shared/TaskActivityOverview/TaskActivityOverview";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { ProjectTaskParams } from "../../shared/typings";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";
import MappingCreatorBanner from "./MappingCreatorBanner";
import { GridBoard, GridBoardItem, GridTileCard } from "../../shared/GridBoard";

export default function TransformPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [notFound, setNotFound] = useState(false);
    const [forbidden, setForbidden] = useState(false);
    const [t] = useTranslation();

    const { pageHeader, updateBreadcrumbsExtensions } = usePageHeader({
        type: DATA_TYPES.TRANSFORM,
        breadcrumbsExtensions: [],
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

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
                    itemType={DATA_TYPES.TRANSFORM}
                    notFoundCallback={setNotFound}
                    forbiddenCallback={setForbidden}
                />
            ),
        },
        {
            id: "editor",
            icon: "item-edit",
            title: t("pages.transform.title", "Mapping editor"),
            defaultLayout: { x: 0, y: 3, w: 8, h: 13 },
            // ProjectTaskTabView carries its own Card (with the active tab's title + tab bar), so it
            // is rendered bare — wrapping it in a GridTileCard would nest a card in a card and repeat
            // the "Mapping editor" title.
            element: (
                <ProjectTaskTabView
                    taskViewConfig={{ pluginId: "transform", projectId: projectId, taskId: taskId }}
                    iFrameName={"detail-page-iframe"}
                    viewActions={{ addLocalBreadcrumbs: updateBreadcrumbsExtensions }}
                />
            ),
        },
        {
            id: "relatedItems",
            icon: "toggler-list",
            title: t("RelatedItems.title", "Related items"),
            defaultLayout: { x: 8, y: 0, w: 4, h: 5 },
            element: <RelatedItems projectId={projectId} taskId={taskId} />,
        },
        {
            id: "taskConfig",
            icon: "item-settings",
            title: t("widget.TaskConfigWidget.title", "Configuration"),
            defaultLayout: { x: 8, y: 5, w: 4, h: 5 },
            element: <TaskConfig projectId={projectId} taskId={taskId} />,
        },
        {
            id: "activity",
            icon: "application-activities",
            title: t("widget.TaskActivityOverview.title", "Activities"),
            defaultLayout: { x: 8, y: 10, w: 4, h: 4 },
            element: <TaskActivityOverview projectId={projectId} taskId={taskId} />,
        },
    ];

    if (forbidden) {
        return <ProjectForbiddenNotification />;
    } else if (notFound) {
        return <NotFound />;
    }
    return (
        <WorkspaceContent className="eccapp-di__transformation">
            {pageHeader}
            <MappingCreatorBanner projectId={projectId} taskId={taskId} />
            <WorkspaceMain>
                <DeprecatedPluginsBanner projectId={projectId} taskId={taskId} />
                <GridBoard items={items} storageKey="transform" />
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
