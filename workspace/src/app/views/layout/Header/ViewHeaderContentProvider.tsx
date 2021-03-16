import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { useLocation } from "react-router";
import { IPageLabels } from "@ducks/router/operations";
import { commonSel } from "@ducks/common";
import { requestTaskItemInfo } from "@ducks/shared/requests";
import { DATA_TYPES } from "../../../constants";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";

export function ViewHeaderContentProvider() {
    const location = useLocation<any>();
    const taskId = useSelector(commonSel.currentTaskIdSelector);
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const [itemType, setItemType] = useState<string | null>(null);

    // Update task type
    useEffect(() => {
        let itemType = "unknown";
        if (location.state?.pageLabels?.itemType) {
            itemType = location.state.pageLabels.itemType;
        } else {
            if (projectId && !taskId) {
                itemType = DATA_TYPES.PROJECT;
            } else if (projectId && taskId) {
                if (!location.state?.pageLabels) {
                    location.state = { ...location.state };
                    location.state.pageLabels = {};
                }
                updateItemType(location.state.pageLabels, location.pathname);
            }
        }
        setItemType(itemType);
    }, [projectId, taskId]);

    const updateItemType = async (pageLabels: IPageLabels, locationPathName: string) => {
        if (projectId && taskId) {
            try {
                const response = await requestTaskItemInfo(projectId, taskId);
                const itemType = response.data.itemType.id;
                if (window.location.pathname === locationPathName) {
                    setItemType(itemType);
                }
            } catch (ex) {
                // Swallow exception, nothing we can do
            }
        }
    };

    const { pageHeader, updateType, updateActionsMenu } = usePageHeader({
        alternateDepiction: "application-homepage",
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    useEffect(() => {
        updateType(itemType);
    }, [itemType]);

    return (
        <>
            {pageHeader}
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={itemType}
                updateActionsMenu={updateActionsMenu}
            />
        </>
    );
}
