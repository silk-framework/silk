import React from "react";
import { useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { Button, Divider, Notification } from "@eccenca/gui-elements";
import { workspaceSel } from "@ducks/workspace";
import { diErrorMessage } from "@ducks/error/typings";
import SearchList from "../../shared/SearchList";
import { GridTileCard } from "../../shared/GridBoard";
import { useWorkbenchListState } from "../../../hooks/useWorkbenchListState";
import WorkspaceToolbar from "../Workspace/Toolbar/WorkspaceToolbar";

interface IProps {
    projectId: string;
}

/**
 * The "Contents" tile of a project detail page: the restyled workbench toolbar (compact search +
 * sort + view toggle + filter dropdowns) over the shared item list. Mirrors the `/workbench`
 * listing, scoped to the current project via the toolbar's `projectId`.
 */
const ProjectContents = ({ projectId }: IProps) => {
    const [t] = useTranslation();

    const data = useSelector(workspaceSel.resultsSelector);
    const { effectiveSearchQuery, error, viewMode, handleSearch, handleViewModeChange, onEnter } =
        useWorkbenchListState("projectContents");

    return (
        <GridTileCard title={t("pages.project.content", "Contents")} data-test-id="project-contents-tile">
            <WorkspaceToolbar
                textQuery={effectiveSearchQuery}
                onSearch={handleSearch}
                onEnter={onEnter}
                viewMode={viewMode}
                onViewModeChange={handleViewModeChange}
                projectId={projectId}
                focusOnCreation={false}
                globalTableKey="projectContents"
            />
            <Divider addSpacing="medium" />
            {!data.length && error.detail ? (
                <Notification
                    intent={error?.status === 503 ? "warning" : "danger"}
                    actions={
                        <Button
                            text={t("common.action.retry", "Retry")}
                            onClick={() => {
                                window.location.reload();
                            }}
                        />
                    }
                >
                    <h3 className="font-medium">
                        {error?.status !== 503
                            ? t("http.error.fetchNotResult", "Error, cannot fetch results.")
                            : t("common.messages.temporarilyUnavailableMessage", {
                                  detailMessage: diErrorMessage(error),
                              })}
                    </h3>
                    {error?.status !== 503 && <p>{error.detail}</p>}
                </Notification>
            ) : (
                <SearchList viewMode={viewMode} flush tableKey="projectContents" />
            )}
        </GridTileCard>
    );
};

export default ProjectContents;
