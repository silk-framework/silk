import React, { useState } from "react";
import { useParams } from "react-router";
import { useTranslation } from "react-i18next";
import { Section, Spacing, WorkspaceContent, WorkspaceMain } from "@eccenca/gui-elements";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../../shared/Metadata";
import { ProjectTaskTabView } from "../../shared/projectTaskTabView/ProjectTaskTabView";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { ArtefactManagementOptions } from "../../shared/ActionsMenu/ArtefactManagementOptions";
import NotFound from "../NotFound";
import { ProjectTaskParams } from "../../shared/typings";
import { LinkageRuleConfig } from "./config/LinkageRuleConfig";
import DeprecatedPluginsBanner from "../Project/DeprecatedPlugins/DeprecatedPluginsBanner";
import { ProjectForbiddenNotification } from "../../shared/ProjectForbiddenNotification";
import { RelatedItemsMenu } from "../../shared/PageHeaderMenus/RelatedItemsMenu";
import { TaskActivitiesMenu } from "../../shared/PageHeaderMenus/TaskActivitiesMenu";
import { HeaderPopoverButton } from "../../shared/PageHeaderMenus/HeaderPopoverButton";

export default function LinkingPage() {
    const { projectId, taskId } = useParams<ProjectTaskParams>();
    const [t] = useTranslation();
    const [notFound, setNotFound] = useState(false);
    const [forbidden, setForbidden] = useState(false);

    const { pageHeader, updateActionsMenu } = usePageHeader({
        type: DATA_TYPES.LINKING,
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    // Must be referentially stable, since a change re-triggers the page header actions update.
    const headerMenus = React.useMemo(
        () => (
            <>
                <RelatedItemsMenu />
                <TaskActivitiesMenu projectId={projectId} taskId={taskId} />
                <HeaderPopoverButton
                    icon="module-linkedrules"
                    title={t("widget.LinkingRuleConfigWidget.title", "Configuration: Linkage rule")}
                    data-test-id="header-linkage-rule-menu"
                >
                    <LinkageRuleConfig projectId={projectId} linkingTaskId={taskId} />
                </HeaderPopoverButton>
            </>
        ),
        [projectId, taskId, t],
    );

    if (forbidden) {
        return <ProjectForbiddenNotification />;
    } else if (notFound) {
        return <NotFound />;
    }

    return (
        <WorkspaceContent className="eccapp-di__linking">
            {pageHeader}
            <ArtefactManagementOptions
                projectId={projectId}
                taskId={taskId}
                itemType={DATA_TYPES.LINKING}
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
                        taskViewConfig={{ pluginId: "linking", projectId: projectId, taskId: taskId }}
                        iFrameName={"detail-page-iframe"}
                    />
                </Section>
            </WorkspaceMain>
        </WorkspaceContent>
    );
}
