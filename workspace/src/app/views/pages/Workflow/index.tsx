import React, { useEffect, useState } from "react";
import { useParams } from "react-router";
import { useSelector } from "react-redux";
import { Intent } from "@gui-elements/blueprint/constants";
import { Section, WorkspaceContent, WorkspaceMain, WorkspaceSide, Spacing } from "@gui-elements/index";
import { datasetSel } from "@ducks/dataset";
import { AppToaster } from "../../../services/toaster";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { IframeWindow } from "../../shared/IframeWindow/IframeWindow";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";

export default function () {
    const error = useSelector(datasetSel.errorSelector);
    const { taskId, projectId } = useParams();
    const [notFound, setNotFound] = useState(false);

    useEffect(() => {
        if (error?.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 0,
            });
        }
    }, [error.detail]);

    const { pageHeader, updateActionsMenu } = usePageHeader({
        type: DATA_TYPES.WORKFLOW,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    return notFound ? (
        <NotFound />
    ) : (
        <WorkspaceContent className="eccapp-di__workflow">
            {pageHeader}
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={DATA_TYPES.WORKFLOW}
                updateActionsMenu={updateActionsMenu}
                notFoundCallback={setNotFound}
            />
            <WorkspaceMain>
                <Section>
                    <Metadata />
                    <Spacing />
                    <IframeWindow iFrameName={"detail-page-iframe"} />
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <RelatedItems messageEventReloadTrigger={(messageId) => messageId === "workflowSaved"} />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
