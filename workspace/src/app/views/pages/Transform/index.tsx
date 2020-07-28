import React, { useEffect } from "react";

import { useSelector } from "react-redux";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@gui-elements/blueprint/constants";
import Metadata from "../../shared/Metadata";
import { datasetSel } from "@ducks/dataset";

import { Section, Spacing, WorkspaceContent, WorkspaceMain, WorkspaceSide } from "@gui-elements/index";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { TaskConfig } from "../../shared/TaskConfig/TaskConfig";
import { useParams } from "react-router";

export default function () {
    const error = useSelector(datasetSel.errorSelector);
    const { taskId, projectId } = useParams();

    useEffect(() => {
        if (error.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 0,
            });
        }
    }, [error.detail]);

    return (
        <WorkspaceContent className="eccapp-di__transformation">
            <WorkspaceMain>
                <Section>
                    <Metadata />
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <RelatedItems projectId={projectId} taskId={taskId} />
                    <Spacing />
                    <TaskConfig projectId={projectId} taskId={taskId} />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
