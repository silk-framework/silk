import React from "react";
import { useDispatch, useSelector } from "react-redux";
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
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import SearchList from "../../shared/SearchList";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import WorkspaceToolbar from "./Toolbar/WorkspaceToolbar";
import { useSelectFirstResult } from "../../../hooks/useSelectFirstResult";
import { AppDispatch } from "store/configureStore";
import { GlobalTableContext } from "../../../GlobalContextsWrapper";
import { WorkbenchViewMode } from "../../../hooks/useStoreGlobalTableSettings";

const WorkspaceSearch = () => {
    const dispatch = useDispatch<AppDispatch>();
    const [t] = useTranslation();

    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const error = useSelector(workspaceSel.errorSelector);
    const { globalTableSettings, updateGlobalTableSettings } = React.useContext(GlobalTableContext);
    const viewMode: WorkbenchViewMode = globalTableSettings["workbench"].viewMode ?? "table";

    // FIXME: Workaround to prevent search with a text query from another page sharing the same Redux state. Needs refactoring.
    const [searchInitialized, setSearchInitialized] = React.useState(false);
    const effectiveSearchQuery = searchInitialized ? textQuery : "";
    const { onEnter } = useSelectFirstResult();

    React.useEffect(() => {
        setSearchInitialized(true);
    }, []);

    const handleSearch = (textQuery: string) => {
        dispatch(workspaceOp.applyFiltersOp({ textQuery }));
    };

    const handleViewModeChange = (mode: WorkbenchViewMode) => {
        updateGlobalTableSettings({ viewMode: mode }, "workbench");
    };

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
