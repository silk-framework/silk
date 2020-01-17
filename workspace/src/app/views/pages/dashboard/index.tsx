import React, { useEffect } from "react";

import './Dashboard.scss';
import { useSelector } from "react-redux";
import { dashboardSel } from "@ducks/dashboard";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/constants";
import { useParams } from "react-router";
import ProjectLayout from "./ProjectLayout";
import TaskLayout from "./TaskLayout";

export default function() {
    const error = useSelector(dashboardSel.errorSelector);
    const {projectId} = useParams();

    useEffect(() => {
        if (error.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 2000
            })
        }
    }, [error.detail]);

    return projectId
        ? <TaskLayout projectId={projectId}/>
        : <ProjectLayout />
}
