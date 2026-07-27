import React from "react";
import { useParams } from "react-router";
import { WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { DATA_TYPES } from "../../../constants";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ProjectTaskParams } from "../../shared/typings";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { GridBoard } from "../../shared/GridBoard";
import { buildTransformTiles } from "../../shared/GridBoard/taskPageTiles";
import { useTaskPageGuards } from "../../shared/GridBoard/useTaskPageGuards";

export default function TransformPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [t] = useTranslation();
    const { guardElement, notFoundCallback, forbiddenCallback } = useTaskPageGuards();

    const { pageHeader, updateBreadcrumbsExtensions } = usePageHeader({
        type: DATA_TYPES.TRANSFORM,
        breadcrumbsExtensions: [],
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    const items = buildTransformTiles({
        t,
        projectId,
        taskId,
        notFoundCallback,
        forbiddenCallback,
        updateBreadcrumbsExtensions,
    });

    if (guardElement) {
        return guardElement;
    }
    return (
        <WorkspaceContent className="eccapp-di__transformation">
            {pageHeader}
            <WorkspaceMain>
                <DeprecatedPluginsBanner projectId={projectId} taskId={taskId} />
                <GridBoard items={items} storageKey="transform" />
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
