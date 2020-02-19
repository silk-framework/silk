import React, { useEffect } from "react";

import './index.scss';
import { useSelector } from "react-redux";
import { workspaceSel } from "@ducks/workspace";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/bluprint/constants";
import { useParams } from "react-router";
import Projects from "./Projects";
import ProjectDetails from "./ProjectDetails";

export default function() {
    const error = useSelector(workspaceSel.errorSelector);
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
        ? <ProjectDetails projectId={projectId}/>
        : <Projects />
}
