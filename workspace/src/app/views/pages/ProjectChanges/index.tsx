import React from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router";
import { batch, useDispatch, useSelector } from "react-redux";
import {
    Divider,
    Grid,
    GridColumn,
    GridRow,
    IconButton,
    Section,
    SectionHeader,
    TitleMainsection,
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
} from "@eccenca/gui-elements";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { routerSel } from "@ducks/router";
import { filtersSlice } from "@ducks/workspace/filtersSlice";
import { previewSlice } from "@ducks/workspace/previewSlice";
import { AppDispatch } from "store/configureStore";
import { GlobalTableContext } from "../../../GlobalContextsWrapper";
import { usePageHeader } from "../../shared/PageHeader/PageHeader";
import { requestProjectMetadata } from "@ducks/shared/requests";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { absoluteProjectPath, getFullRoutePath } from "../../../utils/routerUtils";
import NotFound from "../NotFound";
import MarkdownModal from "../../shared/modals/MarkdownModal";
import SearchBar from "../../shared/SearchBar";
import Filterbar from "../Workspace/Filterbar";
import ChangeList from "./ChangeList";
import { changeJournalHelpMarkdown } from "./changeJournalHelp";

const PAGE_SIZE = 25;

/** The change journal of a project: what has been changed, searched and filtered, and reverting single changes. */
const ProjectChangesPage = ({ projectId }: { projectId: string }) => {
    const [t] = useTranslation();
    const dispatch = useDispatch<AppDispatch>();
    const { registerError } = useErrorHandler();
    const qs = useSelector(routerSel.routerSearchSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);
    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const { clearSearchResults, setError } = previewSlice.actions;
    const { globalTableSettings, updateGlobalTableSettings } = React.useContext(GlobalTableContext);
    const tableSettings = globalTableSettings["changes"] ?? {};
    const [notFound, setNotFound] = React.useState(false);
    const [refreshKey, setRefreshKey] = React.useState(0);
    const [helpOpen, setHelpOpen] = React.useState(false);
    // The search store is shared with the workspace search and the activities page: the list waits until it is this page's
    const [ready, setReady] = React.useState(false);

    const breadcrumbs = [
        {
            text: t("common.app.build", "Workbench"),
            href: getFullRoutePath("?itemType=project"),
        },
        {
            text: t("pages.changes.title"),
            current: true,
        },
    ];

    const { pageHeader, updatePageHeader } = usePageHeader({
        alternateDepiction: "artefact-commit",
        autogeneratePageTitle: true,
        breadcrumbs,
    });

    React.useEffect(() => {
        requestProjectMetadata(projectId)
            .then((res) => {
                updatePageHeader({
                    breadcrumbs: [
                        breadcrumbs[0],
                        {
                            text: res.data.label,
                            href: absoluteProjectPath(projectId),
                        },
                        breadcrumbs[1],
                    ],
                    pageTitle: `${breadcrumbs[1].text}: ${res.data.label}`,
                });
            })
            .catch((ex) => {
                if (ex?.httpStatus === 404) {
                    setNotFound(true);
                } else {
                    registerError("ProjectChanges.projectMetadata", t("pages.changes.errors.fetchProject"), ex);
                }
            });
    }, [projectId]);

    React.useEffect(() => {
        // Settings stored before the page sorted by time name no property; the sort button needs the one it shows
        if (!tableSettings.sortBy) {
            updateGlobalTableSettings({ sortBy: "time", sortOrder: "DESC" }, "changes");
        }
        // Starts from this page's URL, not from what another page left in the store, whose results are not changes;
        // the results are dropped on leaving as well
        batch(() => {
            dispatch(clearSearchResults());
            dispatch(setError({}));
            dispatch(workspaceOp.resetFilters());
            dispatch(filtersSlice.actions.updateFacets([]));
            dispatch(filtersSlice.actions.updateSorters([]));
            dispatch(filtersSlice.actions.changePage(1));
            dispatch(workspaceOp.changeLimitOp(tableSettings.pageSize ?? PAGE_SIZE));
            dispatch(workspaceOp.setupFiltersFromQs(qs));
        });
        setReady(true);
        return () => {
            dispatch(clearSearchResults());
        };
    }, [projectId]);

    // The store's default sorter is the workspace search's recently viewed order, which the journal has not: its one
    // sort property, time, is active from the start, newest first, which is also the server's order without a property
    const changeSorters = {
        list: sorters.list.filter((sorter) => sorter.id),
        applied: { sortBy: tableSettings.sortBy || "time", sortOrder: tableSettings.sortOrder || "DESC" },
    };

    // The search bar also fires for the query it was handed, e.g. the URL's; applying it again would clear the facets.
    // Enter, whose default would search the unchanged query as well, reloads the list instead (onEnter)
    const appliedQuery = React.useRef(textQuery);
    appliedQuery.current = textQuery;
    const handleSearch = (query: string) => {
        if ((query || "") === (appliedQuery.current || "")) {
            return;
        }
        batch(() => {
            dispatch(workspaceOp.applyFiltersOp({ textQuery: query }));
            dispatch(workspaceOp.changePageOp(1));
        });
    };

    if (notFound) {
        return <NotFound />;
    }

    return (
        <WorkspaceContent>
            {pageHeader}
            <WorkspaceMain>
                <Section>
                    <SectionHeader>
                        <Grid>
                            <GridRow>
                                <GridColumn small verticalAlign="center">
                                    <TitleMainsection>{t("pages.changes.title", "Changes")}</TitleMainsection>
                                </GridColumn>
                                <GridColumn>
                                    <div style={{ display: "flex", alignItems: "center" }}>
                                        <div style={{ width: "100%" }}>
                                            <SearchBar
                                                focusOnCreation
                                                textQuery={ready ? textQuery : ""}
                                                sorters={changeSorters}
                                                onSearch={handleSearch}
                                                onEnter={() => setRefreshKey((key) => key + 1)}
                                                globalTableKey={"changes"}
                                            />
                                        </div>
                                        <IconButton
                                            data-test-id={"changes-reload-btn"}
                                            name="item-reload"
                                            text={t("pages.changes.reload")}
                                            onClick={() => setRefreshKey((key) => key + 1)}
                                        />
                                        <IconButton
                                            data-test-id={"changes-help-btn"}
                                            name="item-question"
                                            text={t("pages.changes.help.open")}
                                            onClick={() => setHelpOpen(true)}
                                        />
                                    </div>
                                </GridColumn>
                            </GridRow>
                        </Grid>
                    </SectionHeader>
                    <Divider addSpacing="medium" />
                    <Grid>
                        <GridRow>
                            <GridColumn small>
                                <Filterbar facetsOnly projectId={projectId} />
                            </GridColumn>
                            <GridColumn>
                                <ChangeList projectId={projectId} ready={ready} refreshKey={refreshKey} />
                            </GridColumn>
                        </GridRow>
                    </Grid>
                </Section>
            </WorkspaceMain>
            <WorkspaceSide></WorkspaceSide>
            <MarkdownModal
                data-test-id={"changes-help-modal"}
                isOpen={helpOpen}
                onDiscard={() => setHelpOpen(false)}
                title={t("pages.changes.help.title")}
                markdown={changeJournalHelpMarkdown}
                size="large"
            />
        </WorkspaceContent>
    );
};

/**
 * The changes page of the project in the URL. The router keeps the page when only the project changes, so the page
 * is keyed by the project: nothing of the previous project is kept, and a late answer for it is not shown.
 */
const ProjectChanges = () => {
    const { projectId } = useParams<{ projectId: string }>();
    return <ProjectChangesPage key={projectId} projectId={projectId} />;
};

export default ProjectChanges;
