import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { matchPath, useLocation } from "react-router";
import { Icon, shadcn } from "@eccenca/gui-elements";
import { commonOp, commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { requestSearchList } from "@ducks/workspace/requests";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { DATA_TYPES } from "../../../constants";
import { getFullRoutePath } from "../../../utils/routerUtils";
import { AppDispatch } from "store/configureStore";

const {
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger,
    SidebarInput,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarMenuSub,
    SidebarMenuSubButton,
    SidebarMenuSubItem,
    useSidebar,
} = shadcn;

// Show ~5 project rows before scrolling (each SidebarMenuSubButton is h-7 / 28px).
const VISIBLE_ROWS_MAX_HEIGHT = "max-h-[10rem]";

/**
 * Project switcher, styled like the footer user tile (`NavUser`): a large tile with an icon
 * box, the current project name and a caret. Clicking expands it in place (an inline
 * Collapsible — not a popup) to a search field and a short, scrollable project list fetched via
 * `searchItems` (`itemType: "project"`). Picking a project navigates and collapses back to the
 * tile. With exactly one project the tile selects it directly; with none it reads "Select a
 * project".
 */
export function NavProject() {
    const [t] = useTranslation();
    const dispatch = useDispatch<AppDispatch>();
    const currentProjectId = useSelector(commonSel.currentProjectIdSelector);
    const location = useLocation();
    // Only the dashboard/workspace landing route has "no project in context" as its natural state.
    // Global pages (deprecated plugins, activities) also clear `currentProjectId`, so we must not
    // treat "no current project" alone as a cue to auto-open the single project — see the effect below.
    const onDashboard = matchPath(location.pathname, { path: getFullRoutePath("/"), exact: true }) != null;
    const { state: sidebarState, setOpen: setSidebarExpanded } = useSidebar();

    const [projects, setProjects] = React.useState<ISearchResultsServer[]>([]);
    const [loading, setLoading] = React.useState<boolean>(false);
    const [open, setOpen] = React.useState<boolean>(false);
    const [query, setQuery] = React.useState<string>("");
    const searchRef = React.useRef<HTMLInputElement>(null);

    const loadProjects = React.useCallback(async () => {
        setLoading(true);
        try {
            const { results } = await requestSearchList({ itemType: "project", limit: 100 });
            setProjects(results);
        } catch {
            // Keep whatever list we already have; the tile still works as a static label.
        } finally {
            setLoading(false);
        }
    }, []);

    // Fetch on mount and whenever the active project changes, so the current label stays fresh.
    React.useEffect(() => {
        loadProjects();
    }, [loadProjects, currentProjectId]);

    // "If there is only one project it should be automatically selected" — but ONLY on the dashboard
    // landing route. Every global page (deprecated plugins, activities) also clears `currentProjectId`,
    // and firing here would bounce the user straight back into the single project, making those pages
    // unreachable. Gating on `onDashboard` keeps the convenience without hijacking deliberate navigation.
    // The same route with a query string is the search-results page (quick search navigates to
    // `?textQuery=…`, the logo link to `?itemType=project`), so also require an empty query string —
    // otherwise a 1-project workspace could never see search results.
    React.useEffect(() => {
        if (onDashboard && location.search === "" && !currentProjectId && projects.length === 1) {
            selectProject(projects[0].id);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projects, currentProjectId, onDashboard, location.search]);

    // Move focus into the search field once the switcher expands.
    React.useEffect(() => {
        if (open) {
            searchRef.current?.focus();
        }
    }, [open]);

    const navigate = (path: string) => dispatch(routerOp.goToPage(path));

    const selectProject = (projectId: string) => {
        navigate(`projects/${projectId}`);
        setOpen(false);
        setQuery("");
    };

    // Open the create dialog pre-selected for a project (same action as the empty-workspace
    // "Create Project" button).
    const createNewProject = () => {
        dispatch(commonOp.selectArtefact({ key: DATA_TYPES.PROJECT }));
        setOpen(false);
        setQuery("");
    };

    // Clicking the tile always expands the inline panel (search + list). In the collapsed icon
    // rail there is no room for it, so expand the whole sidebar first.
    const handleOpenChange = (next: boolean) => {
        if (next && sidebarState === "collapsed") {
            setSidebarExpanded(true);
        }
        if (next) {
            loadProjects();
        }
        setOpen(next);
    };

    const currentProject = projects.find((p) => p.id === currentProjectId);
    const label = currentProject?.label || currentProjectId || t("navigation.side.project.select", "Select a project");

    const normalizedQuery = query.trim().toLowerCase();
    const filteredProjects = normalizedQuery
        ? projects.filter((p) => (p.label || p.id).toLowerCase().includes(normalizedQuery))
        : projects;

    return (
        <SidebarMenu>
            <SidebarMenuItem>
                {/* The whole switcher (tile + search + list) is ONE flat card at all times — the
                    tile is its header; expanding just reveals the search + list inside the card. */}
                <Collapsible
                    open={open}
                    onOpenChange={handleOpenChange}
                    className="group/project rounded-lg border border-sidebar-border bg-card p-1 group-data-[collapsible=icon]:border-0 group-data-[collapsible=icon]:bg-transparent group-data-[collapsible=icon]:p-0"
                >
                    <CollapsibleTrigger asChild>
                        {/* `title`, not `tooltip`: a `tooltip` prop makes SidebarMenuButton render a
                            <Tooltip> wrapper, which swallows the CollapsibleTrigger's click. The
                            classes mirror NavUser's footer tile so the two read as a pair. */}
                        <SidebarMenuButton
                            size="lg"
                            title={label}
                            aria-label={t("navigation.side.project.switch", "Choose a project")}
                        >
                            <div className="flex aspect-square size-8 shrink-0 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                                <Icon name="artefact-project" />
                            </div>
                            <div className="grid flex-1 text-left text-sm leading-tight">
                                <span className="truncate font-medium">{label}</span>
                                <span className="truncate text-xs text-muted-foreground">
                                    {t("navigation.side.project.label", "Project")}
                                </span>
                            </div>
                            <Icon name="toggler-caret" small className="ml-auto" />
                        </SidebarMenuButton>
                    </CollapsibleTrigger>
                    <CollapsibleContent>
                        {/* The card frame lives on the Collapsible root now; this just lays out the
                            search field + list below the tile header. */}
                        <div className="mt-1 flex flex-col gap-2 px-1 pb-1">
                            <SidebarInput
                                ref={searchRef}
                                type="search"
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                                placeholder={t("navigation.side.project.searchPlaceholder", "Search projects…")}
                                aria-label={t("navigation.side.project.searchPlaceholder", "Search projects…")}
                            />
                            {/* Drop SidebarMenuSub's tree indent/left-rule so the list sits flush in the card. */}
                            <SidebarMenuSub
                                className={`mx-0 gap-1 border-l-0 px-0 ${VISIBLE_ROWS_MAX_HEIGHT} overflow-y-auto`}
                            >
                                {loading && projects.length === 0 ? (
                                    <li className="px-2 py-1.5 text-sm text-sidebar-foreground/70">
                                        {t("common.words.loading", "Loading")}…
                                    </li>
                                ) : filteredProjects.length === 0 ? (
                                    <li className="px-2 py-1.5 text-sm text-sidebar-foreground/70">
                                        {t("navigation.side.project.empty", "No projects found")}
                                    </li>
                                ) : (
                                    filteredProjects.map((project) => (
                                        <SidebarMenuSubItem key={project.id}>
                                            <SidebarMenuSubButton
                                                asChild
                                                isActive={project.id === currentProjectId}
                                                className="h-8 w-full"
                                            >
                                                <button type="button" onClick={() => selectProject(project.id)}>
                                                    <span>{project.label || project.id}</span>
                                                </button>
                                            </SidebarMenuSubButton>
                                        </SidebarMenuSubItem>
                                    ))
                                )}
                            </SidebarMenuSub>
                            {/* Create-project action, separated from the list by a divider. */}
                            <div className="mt-1 border-t border-sidebar-border pt-1">
                                <button
                                    type="button"
                                    onClick={createNewProject}
                                    className="flex h-8 w-full items-center gap-2 rounded-md px-2 text-sm font-medium text-sidebar-primary hover:bg-sidebar-accent"
                                >
                                    <Icon name="item-add-artefact" small />
                                    <span>{t("navigation.side.project.new", "New project")}</span>
                                </button>
                            </div>
                        </div>
                    </CollapsibleContent>
                </Collapsible>
            </SidebarMenuItem>
        </SidebarMenu>
    );
}
