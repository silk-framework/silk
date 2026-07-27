import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { matchPath, useLocation } from "react-router";
import { Icon, shadcn } from "@eccenca/gui-elements";
import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { requestSearchList } from "@ducks/workspace/requests";
import { requestProjectMetadata } from "@ducks/shared/requests";
import { SERVE_PATH } from "../../../constants/path";
import { getFullRoutePath } from "../../../utils/routerUtils";
import { AppDispatch } from "store/configureStore";

const { SidebarMenu, SidebarMenuButton, SidebarMenuItem } = shadcn;

/**
 * Project tile — a link to the projects page that also indicates the currently selected project.
 * The header shows the active project's label (or "Select a project"); clicking navigates to the
 * project item list. On the dashboard landing route with exactly one project, that project is
 * auto-selected.
 */
export function NavProject() {
    const [t] = useTranslation();
    const dispatch = useDispatch<AppDispatch>();
    const currentProjectId = useSelector(commonSel.currentProjectIdSelector);
    const location = useLocation();
    // Only the dashboard landing route has "no project in context" as its natural state. Global
    // pages (deprecated plugins, activities) also clear `currentProjectId`, so gate auto-select on
    // the dashboard route to avoid bouncing the user back into the single project.
    const onDashboard = matchPath(location.pathname, { path: getFullRoutePath("/"), exact: true }) != null;

    const [projectLabel, setProjectLabel] = React.useState<string | undefined>(undefined);

    const navigate = (path: string) => dispatch(routerOp.goToPage(path));

    // Resolve the current project's label directly instead of listing (up to 100) projects just to
    // find the one we need; refresh when the active project changes.
    React.useEffect(() => {
        if (!currentProjectId) {
            setProjectLabel(undefined);
            return;
        }
        let active = true;
        requestProjectMetadata(currentProjectId)
            .then(({ data }) => {
                if (active) setProjectLabel(data.label);
            })
            .catch(() => {
                // Keep the id as the label fallback.
            });
        return () => {
            active = false;
        };
    }, [currentProjectId]);

    // "If there is only one project it should be automatically selected" — but ONLY on the
    // dashboard landing route with an empty query string, so global pages and search stay reachable.
    React.useEffect(() => {
        if (!(onDashboard && location.search === "" && !currentProjectId)) {
            return;
        }
        let active = true;
        // Only the count matters here, so a minimal `limit` avoids fetching a project list just to
        // check its size.
        requestSearchList({ itemType: "project", limit: 1 })
            .then(({ total, results }) => {
                if (active && total === 1 && results[0]) {
                    navigate(`projects/${results[0].id}`);
                }
            })
            .catch(() => {
                // Stay on the dashboard; nothing to auto-select.
            });
        return () => {
            active = false;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentProjectId, onDashboard, location.search]);

    const label = projectLabel || currentProjectId || t("navigation.side.project.select", "Select a project");

    return (
        <SidebarMenu>
            <SidebarMenuItem className="rounded-lg border border-sidebar-border bg-card p-1 group-data-[collapsible=icon]:border-0 group-data-[collapsible=icon]:bg-transparent group-data-[collapsible=icon]:p-0">
                {/* Links to the projects page; the label indicates the currently selected project. */}
                <SidebarMenuButton
                    size="lg"
                    onClick={() => navigate(`${SERVE_PATH}?itemType=project`)}
                    title={label}
                    aria-label={t("navigation.side.project.projects", "Projects")}
                    // Shown only in the collapsed icon rail (shadcn hides it when expanded). Safe to
                    // use now that this button is no longer wrapped in a CollapsibleTrigger. The
                    // `--z-app-header-menu` token lifts it above the header (default is z-50).
                    tooltip={{ children: label, className: "z-[var(--z-app-header-menu)]" }}
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
                    <Icon name="toggler-caretright" small className="ml-auto" />
                </SidebarMenuButton>
            </SidebarMenuItem>
        </SidebarMenu>
    );
}
