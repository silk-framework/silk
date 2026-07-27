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
import { ProjectTaskParams } from "../../shared/typings";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";
import { DataPreviewProps } from "../../plugins/plugin.types";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { IPluginDetails } from "@ducks/common/typings";
import { GridBoard } from "../../shared/GridBoard";
import { buildDatasetTiles } from "../../shared/GridBoard/taskPageTiles";
import { useTaskPageGuards } from "../../shared/GridBoard/useTaskPageGuards";

// The dataset plugins that should show the data preview automatically without user interaction.
const automaticallyPreviewedDatasets = ["json", "xml", "csv"];
// Datasets that should have no preview at all (e.g. because they are always empty)
const noDataPreviewDatasets = ["variableDataset"];

export function Dataset() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [t] = useTranslation();
    // Shared by the always-mounted TaskConfig tile, so the task data is only fetched once.
    const [pluginDetails, setPluginDetails] = useState<IPluginDetails | undefined>(undefined);
    const { guardElement, notFoundCallback, forbiddenCallback } = useTaskPageGuards();
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

    const items = buildDatasetTiles({
        t,
        projectId,
        taskId,
        notFoundCallback,
        forbiddenCallback,
        pluginDataCallback: setPluginDetails,
        mainContent,
    });

    if (guardElement) {
        return guardElement;
    }
    return (
        <WorkspaceContent className="eccapp-di__dataset">
            {pageHeader}
            <WorkspaceMain>
                <DeprecatedPluginsBanner projectId={projectId} taskId={taskId} />
                <GridBoard items={items} storageKey="dataset" />
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
