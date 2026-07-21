import React, { useState } from "react";
import { useParams } from "react-router";
import { WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import VariablesWidget from "../../shared/VariablesWidget/VariablesWidget";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";
import { ProjectTaskParams } from "../../shared/typings";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { GridBoard, GridBoardItem, GridTileCard } from "../../shared/GridBoard";

export default function WorkflowPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [notFound, setNotFound] = useState(false);
    const [forbidden, setForbidden] = useState(false);
    const [t] = useTranslation();

    const { pageHeader } = usePageHeader({
        type: DATA_TYPES.WORKFLOW,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    // View action that should be triggered when a workflow is saved
    const onSave = () => {
        window.top?.postMessage(
            JSON.stringify({
                id: "workflowSaved",
                message: "Workflow updated",
            }),
            "*",
        );
    };

    const items: GridBoardItem[] = [
        {
            id: "summary",
            icon: "item-info",
            title: t("common.words.summary", "Summary"),
            defaultLayout: { x: 0, y: 0, w: 8, h: 3 },
            element: (
                <GridTileCard title={t("common.words.summary", "Summary")} data-test-id="workflow-summary-tile">
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
                    itemType={DATA_TYPES.WORKFLOW}
                    notFoundCallback={setNotFound}
                    forbiddenCallback={setForbidden}
                />
            ),
        },
        {
            id: "editor",
            icon: "item-edit",
            title: t("widget.WorkflowEditor.title", "Workflow editor"),
            defaultLayout: { x: 0, y: 3, w: 8, h: 13 },
            // ProjectTaskTabView carries its own Card (with the active tab's title + tab bar), so it
            // is rendered bare — wrapping it in a GridTileCard would nest a card in a card and repeat
            // the "Workflow editor" title.
            element: (
                <ProjectTaskTabView
                    iFrameName={"detail-page-iframe"}
                    taskViewConfig={{ pluginId: "workflow", projectId, taskId }}
                    viewActions={{ onSave }}
                />
            ),
        },
        {
            id: "relatedItems",
            icon: "toggler-list",
            title: t("RelatedItems.title", "Related items"),
            defaultLayout: { x: 8, y: 0, w: 4, h: 5 },
            element: <RelatedItems messageEventReloadTrigger={(messageId) => messageId === "workflowSaved"} />,
        },
        {
            id: "variables",
            icon: "data-string",
            title: t("widget.VariableWidget.title", "Project Variables"),
            defaultLayout: { x: 8, y: 5, w: 4, h: 5 },
            element: <VariablesWidget projectId={projectId} taskId={taskId} />,
        },
    ];

    if (forbidden) {
        return <ProjectForbiddenNotification />;
    } else if (notFound) {
        return <NotFound />;
    }
    return (
        <WorkspaceContent className="eccapp-di__workflow">
            {pageHeader}
            <WorkspaceMain>
                <DeprecatedPluginsBanner projectId={projectId} taskId={taskId} />
                <GridBoard items={items} storageKey="workflow" />
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
