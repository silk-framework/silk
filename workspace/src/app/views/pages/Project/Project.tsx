import React, { useEffect, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { routerSel } from "@ducks/router";
import { commonOp, commonSel } from "@ducks/common";
import Loading from "../../shared/Loading";
import { DATA_TYPES } from "../../../constants";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import ConfigurationWidget from "./ProjectNamespacePrefixManagementWidget";
import { ProjectTaskLoadingErrors } from "./WarningWidget/ProjectTaskLoadingErrors";
import FileWidget from "./FileWidget";
import ProjectContents from "./ProjectContents";
import NotFound from "../NotFound";
import { previewSlice } from "@ducks/workspace/previewSlice";
import VariablesWidget from "../../../views/shared/VariablesWidget/VariablesWidget";
import { AppDispatch } from "store/configureStore";
import { GlobalTableContext } from "../../../GlobalContextsWrapper";
import { DeprecatedPluginsWidget } from "./DeprecatedPlugins/DeprecatedPluginsWidget";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";
import { ProjectAccessControlProps } from "../../plugins/plugin.types";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";
import { GridBoard, GridBoardItem } from "../../shared/GridBoard";
import { summaryTile, actionsTile } from "../../shared/GridBoard/taskPageTiles";

const Project = () => {
    const dispatch = useDispatch<AppDispatch>();

    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const currentSearchQuery = useRef<string>("");
    currentSearchQuery.current = textQuery;
    const error = useSelector(workspaceSel.errorSelector);
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const qs = useSelector(routerSel.routerSearchSelector);
    const { clearSearchResults } = previewSlice.actions;
    const [t] = useTranslation();
    const { clearErrors } = useErrorHandler();
    const accessForbidden = error?.status === 403;

    const projectAccessControl = pluginRegistry.pluginReactComponent<ProjectAccessControlProps>(
        SUPPORTED_PLUGINS.DI_PROJECT_ACL,
    );

    const { globalTableSettings } = React.useContext(GlobalTableContext);

    React.useEffect(() => {
        // Clear all errors from the queue, since they will only repeat what's being displayed in the notification
        if (accessForbidden) {
            clearErrors();
        }
    }, [accessForbidden, clearErrors]);

    /**
     * Get available Datatypes
     */
    useEffect(() => {
        dispatch(commonOp.fetchAvailableDTypesAsync(projectId));
    }, []);

    useEffect(() => {
        // Reset the filters, due to redirecting
        dispatch(workspaceOp.resetFilters());
    }, [window.location.pathname]);

    // The Project "Contents" tile has its own settings slot, separate from `/workbench`.
    const tableSettings = globalTableSettings["projectContents"];

    useEffect(() => {
        // Setup the filters from query string
        dispatch(workspaceOp.setupFiltersFromQs(qs));

        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync(tableSettings));
        return () => {
            dispatch(clearSearchResults());
        };
    }, [qs, projectId, tableSettings.sortBy, tableSettings.sortOrder, tableSettings.pageSize]);

    const handleSearch = (textQuery: string) => {
        dispatch(workspaceOp.applyFiltersOp({ textQuery }));
    };

    const { pageHeader } = usePageHeader({
        type: DATA_TYPES.PROJECT,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    if (accessForbidden) {
        return <ProjectForbiddenNotification detail={error.detail} />;
    } else if (error?.status === 404) {
        return <NotFound />;
    } else if (!projectId) {
        return <Loading posGlobal description={t("pages.project.loading", "Loading project data")} />;
    }

    const items: GridBoardItem[] = [
        summaryTile(t),
        actionsTile({ t, projectId, itemType: DATA_TYPES.PROJECT }),
        {
            id: "contents",
            title: t("pages.project.content", "Contents"),
            icon: "item-viewdetails",
            defaultLayout: { x: 0, y: 3, w: 8, h: 14 },
            element: <ProjectContents projectId={projectId} />,
        },
        {
            // Error log — collapses out of the grid automatically when there are no warnings.
            id: "warnings",
            title: t("widget.WarningWidget.title", "Error log"),
            icon: "artefact-errorlog",
            defaultLayout: { x: 8, y: 0, w: 4, h: 5 },
            element: <ProjectTaskLoadingErrors refreshProjectPage={() => handleSearch(currentSearchQuery.current)} />,
        },
        {
            id: "configuration",
            title: t("widget.ConfigWidget.title", "Configuration"),
            icon: "item-settings",
            defaultLayout: { x: 8, y: 5, w: 4, h: 5 },
            element: <ConfigurationWidget />,
        },
        {
            id: "variables",
            title: t("widget.VariableWidget.title.project", "Project variables"),
            icon: "data-string",
            defaultLayout: { x: 8, y: 10, w: 4, h: 5 },
            element: <VariablesWidget projectId={projectId} />,
        },
        {
            id: "files",
            title: t("widget.FileWidget.title", "Project files"),
            icon: "artefact-file",
            defaultLayout: { x: 8, y: 20, w: 4, h: 5 },
            element: <FileWidget />,
        },
        {
            // Collapses out of the grid automatically when there are no deprecated plugins.
            id: "deprecated",
            title: t("widget.DeprecatedPluginsWidget.title", "Deprecated plugins"),
            icon: "artefact-deprecated",
            defaultLayout: { x: 8, y: 25, w: 4, h: 5 },
            element: <DeprecatedPluginsWidget projectId={projectId} />,
        },
    ];

    if (projectAccessControl) {
        items.push({
            id: "accessControl",
            title: t("widget.AccessControlWidget.title", "Access control"),
            icon: "module-accesscontrol",
            defaultLayout: { x: 8, y: 15, w: 4, h: 5 },
            element: <projectAccessControl.Component projectId={projectId} />,
        });
    }

    return (
        <WorkspaceContent className="eccapp-di__project">
            {pageHeader}
            <WorkspaceMain>
                <GridBoard items={items} storageKey="project" />
            </WorkspaceMain>
        </WorkspaceContent>
    );
};

export default Project;
