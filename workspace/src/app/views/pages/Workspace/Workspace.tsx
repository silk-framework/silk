import React, { useEffect } from "react";

import './index.scss';
import { useSelector } from "react-redux";
import { workspaceSel } from "@ducks/workspace";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/blueprint/constants";
import { useParams } from "react-router";
import Artefacts from "./Artefacts";
import Project from "../Project/Project";

export function Workspace() {
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
        ? <Project projectId={projectId}/>
        : <Artefacts />
}
