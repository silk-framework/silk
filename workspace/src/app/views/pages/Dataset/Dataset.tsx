import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { useParams } from "react-router";
import { useTranslation } from "react-i18next";
import { Section, Spacing, WorkspaceContent, WorkspaceMain, WorkspaceSide } from "gui-elements";
import { commonSel } from "@ducks/common";
import { requestTaskData } from "@ducks/shared/requests";
import { IProjectTask } from "@ducks/shared/typings";
import { DATA_TYPES } from "../../../constants";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { Loading } from "../../shared/Loading/Loading";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import Metadata from "../../shared/Metadata";
import NotFound from "../NotFound";
import useErrorHandler from "../../../hooks/useErrorHandler";
import {ProjectTaskParams} from "../../shared/typings";
import { TaskActivityOverview } from "../../shared/TaskActivityOverview/TaskActivityOverview";
import { SUPPORTED_PLUGINS, pluginRegistry } from "../../plugins/PluginRegistry";

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
            if (projectTask?.data?.type) {
                setTaskData(projectTask);
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
    const dataPreviewComponent = pluginRegistry.pluginComponent(SUPPORTED_PLUGINS.DATA_PREVIEW);

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
                dataPreviewComponent && (
                    <dataPreviewComponent.Component
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
