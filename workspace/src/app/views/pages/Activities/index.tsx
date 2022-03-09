import { useTranslation } from "react-i18next";
import React from "react";
import { useDispatch, useSelector } from "react-redux";
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
} from "gui-elements";
import SearchBar from "../../shared/SearchBar";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import NotFound from "../NotFound";
import Filterbar from "../Workspace/Filterbar";
import utils from "./ActivitiesUtils";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { commonOp, commonSel } from "@ducks/common";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { routerSel } from "@ducks/router";
import ActivityList from "./ActivityList";

const Activities = () => {
    const dispatch = useDispatch();
    const { registerError } = useErrorHandler();
    const error = useSelector(workspaceSel.errorSelector);
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const qs = useSelector(routerSel.routerSearchSelector);
    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);

    const [t] = useTranslation();
    const { pageHeader } = usePageHeader({
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
        alternateDepiction: "application-activities",
        pageTitle: "Activity overview",
    });

    React.useEffect(() => {
        if (error.detail) {
            registerError("activities-error", "An error has occurred during loading the page.", error);
        }
    }, [error.detail]);

    /**
     * Get available Datatypes
     */
    React.useEffect(() => {
        dispatch(commonOp.fetchAvailableDTypesAsync(projectId));
    }, []);

    React.useEffect(() => {
        // Reset the filters, due to redirecting
        dispatch(workspaceOp.resetFilters());

        // Setup the filters from query string
        dispatch(workspaceOp.setupFiltersFromQs(qs));
        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync(utils.searchActivities));
    }, [qs]);

    /** need websocket connection to get updates on activities */
    /** handle filtering using facets */
    /** handle pagination */

    /** handle sorting */
    const handleSort = (sortBy: string) => {
        dispatch(workspaceOp.applySorterOp(sortBy));
    };

    /** handle search */
    const handleSearch = (textQuery: string) => {
        dispatch(workspaceOp.applyFiltersOp({ textQuery }));
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
                                    <SearchBar
                                        focusOnCreation={true}
                                        textQuery={textQuery}
                                        sorters={sorters}
                                        onSort={handleSort}
                                        onSearch={handleSearch}
                                    />
                                </GridColumn>
                            </GridRow>
                        </Grid>
                    </SectionHeader>
                    <Divider addSpacing="medium" />
                    <Grid>
                        <GridRow>
                            <GridColumn small>
                                <Filterbar extraItemTypeModifiers={[{ id: "global", label: "Global" }]} />
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
            <WorkspaceSide>
                <Section></Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
};

export default Activities;
