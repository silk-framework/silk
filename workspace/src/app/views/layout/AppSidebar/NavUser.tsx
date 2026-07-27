import React from "react";
import { useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { Icon, Menu, shadcn } from "@eccenca/gui-elements";
import { commonSel } from "@ducks/common";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";
import { APPLICATION_CORPORATION_NAME } from "../../../constants/base";

const {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuTrigger,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    useSidebar,
} = shadcn;

/** Local name of a user URI (`…#name` / `…/name` / `…:name`). */
const localPart = (uri: string): string => {
    const i = Math.max(uri.lastIndexOf("#"), uri.lastIndexOf("/"), uri.lastIndexOf(":"));
    return i >= 0 ? uri.slice(i + 1) : uri;
};

/**
 * Sidebar footer profile menu. The tile shows the logged-in user (from
 * `initialSettings.userUri`); clicking opens a popup menu to the side with the language switcher,
 * account actions (logout) and the version line. The menu's gui-elements `MenuItem`s render in
 * STATIC mode (plain `<li>` via `<Menu>`) — dropdown mode would emit a Radix item from a
 * different Radix instance than `shadcn.DropdownMenuContent` and crash ("MenuItem must be used
 * within Menu").
 */
export function NavUser() {
    const [t] = useTranslation();
    const { dmBaseUrl, version, userUri } = useSelector(commonSel.initialSettingsSelector);
    const { isMobile } = useSidebar();

    const diUserMenuItems = pluginRegistry.pluginReactComponent<{}>(SUPPORTED_PLUGINS.DI_USER_MENU_ITEMS);
    const languageSwitcher = pluginRegistry.pluginReactComponent<{}>(SUPPORTED_PLUGINS.DI_LANGUAGE_SWITCHER);

    const userName = userUri ? localPart(userUri) : t("navigation.user.account", "Account");

    return (
        <SidebarMenu>
            <SidebarMenuItem className="rounded-lg border border-sidebar-border bg-card p-1 group-data-[collapsible=icon]:border-0 group-data-[collapsible=icon]:bg-transparent group-data-[collapsible=icon]:p-0">
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        {/* `id` kept for the logout integration test (opens this menu, clicks logout). */}
                        <SidebarMenuButton
                            id="headerUserMenu"
                            size="lg"
                            aria-label={t("navigation.user.open", "Open user menu")}
                            className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
                        >
                            {/* Outline avatar box — secondary to the solid-blue project tile. */}
                            <div className="flex aspect-square size-8 shrink-0 items-center justify-center rounded-lg border border-sidebar-primary text-sidebar-primary">
                                <Icon name="application-useraccount" />
                            </div>
                            <div className="grid flex-1 text-left text-sm leading-tight">
                                <span className="truncate font-medium">{userName}</span>
                                <span className="truncate text-xs text-muted-foreground">
                                    {t("navigation.user.account", "Account")}
                                </span>
                            </div>
                            <Icon name="toggler-caret" small className="ml-auto" />
                        </SidebarMenuButton>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                        // No fixed trigger-width: let the menu size to its content so the version
                        // line fits on a single line instead of being clipped/wrapped.
                        className="min-w-56 rounded-lg"
                        side={isMobile ? "bottom" : "right"}
                        align="end"
                        sideOffset={4}
                    >
                        {languageSwitcher && (
                            <div className="px-1 pt-1">
                                <div className="px-2 pb-1 text-xs font-medium text-muted-foreground">
                                    {t("navigation.user.language", "Language")}
                                </div>
                                <languageSwitcher.Component />
                            </div>
                        )}
                        {!!dmBaseUrl && diUserMenuItems && (
                            <Menu>
                                <diUserMenuItems.Component />
                            </Menu>
                        )}
                        {version && (
                            <p className="mt-2 whitespace-nowrap border-t border-sidebar-border px-2 pt-2 text-xs text-muted-foreground">
                                {APPLICATION_CORPORATION_NAME()
                                    ? t("navigation.user.versionBy", "{{version}} by {{corporation}}", {
                                          version,
                                          corporation: APPLICATION_CORPORATION_NAME(),
                                      })
                                    : version}
                            </p>
                        )}
                    </DropdownMenuContent>
                </DropdownMenu>
            </SidebarMenuItem>
        </SidebarMenu>
    );
}
