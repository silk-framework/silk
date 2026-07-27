import React from "react";
import { useTranslation } from "react-i18next";
import {
    Button,
    Divider,
    Notification,
    Section,
    SectionHeader,
    Spacing,
    TitleMainsection,
    WorkspaceContent,
    WorkspaceMain,
} from "@eccenca/gui-elements";
import SearchList from "../../shared/SearchList";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import WorkspaceToolbar from "./Toolbar/WorkspaceToolbar";
import { useWorkbenchListState } from "../../../hooks/useWorkbenchListState";

const WorkspaceSearch = () => {
    const [t] = useTranslation();

    const { effectiveSearchQuery, error, viewMode, handleSearch, handleViewModeChange, onEnter } =
        useWorkbenchListState("workbench");

    const { pageHeader } = usePageHeader({
        alternateDepiction: "application-homepage",
        autogenerateBreadcrumbs: true,
        autogeneratePageTitle: true,
    });

    return (
        <WorkspaceContent className="eccapp-di__workspace">
            {pageHeader}
            <WorkspaceMain>
                <Section>
                    <SectionHeader>
                        <TitleMainsection>{t("pages.workspace.contents", "Contents")}</TitleMainsection>
                    </SectionHeader>
                    <Spacing size="small" />
                    <WorkspaceToolbar
                        textQuery={effectiveSearchQuery}
                        onSearch={handleSearch}
                        onEnter={onEnter}
                        viewMode={viewMode}
                        onViewModeChange={handleViewModeChange}
                    />
                    <Divider addSpacing="medium" />
                    {error.detail ? (
                        <Notification
                            intent="danger"
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
                                {t("http.error.fetchNotResult", "Error, cannot fetch results.")}
                            </h3>
                            <p>{error.detail}</p>
                        </Notification>
                    ) : (
                        <SearchList viewMode={viewMode} />
                    )}
                </Section>
            </WorkspaceMain>
        </WorkspaceContent>
    );
};

export default WorkspaceSearch;
