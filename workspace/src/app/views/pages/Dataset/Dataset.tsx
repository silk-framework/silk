import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { useParams } from "react-router";
import { useTranslation } from "react-i18next";
import { WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { commonSel } from "@ducks/common";
import { DATA_TYPES } from "../../../constants";
import { Loading } from "../../shared/Loading/Loading";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import Metadata from "../../shared/Metadata";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { TaskActivityOverview } from "../../shared/TaskActivityOverview/TaskActivityOverview";
import NotFound from "../NotFound";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";
import { ProjectTaskParams } from "../../shared/typings";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";
import { DataPreviewProps } from "../../plugins/plugin.types";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { IPluginDetails } from "@ducks/common/typings";
import { GridBoard, GridBoardItem, GridTileCard } from "../../shared/GridBoard";

// The dataset plugins that should show the data preview automatically without user interaction.
const automaticallyPreviewedDatasets = ["json", "xml", "csv"];
// Datasets that should have no preview at all (e.g. because they are always empty)
const noDataPreviewDatasets = ["variableDataset"];

export function Dataset() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [t] = useTranslation();
    // Shared by the always-mounted TaskConfig tile, so the task data is only fetched once.
    const [pluginDetails, setPluginDetails] = useState<IPluginDetails | undefined>(undefined);
    const [notFound, setNotFound] = useState(false);
    const [forbidden, setForbidden] = useState(false);
    const { dmBaseUrl } = useSelector(commonSel.initialSettingsSelector);

    const pluginId = pluginDetails?.pluginId;

    const showPreviewAutomatically = automaticallyPreviewedDatasets.includes(pluginId ?? "");
    const showPreview = !noDataPreviewDatasets.includes(pluginId ?? "");
    const DataPreviewComponent = pluginRegistry.pluginReactComponent<DataPreviewProps>(SUPPORTED_PLUGINS.DATA_PREVIEW);

    useEffect(() => {
        if (pluginDetails) {
            updateType(pluginDetails.taskType, pluginDetails.pluginId);
        } else {
            updateType(DATA_TYPES.DATASET);
        }
    }, [pluginDetails]);

    const additionalContent = () => {
        if (pluginId === "eccencaDataPlatform") {
            return dmBaseUrl && <ProjectTaskTabView iFrameName={"detail-page-iframe"} />;
        } else {
            return (
                showPreview &&
                DataPreviewComponent && (
                    <DataPreviewComponent.Component
                        id={"datasetPageDataPreview"}
                        title={t("pages.dataset.title", "Data preview")}
                        preview={{ project: projectId, dataset: taskId }}
                        autoLoad={showPreviewAutomatically}
                    />
                )
            );
        }
    };

    const { pageHeader, updateType } = usePageHeader({
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
        alternateDepiction: "artefact-dataset",
    });

    // Main content = data preview (or the knowledge-graph explore/query iframe); may be absent.
    const mainContent = !pluginDetails ? <Loading /> : additionalContent();

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
                    itemType={DATA_TYPES.DATASET}
                    notFoundCallback={setNotFound}
                    forbiddenCallback={setForbidden}
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
            element: <TaskConfig projectId={projectId} taskId={taskId} pluginDataCallback={setPluginDetails} />,
        },
        {
            id: "activity",
            icon: "application-activities",
            title: t("widget.TaskActivityOverview.title", "Activities"),
            defaultLayout: { x: 8, y: 10, w: 4, h: 4 },
            element: <TaskActivityOverview projectId={projectId} taskId={taskId} />,
        },
    ];

    if (mainContent) {
        items.splice(1, 0, {
            id: "preview",
            icon: "item-viewdetails",
            title: t("pages.dataset.title", "Data preview"),
            defaultLayout: { x: 0, y: 3, w: 8, h: 11 },
            element: mainContent,
        });
    }

    return forbidden ? (
        <ProjectForbiddenNotification />
    ) : notFound ? (
        <NotFound />
    ) : (
        <WorkspaceContent className="eccapp-di__dataset">
            {pageHeader}
            <WorkspaceMain>
                <DeprecatedPluginsBanner projectId={projectId} taskId={taskId} />
                <GridBoard items={items} storageKey="dataset" />
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
