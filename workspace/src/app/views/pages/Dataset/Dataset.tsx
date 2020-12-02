import React, { useEffect, useState } from "react";

import { useSelector } from "react-redux";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@gui-elements/blueprint/constants";
import { useParams } from "react-router";
import Metadata from "../../shared/Metadata";
import { datasetSel } from "@ducks/dataset";

import { Section, Spacing, WorkspaceContent, WorkspaceMain, WorkspaceSide } from "@gui-elements/index";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { DataPreview } from "../../shared/DataPreview/DataPreview";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { useTranslation } from "react-i18next";
import { LegacyWindow } from "../../shared/LegacyWindow/LegacyWindow";
import { Loading } from "../../shared/Loading/Loading";
import { requestTaskData } from "@ducks/shared/requests";
import { IProjectTask } from "@ducks/shared/typings";

// The dataset plugins that should show the data preview automatically without user interaction.
const automaticallyPreviewedDatasets = ["json", "xml", "csv"];

export function Dataset() {
    const error = useSelector(datasetSel.errorSelector);
    const { taskId, projectId } = useParams();
    const [t] = useTranslation();
    const [mainViewLoading, setMainViewLoading] = useState(true);
    const [taskData, setTaskData] = useState<IProjectTask | null>(null);

    useEffect(() => {
        if (error?.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 0,
            });
        }
    }, [error.detail]);

    const fetchDatasetTaskData = async () => {
        setMainViewLoading(true);
        try {
            const projectTask = await requestTaskData(projectId, taskId, true);
            if (projectTask?.data?.type) {
                setTaskData(projectTask);
            }
        } finally {
            setMainViewLoading(false);
        }
    };

    const pluginId = taskData?.data?.type;

    const showPreviewAutomatically = automaticallyPreviewedDatasets.includes(taskData?.data?.type);

    useEffect(() => {
        if (taskId && projectId) {
            fetchDatasetTaskData();
        }
    }, [taskId, projectId]);

    return (
        <WorkspaceContent className="eccapp-di__dataset">
            <WorkspaceMain>
                <Section>
                    <Metadata />
                    <Spacing />
                    {mainViewLoading ? (
                        <Loading />
                    ) : // Show explore and query tab for knowledge graph dataset
                    pluginId !== "eccencaDataPlatform" ? (
                        <DataPreview
                            title={t("pages.dataset.title", "Data preview")}
                            preview={{ project: projectId, dataset: taskId }}
                            autoLoad={showPreviewAutomatically}
                        />
                    ) : (
                        <LegacyWindow />
                    )}
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <RelatedItems />
                    <Spacing />
                    <TaskConfig projectId={projectId} taskId={taskId} />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
