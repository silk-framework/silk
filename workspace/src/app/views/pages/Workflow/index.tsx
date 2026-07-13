import React, { useState } from "react";
import { useParams } from "react-router";
import { Section, Spacing, WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";
import { ProjectTaskParams } from "../../shared/typings";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { RelatedItemsMenu } from "../../shared/PageHeaderMenus/RelatedItemsMenu";

export default function WorkflowPage() {
    const { taskId, projectId } = useParams<ProjectTaskParams>();
    const [notFound, setNotFound] = useState(false);
    const [forbidden, setForbidden] = useState(false);

    const { pageHeader, updateActionsMenu } = usePageHeader({
        type: DATA_TYPES.WORKFLOW,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    // View action that should be triggered when a workflow is saved
    const onSave = () => {
        window.top?.postMessage(
            JSON.stringify({
                id: "workflowSaved",
                message: "Workflow updated",
            }),
            "*",
        );
    };

    // Must be referentially stable, since a change re-triggers the page header actions update.
    const headerMenus = React.useMemo(
        () => <RelatedItemsMenu messageEventReloadTrigger={(messageId) => messageId === "workflowSaved"} />,
        [],
    );

    if (forbidden) {
        return <ProjectForbiddenNotification />;
    } else if (notFound) {
        return <NotFound />;
    }
    return (
        <WorkspaceContent className="eccapp-di__workflow">
            {pageHeader}
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={DATA_TYPES.WORKFLOW}
                updateActionsMenu={updateActionsMenu}
                notFoundCallback={setNotFound}
                forbiddenCallback={setForbidden}
                headerMenus={headerMenus}
            />
            <WorkspaceMain>
                <Section>
                    <DeprecatedPluginsBanner projectId={projectId} taskId={taskId} />
                    <Metadata />
                    <Spacing />
                    <ProjectTaskTabView
                        iFrameName={"detail-page-iframe"}
                        taskViewConfig={{ pluginId: "workflow", projectId, taskId }}
                        viewActions={{
                            onSave,
                        }}
                    />
                </Section>
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
