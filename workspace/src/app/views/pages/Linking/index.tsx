import React, { useState } from "react";
import { useParams } from "react-router";
import { useTranslation } from "react-i18next";
import { WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
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
import VariablesWidget from "../../shared/VariablesWidget/VariablesWidget";
import { LinkageRuleConfig } from "./config/LinkageRuleConfig";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";
import { GridBoard, GridBoardItem, GridTileCard } from "../../shared/GridBoard";

export default function LinkingPage() {
    const { projectId, taskId } = useParams<ProjectTaskParams>();
    const [t] = useTranslation();
    const [notFound, setNotFound] = useState(false);
    const [forbidden, setForbidden] = useState(false);

    const { pageHeader } = usePageHeader({
        type: DATA_TYPES.LINKING,
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
                    itemType={DATA_TYPES.LINKING}
                    notFoundCallback={setNotFound}
                    forbiddenCallback={setForbidden}
                />
            ),
        },
        {
            id: "editor",
            icon: "item-edit",
            title: t("pages.linking.title", "Linking editor"),
            defaultLayout: { x: 0, y: 3, w: 8, h: 16 },
            // ProjectTaskTabView carries its own Card (with the active tab's title + tab bar), so it
            // is rendered bare — wrapping it in a GridTileCard would nest a card in a card and repeat
            // the "Linking editor" title.
            element: (
                <ProjectTaskTabView
                    taskViewConfig={{ pluginId: "linking", projectId: projectId, taskId: taskId }}
                    iFrameName={"detail-page-iframe"}
                />
            ),
        },
        {
            id: "relatedItems",
            icon: "toggler-list",
            title: t("RelatedItems.title", "Related items"),
            defaultLayout: { x: 8, y: 0, w: 4, h: 5 },
            element: <RelatedItems />,
        },
        {
            id: "taskConfig",
            icon: "item-settings",
            title: t("widget.TaskConfigWidget.title", "Configuration"),
            defaultLayout: { x: 8, y: 5, w: 4, h: 5 },
            element: <TaskConfig projectId={projectId} taskId={taskId} />,
        },
        {
            id: "linkageRuleConfig",
            icon: "artefact-linking",
            title: t("widget.LinkingRuleConfigWidget.title", "Configuration: Linkage rule"),
            defaultLayout: { x: 8, y: 10, w: 4, h: 5 },
            element: <LinkageRuleConfig projectId={projectId} linkingTaskId={taskId} />,
        },
        {
            id: "activity",
            icon: "application-activities",
            title: t("widget.TaskActivityOverview.title", "Activities"),
            defaultLayout: { x: 8, y: 15, w: 4, h: 4 },
            element: <TaskActivityOverview projectId={projectId} taskId={taskId} />,
        },
        {
            id: "variables",
            icon: "data-string",
            title: t("widget.VariableWidget.title.execution", "Execution variables"),
            defaultLayout: { x: 8, y: 19, w: 4, h: 5 },
            element: <VariablesWidget projectId={projectId} taskId={taskId} />,
        },
    ];

    if (forbidden) {
        return <ProjectForbiddenNotification />;
    } else if (notFound) {
        return <NotFound />;
    }

    return (
        <WorkspaceContent className="eccapp-di__linking">
            {pageHeader}
            <WorkspaceMain>
                <DeprecatedPluginsBanner projectId={projectId} taskId={taskId} />
                <GridBoard items={items} storageKey="linking" />
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
