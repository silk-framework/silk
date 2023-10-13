import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import {
    Button,
    Divider,
    Grid,
    GridColumn,
    GridRow,
    Notification,
    Section,
    SectionHeader,
    TitleMainsection,
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
} from "@eccenca/gui-elements";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import SearchList from "../../shared/SearchList";
import SearchBar from "../../shared/SearchBar";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import Filterbar from "./Filterbar";

const WorkspaceSearch = () => {
    const dispatch = useDispatch();
    const [t] = useTranslation();

    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);
    const error = useSelector(workspaceSel.errorSelector);

    // FIXME: Workaround to prevent search with a text query from another page sharing the same Redux state. Needs refactoring.
    const [searchInitialized, setSearchInitialized] = React.useState(false);
    const effectiveSearchQuery = searchInitialized ? textQuery : "";

    React.useEffect(() => {
        setSearchInitialized(true);
    }, []);

    const handleSort = (sortBy: string) => {
        dispatch(workspaceOp.applySorterOp(sortBy));
    };

    const handleSearch = (textQuery: string) => {
        dispatch(workspaceOp.applyFiltersOp({ textQuery }));
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
                        <Grid>
                            <GridRow>
                                <GridColumn small verticalAlign="center">
                                    <TitleMainsection>{t("pages.workspace.contents", "Contents")}</TitleMainsection>
                                </GridColumn>
                                <GridColumn full>
                                    <SearchBar
                                        selectFirstResultItemOnEnter
                                        focusOnCreation={true}
                                        textQuery={effectiveSearchQuery}
                                        sorters={sorters}
                                        onSort={handleSort}
                                        onSearch={handleSearch}
                                    />
                                </GridColumn>
                            </GridRow>
                        </Grid>
                    </SectionHeader>
                    <Divider addSpacing="medium" />
                    <Grid>
                        <GridRow>
                            <GridColumn small>
                                <Filterbar />
                            </GridColumn>
                            <GridColumn full>
                                {error.detail ? (
                                    <Notification
                                        danger={true}
                                        actions={
                                            <Button
                                                text={t("common.action.retry", "Retry")}
                                                onClick={() => {
                                                    window.location.reload();
                                                }}
                                            />
                                        }
                                    >
                                        <h3>{t("http.error.fetchNotResult", "Error, cannot fetch results.")}</h3>
                                        <p>{error.detail}</p>
                                    </Notification>
                                ) : (
                                    <SearchList />
                                )}
                            </GridColumn>
                        </GridRow>
                    </Grid>
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section></Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
};

export default WorkspaceSearch;
