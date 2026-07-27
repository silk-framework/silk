import React from "react";
import { useParams } from "react-router";
import { useTranslation } from "react-i18next";
import { WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { DATA_TYPES } from "../../../constants";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ProjectTaskParams } from "../../shared/typings";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { GridBoard } from "../../shared/GridBoard";
import { buildLinkingTiles } from "../../shared/GridBoard/taskPageTiles";
import { useTaskPageGuards } from "../../shared/GridBoard/useTaskPageGuards";

export default function LinkingPage() {
    const { projectId, taskId } = useParams<ProjectTaskParams>();
    const [t] = useTranslation();
    const { guardElement, notFoundCallback, forbiddenCallback } = useTaskPageGuards();

    const { pageHeader } = usePageHeader({
        type: DATA_TYPES.LINKING,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    const items = buildLinkingTiles({ t, projectId, taskId, notFoundCallback, forbiddenCallback });

    if (guardElement) {
        return guardElement;
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
