import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { useParams } from "react-router";
import { useTranslation } from "react-i18next";
import { Section, Spacing, WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { commonSel } from "@ducks/common";
import { DATA_TYPES } from "../../../constants";
import { Loading } from "../../shared/Loading/Loading";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import Metadata from "../../shared/Metadata";
import NotFound from "../NotFound";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";
import { ProjectTaskParams } from "../../shared/typings";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";
import { DataPreviewProps } from "../../plugins/plugin.types";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { RelatedItemsMenu } from "../../shared/PageHeaderMenus/RelatedItemsMenu";
import { TaskActivitiesMenu } from "../../shared/PageHeaderMenus/TaskActivitiesMenu";
import { useTaskPluginDetails } from "../../shared/TaskConfig/useTaskPluginDetails";

// The dataset plugins that should show the data preview automatically without user interaction.
const automaticallyPreviewedDatasets = ["json", "xml", "csv"];
// Datasets that should have no preview at all (e.g. because they are always empty)
const noDataPreviewDatasets = ["variableDataset"];

export function Dataset() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [t] = useTranslation();
    const pluginDetails = useTaskPluginDetails(projectId, taskId);
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

    const { pageHeader, updateType, updateActionsMenu } = usePageHeader({
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
        alternateDepiction: "artefact-dataset",
    });

    // Must be referentially stable, since a change re-triggers the page header actions update.
    const headerMenus = React.useMemo(
        () => (
            <>
                <RelatedItemsMenu />
                <TaskActivitiesMenu projectId={projectId} taskId={taskId} />
            </>
        ),
        [projectId, taskId],
    );

    return forbidden ? (
        <ProjectForbiddenNotification />
    ) : notFound ? (
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
                forbiddenCallback={setForbidden}
                headerMenus={headerMenus}
            />
            <WorkspaceMain>
                <Section>
                    <DeprecatedPluginsBanner projectId={projectId} taskId={taskId} />
                    <Metadata />
                    <Spacing />
                    {!pluginDetails ? (
                        <Loading />
                    ) : (
                        // Show explore and query tab for knowledge graph dataset
                        additionalContent()
                    )}
                </Section>
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
