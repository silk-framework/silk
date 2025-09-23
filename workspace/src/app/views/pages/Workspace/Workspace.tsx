import React, { useEffect } from "react";

import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import WorkspaceSearch from "./WorkspaceSearch";
import { Grid, GridColumn, GridRow } from "@eccenca/gui-elements";
import { EmptyWorkspace } from "./EmptyWorkspace/EmptyWorkspace";
import { commonOp, commonSel } from "@ducks/common";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { previewSlice } from "@ducks/workspace/previewSlice";
import { routerSel } from "@ducks/router";

export function Workspace() {
    const dispatch = useDispatch();
    const { registerError } = useErrorHandler();

    const error = useSelector(workspaceSel.errorSelector);
    const isEmptyWorkspace = useSelector(workspaceSel.isEmptyPageSelector);
    const pagination = useSelector(workspaceSel.paginationSelector);
    const qs = useSelector(routerSel.routerSearchSelector);
    const sorter = useSelector(workspaceSel.sortersSelector);
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const { clearSearchResults } = previewSlice.actions;

    useEffect(() => {
        if (error.detail) {
            registerError("workspace-page-error", "An error has occurred during loading the page.", error);
        }
    }, [error.detail]);

    /**
     * Get available Datatypes
     */
    useEffect(() => {
        dispatch(commonOp.fetchAvailableDTypesAsync(projectId));
    }, []);

    useEffect(() => {
        // Reset the filters, due to redirecting
        dispatch(workspaceOp.resetFilters());

        // Setup the filters from query string
        dispatch(workspaceOp.setupFiltersFromQs(qs));
        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync());
        return () => {
            dispatch(clearSearchResults());
        };
    }, [pagination.limit, sorter?.applied, qs]);

    return !isEmptyWorkspace ? (
        <WorkspaceSearch />
    ) : (
        <Grid>
            <GridRow fullHeight>
                <GridColumn verticalAlign={"center"}>
                    <EmptyWorkspace />
                </GridColumn>
            </GridRow>
        </Grid>
    );
}
