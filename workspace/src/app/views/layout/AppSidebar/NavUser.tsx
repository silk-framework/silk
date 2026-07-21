import React from "react";
import { useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { Menu, shadcn } from "@eccenca/gui-elements";
import { commonSel } from "@ducks/common";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";
import { UserMenuFooterProps } from "../../plugins/plugin.types";

const { SidebarMenu, SidebarMenuItem } = shadcn;

/**
 * Sidebar footer — a flat, always-visible block instead of a dropdown "user menu". There is no
 * real user identity to justify a menu, so the useful bits live inline: the language switcher,
 * the DM account actions (logout) and a small version line. Hidden in the collapsed icon rail
 * (no room); expand the sidebar to reach it.
 */
export function NavUser() {
    const [t] = useTranslation();
    const { dmBaseUrl, version } = useSelector(commonSel.initialSettingsSelector);

    const diUserMenuItems = pluginRegistry.pluginReactComponent<{}>(SUPPORTED_PLUGINS.DI_USER_MENU_ITEMS);
    const diUserMenuFooter = pluginRegistry.pluginReactComponent<UserMenuFooterProps>(
        SUPPORTED_PLUGINS.DI_USER_MENU_FOOTER,
    );
    const languageSwitcher = pluginRegistry.pluginReactComponent<{}>(SUPPORTED_PLUGINS.DI_LANGUAGE_SWITCHER);

    return (
        <SidebarMenu>
            {/* `id` kept for the logout integration test, which used to open a dropdown here. */}
            <SidebarMenuItem
                id="headerUserMenu"
                className="rounded-lg border border-sidebar-border bg-card group-data-[collapsible=icon]:hidden"
            >
                <div className="flex flex-col gap-2 p-2">
                    {languageSwitcher && (
                        <div>
                            <div className="px-1 pb-1 text-xs font-medium text-muted-foreground">
                                {t("navigation.user.language", "Language")}
                            </div>
                            <languageSwitcher.Component />
                        </div>
                    )}
                    {/* DM account actions (logout). Static `<li>` rows via `<Menu>` — gui-elements
                        `MenuItem` in dropdown mode clashes with the shadcn Radix instance, but flat
                        static rows render fine. The plugin emits its own leading divider. */}
                    {!!dmBaseUrl && diUserMenuItems && (
                        <Menu>
                            <diUserMenuItems.Component />
                        </Menu>
                    )}
                    {/* Footer plugin (user identity on DM-authenticated deployments + version with
                        vendor link). Renders plain ToolbarSection/HtmlContentBlock rows — no
                        MenuItems — so no <Menu> wrapper is needed. Hardcoded version line only as
                        the fallback when the plugin is absent. */}
                    {diUserMenuFooter ? (
                        <div className="border-t border-sidebar-border px-1 pt-2 text-xs text-muted-foreground">
                            <diUserMenuFooter.Component version={version} />
                        </div>
                    ) : (
                        version && (
                            <p className="truncate border-t border-sidebar-border px-1 pt-2 text-xs text-muted-foreground">
                                {version} by eccenca GmbH
                            </p>
                        )
                    )}
                </div>
            </SidebarMenuItem>
        </SidebarMenu>
    );
}
