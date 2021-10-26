import React, { useEffect } from "react";

import "./index.scss";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import WorkspaceSearch from "./WorkspaceSearch";
import { routerSel } from "@ducks/router";
import { Grid, GridColumn, GridRow } from "@gui-elements/index";
import { EmptyWorkspace } from "./EmptyWorkspace/EmptyWorkspace";
import { commonOp, commonSel } from "@ducks/common";
import useErrorHandler from "../../../hooks/useErrorHandler";

export function Workspace() {
    const dispatch = useDispatch();
    const { registerError } = useErrorHandler();

    const error = useSelector(workspaceSel.errorSelector);
    const qs = useSelector(routerSel.routerSearchSelector);
    const isEmptyWorkspace = useSelector(workspaceSel.isEmptyPageSelector);
    const projectId = useSelector(commonSel.currentProjectIdSelector);

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
    }, [qs]);

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
