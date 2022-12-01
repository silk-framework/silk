import { commonSel } from "@ducks/common";
import { requestTaskData } from "@ducks/shared/requests";
import { IProjectTask } from "@ducks/shared/typings";
import { Section, Spacing, WorkspaceContent, WorkspaceMain, WorkspaceSide } from "@eccenca/gui-elements";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { useParams } from "react-router";

import { DATA_TYPES } from "../../../constants";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { DataPreviewProps } from "../../plugins/plugin.types";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import { Loading } from "../../shared/Loading/Loading";
import Metadata from "../../shared/Metadata";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { TaskActivityOverview } from "../../shared/TaskActivityOverview/TaskActivityOverview";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { ProjectTaskParams } from "../../shared/typings";
import NotFound from "../NotFound";

// The dataset plugins that should show the data preview automatically without user interaction.
const automaticallyPreviewedDatasets = ["json", "xml", "csv"];
// Datasets that should have no preview at all (e.g. because they are always empty)
const noDataPreviewDatasets = ["variableDataset"];

export function Dataset() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();
    const [mainViewLoading, setMainViewLoading] = useState(true);
    const [taskData, setTaskData] = useState<IProjectTask | null>(null);
    const [notFound, setNotFound] = useState(false);
    const { dmBaseUrl } = useSelector(commonSel.initialSettingsSelector);

    const fetchDatasetTaskData = async () => {
        setMainViewLoading(true);
        try {
            const projectTask = await requestTaskData(projectId, taskId, true);
            if (projectTask.data.data.type) {
                setTaskData(projectTask.data);
            }
        } catch (ex) {
            registerError("Dataset-fetchDatasetTaskData", "Error fetching dataset information.", ex);
        } finally {
            setMainViewLoading(false);
        }
    };

    const pluginId = taskData?.data?.type;

    const showPreviewAutomatically = automaticallyPreviewedDatasets.includes(taskData?.data?.type ?? "");
    const showPreview = !noDataPreviewDatasets.includes(taskData?.data?.type ?? "");
    const DataPreviewComponent = pluginRegistry.pluginReactComponent<DataPreviewProps>(SUPPORTED_PLUGINS.DATA_PREVIEW);

    useEffect(() => {
        if (taskId && projectId) {
            fetchDatasetTaskData();
        }
    }, [taskId, projectId]);

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

    const { pageHeader, updateType, updateActionsMenu } = usePageHeader({
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
        alternateDepiction: "artefact-dataset",
    });

    useEffect(() => {
        if (!!pluginId) {
            updateType(DATA_TYPES.DATASET + "-" + pluginId);
        } else {
            updateType(DATA_TYPES.DATASET);
        }
    }, [pluginId]);

    return notFound ? (
        <NotFound />
    ) : (
        <WorkspaceContent className="eccapp-di__dataset">
            {pageHeader}
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={DATA_TYPES.DATASET}
                updateActionsMenu={updateActionsMenu}
                notFoundCallback={setNotFound}
            />
            <WorkspaceMain>
                <Section>
                    <Metadata />
                    <Spacing />
                    {mainViewLoading ? (
                        <Loading />
                    ) : (
                        // Show explore and query tab for knowledge graph dataset
                        additionalContent()
                    )}
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <RelatedItems />
                    <Spacing />
                    <TaskConfig projectId={projectId} taskId={taskId} />
                    <Spacing />
                    <TaskActivityOverview projectId={projectId} taskId={taskId} />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
