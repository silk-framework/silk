import { useTranslation } from "react-i18next";
import React from "react";
import { batch, useDispatch, useSelector } from "react-redux";
import {
    WorkspaceContent,
    WorkspaceMain,
    Section,
    SectionHeader,
    Grid,
    GridRow,
    GridColumn,
    TitleMainsection,
    Divider,
    WorkspaceSide,
    Notification,
    Button,
    IconButton,
} from "@eccenca/gui-elements";
import SearchBar from "../../shared/SearchBar";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import NotFound from "../NotFound";
import Filterbar from "../Workspace/Filterbar";
import utils from "./ActivitiesUtils";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { commonOp } from "@ducks/common";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { routerSel } from "@ducks/router";
import ActivityList from "./ActivityList";
import { useHistory, useParams } from "react-router";
import { SERVE_PATH } from "../../../constants/path";
import { ProjectTaskParams } from "views/shared/typings";
import { previewSlice } from "@ducks/workspace/previewSlice";

const Activities = () => {
    const dispatch = useDispatch();
    const { registerError } = useErrorHandler();
    const history = useHistory();
    const error = useSelector(workspaceSel.errorSelector);
    const path = useSelector(routerSel.pathnameSelector);
    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const pagination = useSelector(workspaceSel.paginationSelector);
    const sorter = useSelector(workspaceSel.sortersSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);
    const { clearSearchResults } = previewSlice.actions;

    const [t] = useTranslation();

    // FIXME: Workaround to prevent search with a text query from another page sharing the same Redux state. Needs refactoring.
    const [searchInitialized, setSearchInitialized] = React.useState(false);
    const effectiveSearchQuery = searchInitialized ? textQuery : "";

    React.useEffect(() => {
        setSearchInitialized(true);
    }, []);

    const breadcrumbs = [
        {
            text: t("navigation.side.diBrowse"),
            href: SERVE_PATH,
        },
        {
            text: t("pages.activities.title"),
            current: true,
        },
    ];

    const { pageHeader, updatePageHeader } = usePageHeader({
        alternateDepiction: "application-activities",
        autogeneratePageTitle: true,
        breadcrumbs,
    });

    const { projectId } = useParams<Partial<ProjectTaskParams>>();

    React.useEffect(() => {
        if (projectId)
            utils.getProjectInfo(projectId).then((res) => {
                updatePageHeader({
                    breadcrumbs: [
                        breadcrumbs[0],
                        {
                            text: res.data.label,
                            href: `${SERVE_PATH}/projects/${projectId}`,
                        },
                        breadcrumbs[1],
                    ],
                    pageTitle: `${breadcrumbs[1].text}: ${res.data.label}`,
                });
            });
    }, [projectId]);

    React.useEffect(() => {
        if (error.detail) {
            registerError("activities-error", "An error has occurred during loading the page.", error);
        }
    }, [error.detail]);

    /**
     * Get available Datatypes
     */
    React.useEffect(() => {
        batch(() => {
            dispatch(workspaceOp.changeProjectsLimit(25));
            dispatch(commonOp.fetchAvailableDTypesAsync(projectId as string));
        });
    }, []);

    React.useEffect(() => {
        if (path.endsWith("activities")) {
            batch(() => {
                // Setup the filters from query string
                projectId && dispatch(commonOp.setProjectId(projectId));
                // Fetch the list of projects
                dispatch(workspaceOp.fetchListAsync(utils.searchActivities));
            });
        }
        return () => {
            dispatch(clearSearchResults());
        };
    }, [pagination.limit, sorter?.applied?.sortBy]);

    /** handle sorting */
    const handleSort = (sortBy: string) => {
        dispatch(workspaceOp.applySorterOp(sortBy));
    };

    /** handle search */
    const handleSearch = (textQuery: string) => {
        dispatch(workspaceOp.applyFiltersOp({ textQuery, limit: 25, project: projectId }));
    };

    return error.status === 404 ? (
        <NotFound />
    ) : (
        <WorkspaceContent>
            {pageHeader}
            <WorkspaceMain>
                <Section>
                    <SectionHeader>
                        <Grid>
                            <GridRow>
                                <GridColumn small verticalAlign="center">
                                    <TitleMainsection>{t("pages.activities.title", "Activities")}</TitleMainsection>
                                </GridColumn>
                                <GridColumn>
                                    <div style={{ display: "flex", alignItems: "center" }}>
                                        <div style={{ width: "100%" }}>
                                            <SearchBar
                                                focusOnCreation
                                                textQuery={effectiveSearchQuery}
                                                sorters={sorters}
                                                onSort={handleSort}
                                                onSearch={handleSearch}
                                            />
                                        </div>
                                        <IconButton
                                            name="item-reload"
                                            text="Reload activities"
                                            onClick={() => history.go(0)}
                                        />
                                    </div>
                                </GridColumn>
                            </GridRow>
                        </Grid>
                    </SectionHeader>
                    <Divider addSpacing="medium" />
                    <Grid>
                        <GridRow>
                            <GridColumn small>
                                <Filterbar
                                    extraItemTypeModifiers={[{ id: "global", label: "Global" }]}
                                    projectId={projectId}
                                />
                            </GridColumn>
                            <GridColumn>
                                {error.detail ? (
                                    <Notification
                                        danger={true}
                                        actions={
                                            <Button
                                                text={t("common.action.retry", "Retry")}
                                                onClick={() => {
                                                    window.location.reload();
                                                }}
                                            />
                                        }
                                    >
                                        <h3>{t("http.error.fetchNotResult", "Error, cannot fetch results.")}</h3>
                                        <p>{error.detail}</p>
                                    </Notification>
                                ) : (
                                    <ActivityList />
                                )}
                            </GridColumn>
                        </GridRow>
                    </Grid>
                </Section>
            </WorkspaceMain>
            <WorkspaceSide></WorkspaceSide>
        </WorkspaceContent>
    );
};

export default Activities;
