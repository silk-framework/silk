import React, { useEffect } from "react";

import "./index.scss";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/blueprint/constants";
import Artefacts from "./Artefacts";
import Project from "../Project/Project";
import { routerSel } from "@ducks/router";
import { Grid, GridColumn, GridRow } from "@wrappers/index";
import { EmptyWorkspace } from "./EmptyWorkspace/EmptyWorkspace";
import { commonSel } from "@ducks/common";

export function Workspace() {
    const dispatch = useDispatch();

    const error = useSelector(workspaceSel.errorSelector);
    const qs = useSelector(routerSel.routerSearchSelector);
    const isEmptyWorkspace = useSelector(workspaceSel.isEmptyPageSelector);
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const taskId = useSelector(commonSel.currentTaskIdSelector);

    useEffect(() => {
        if (error.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 0,
            });
        }
    }, [error.detail]);

    useEffect(() => {
        // Reset the filters, due to redirecting
        dispatch(workspaceOp.resetFilters());

        // Setup the filters from query string
        dispatch(workspaceOp.setupFiltersFromQs(qs));

        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync());
    }, [projectId, taskId, qs]);

    if (projectId) {
        return <Project />;
    }

    return !isEmptyWorkspace ? (
        <Artefacts />
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
