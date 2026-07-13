import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import { Icon, shadcn } from "@eccenca/gui-elements";
import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { APPLICATION_CORPORATION_NAME, APPLICATION_NAME, APPLICATION_SUITE_NAME } from "../../../constants/base";
import { CONTEXT_PATH, SERVE_PATH } from "../../../constants/path";
import { getFullRoutePath } from "../../../utils/routerUtils";
import { AppDispatch } from "store/configureStore";
import { NavUser } from "./NavUser";

const {
    Sidebar,
    SidebarContent,
    SidebarFooter,
    SidebarGroup,
    SidebarGroupLabel,
    SidebarHeader,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarRail,
} = shadcn;

/**
 * Application sidebar (shadcn sidebar-07 pattern): brand block on top, DM/DI navigation
 * groups, user menu at the footer. Collapses to an icon rail (tooltips take over the labels)
 * via the header trigger, the edge rail or Cmd/Ctrl+B; on small viewports it becomes a sheet.
 */
export function AppSidebar() {
    const dispatch = useDispatch<AppDispatch>();
    const location = useLocation();
    const locationParams = new URLSearchParams(location.search?.substring(1));
    const { dmBaseUrl, dmModuleLinks } = useSelector(commonSel.initialSettingsSelector);
    const [t] = useTranslation();

    const handleNavigate = (path: string) => {
        dispatch(routerOp.goToPage(path));
    };

    const searchURL = (page: string) => `?itemType=${page}`;
    const activitiesPageLink = SERVE_PATH + "/activities";
    const brandingSuffix =
        APPLICATION_CORPORATION_NAME() || APPLICATION_SUITE_NAME()
            ? ` @ ${APPLICATION_CORPORATION_NAME()} ${APPLICATION_SUITE_NAME()}`
            : "";

    const diNavItem = (itemPath: string, icon: string, label: string, tooltip: string, active: boolean) => (
        <SidebarMenuItem>
            <SidebarMenuButton asChild isActive={active} tooltip={tooltip || label}>
                <a
                    href={itemPath}
                    onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        handleNavigate(itemPath);
                    }}
                >
                    <Icon name={[icon]} />
                    <span>{label}</span>
                </a>
            </SidebarMenuButton>
        </SidebarMenuItem>
    );

    return (
        <Sidebar collapsible="icon">
            <SidebarHeader>
                <SidebarMenu>
                    <SidebarMenuItem>
                        <SidebarMenuButton size="lg" asChild tooltip={APPLICATION_NAME()}>
                            <a href={getFullRoutePath("?itemType=project")}>
                                <div className="flex aspect-square size-8 shrink-0 items-center justify-center overflow-hidden rounded-lg">
                                    <img
                                        className="max-h-full max-w-full object-contain"
                                        src={CONTEXT_PATH + "/core/logoSmall.png"}
                                        alt={`Logo: ${APPLICATION_NAME()}${brandingSuffix}`}
                                    />
                                </div>
                                <div className="grid flex-1 text-left text-sm leading-tight">
                                    <span className="truncate font-medium">{APPLICATION_NAME()}</span>
                                    {(APPLICATION_CORPORATION_NAME() || APPLICATION_SUITE_NAME()) && (
                                        <span className="truncate text-xs text-muted-foreground">
                                            {`${APPLICATION_CORPORATION_NAME()} ${APPLICATION_SUITE_NAME()}`.trim()}
                                        </span>
                                    )}
                                </div>
                            </a>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                </SidebarMenu>
            </SidebarHeader>
            <SidebarContent>
                {!!dmBaseUrl && (
                    <SidebarGroup>
                        <SidebarGroupLabel title={t("navigation.side.dmBrowserTooltip", "")}>
                            {t("navigation.side.dmBrowser", "Explore")}
                        </SidebarGroupLabel>
                        <SidebarMenu>
                            {dmModuleLinks ? (
                                dmModuleLinks.map((link) => (
                                    <SidebarMenuItem key={link.path}>
                                        <SidebarMenuButton
                                            asChild
                                            tooltip={t("navigation.side.dm." + link.path, link.defaultLabel)}
                                        >
                                            <a
                                                href={dmBaseUrl + "/" + link.path}
                                                title={t("navigation.side.dm." + link.path + "Tooltip")}
                                            >
                                                <Icon name={link.icon ? [link.icon] : "undefined"} />
                                                <span>{t("navigation.side.dm." + link.path, link.defaultLabel)}</span>
                                            </a>
                                        </SidebarMenuButton>
                                    </SidebarMenuItem>
                                ))
                            ) : (
                                <SidebarMenuItem>
                                    <SidebarMenuButton
                                        asChild
                                        tooltip={t("navigation.side.dm.explore", "Knowledge Graphs")}
                                    >
                                        <a href={dmBaseUrl} title={t("navigation.side.dm.exploreTooltip")}>
                                            <Icon name="application-explore" />
                                            <span>{t("navigation.side.dm.explore", "Knowledge Graphs")}</span>
                                        </a>
                                    </SidebarMenuButton>
                                </SidebarMenuItem>
                            )}
                        </SidebarMenu>
                    </SidebarGroup>
                )}
                <SidebarGroup>
                    <SidebarGroupLabel title={t("navigation.side.diBrowseTooltip", "")}>
                        {t("navigation.side.diBrowse", "")}
                    </SidebarGroupLabel>
                    <SidebarMenu>
                        {diNavItem(
                            SERVE_PATH + searchURL("project"),
                            "artefact-project",
                            t("navigation.side.di.projects", "Projects"),
                            t("navigation.side.di.projectsTooltip"),
                            location.pathname === SERVE_PATH && locationParams.get("itemType") === "project",
                        )}
                        {diNavItem(
                            SERVE_PATH + searchURL("dataset"),
                            "artefact-dataset",
                            t("navigation.side.di.datasets", "Datasets"),
                            t("navigation.side.di.datasetsTooltip"),
                            location.pathname === SERVE_PATH && locationParams.get("itemType") === "dataset",
                        )}
                        {diNavItem(
                            SERVE_PATH + searchURL("workflow"),
                            "artefact-workflow",
                            t("navigation.side.di.workflows", "Workflows"),
                            t("navigation.side.di.workflowsTooltip"),
                            location.pathname === SERVE_PATH && locationParams.get("itemType") === "workflow",
                        )}
                        {diNavItem(
                            activitiesPageLink,
                            "application-activities",
                            t("navigation.side.di.activities", "Activities"),
                            t("navigation.side.di.activitiesTooltip"),
                            location.pathname.includes(activitiesPageLink),
                        )}
                    </SidebarMenu>
                </SidebarGroup>
            </SidebarContent>
            <SidebarFooter>
                <NavUser />
            </SidebarFooter>
            <SidebarRail />
        </Sidebar>
    );
}
