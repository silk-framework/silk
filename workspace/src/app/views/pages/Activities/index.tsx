import { commonOp } from "@ducks/common";
import { routerSel } from "@ducks/router";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { previewSlice } from "@ducks/workspace/previewSlice";
import {
    Button,
    Divider,
    Grid,
    GridColumn,
    GridRow,
    IconButton,
    Notification,
    Section,
    SectionHeader,
    TitleMainsection,
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { batch, useDispatch, useSelector } from "react-redux";
import { useHistory, useParams } from "react-router";
import { ProjectTaskParams } from "views/shared/typings";

import { SERVE_PATH } from "../../../constants/path";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import SearchBar from "../../shared/SearchBar";
import NotFound from "../NotFound";
import Filterbar from "../Workspace/Filterbar";
import utils from "./ActivitiesUtils";
import ActivityList from "./ActivityList";

const Activities = () => {
    const dispatch = useDispatch();
    const { registerError } = useErrorHandler();
    const history = useHistory();
    const error = useSelector(workspaceSel.errorSelector);
    const qs = useSelector(routerSel.routerSearchSelector);
    const path = useSelector(routerSel.pathnameSelector);
    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);
    const { clearSearchResults } = previewSlice.actions;

    const [t] = useTranslation();

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
                // Reset the filters, due to redirecting
                dispatch(workspaceOp.resetFilters());

                // Setup the filters from query string
                dispatch(workspaceOp.setupFiltersFromQs(qs));
                projectId && dispatch(commonOp.setProjectId(projectId));
                // Fetch the list of projects
                dispatch(workspaceOp.fetchListAsync(utils.searchActivities));
            });
        }
        return () => {
            dispatch(clearSearchResults());
        };
    }, [qs]);

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
                                <GridColumn full>
                                    <div style={{ display: "flex", alignItems: "center" }}>
                                        <div style={{ width: "100%" }}>
                                            <SearchBar
                                                focusOnCreation
                                                textQuery={textQuery}
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
                            <GridColumn full>
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
