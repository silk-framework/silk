import React, { useEffect } from "react";

import './index.scss';
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/blueprint/constants";
import { useLocation, useParams } from "react-router";
import Artefacts from "./Artefacts";
import Project from "../Project/Project";
import { routerSel } from "@ducks/router";
import { globalOp } from "@ducks/global";

export function Workspace() {
    const dispatch = useDispatch();
    const error = useSelector(workspaceSel.errorSelector);
    const qs = useSelector(routerSel.routerSearchSelector);

    const location = useLocation();
    const {projectId} = useParams();

    useEffect(() => {
        dispatch(globalOp.fetchAvailableDTypesAsync(projectId));
    }, [projectId]);

    useEffect(() => {
        if (error.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 2000
            })
        }
    }, [error.detail]);

    useEffect(() => {
        // Reset the filters, due to redirecting
        dispatch(workspaceOp.resetFilters());

        // Setup the filters from query string
        dispatch(workspaceOp.setupFiltersFromQs(qs));

        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync());
    }, [location.pathname, qs]);

    return projectId
        ? <Project projectId={projectId}/>
        : <Artefacts />
}
