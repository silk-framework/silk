import React from "react";
import { useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { HtmlContentBlock, Icon, Menu, MenuDivider, MenuItem, MenuModeProvider, shadcn, Tag } from "@eccenca/gui-elements";
import { commonSel } from "@ducks/common";
import { triggerHotkeyHandler } from "../../shared/HotKeyHandler/HotKeyHandler";
import { CONTEXT_PATH } from "../../../constants/path";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";
import { UserMenuFooterProps } from "../../plugins/plugin.types";
import { ExampleProjectImportMenu } from "../Header/ExampleProjectImportMenu";

const {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    useSidebar,
} = shadcn;

/**
 * User menu at the sidebar footer (shadcn sidebar-07 "nav-user" pattern). Opens a dropdown
 * to the right containing everything the former header toolbar panel offered: language
 * switcher, quick search, keyboard shortcuts, API docs, deprecated plugins, example project
 * import, plugin-provided items (e.g. logout) and the version footer.
 */
export function NavUser() {
    const [t] = useTranslation();
    const { isMobile } = useSidebar();
    const { hotKeys, dmBaseUrl, version } = useSelector(commonSel.initialSettingsSelector);

    const diUserMenuItems = pluginRegistry.pluginReactComponent<{}>(SUPPORTED_PLUGINS.DI_USER_MENU_ITEMS);
    const diUserMenuFooter = pluginRegistry.pluginReactComponent<UserMenuFooterProps>(
        SUPPORTED_PLUGINS.DI_USER_MENU_FOOTER,
    );
    const languageSwitcher = pluginRegistry.pluginReactComponent<{}>(SUPPORTED_PLUGINS.DI_LANGUAGE_SWITCHER);

    return (
        <SidebarMenu>
            <SidebarMenuItem>
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <SidebarMenuButton
                            id="headerUserMenu"
                            size="lg"
                            aria-label={t("navigation.user.open", "Open user menu")}
                            className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
                        >
                            <div className="flex aspect-square size-8 shrink-0 items-center justify-center rounded-lg bg-sidebar-accent text-sidebar-accent-foreground">
                                <Icon name="application-useraccount" title={t("navigation.user.menu", "User menu")} />
                            </div>
                            <div className="grid flex-1 text-left text-sm leading-tight">
                                <span className="truncate font-medium">{t("navigation.user.menu", "User menu")}</span>
                                {version && (
                                    <span className="truncate text-xs text-muted-foreground">{version}</span>
                                )}
                            </div>
                            <Icon name="toggler-caret" small className="ml-auto" />
                        </SidebarMenuButton>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                        className="w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg"
                        side={isMobile ? "bottom" : "right"}
                        align="end"
                        sideOffset={4}
                    >
                        <MenuModeProvider mode="dropdown">
                            <Menu>
                                {languageSwitcher && <languageSwitcher.Component />}
                                <MenuDivider />
                                {hotKeys.quickSearch && (
                                    <MenuItem
                                        text={t("RecentlyViewedModal.title")}
                                        href={"#"}
                                        onClick={(e) => {
                                            if (e) {
                                                e.preventDefault();
                                            }
                                            triggerHotkeyHandler(hotKeys.quickSearch as string);
                                        }}
                                        icon={"operation-search"}
                                        labelElement={
                                            <Tag htmlTitle={`Hotkey: ${hotKeys.quickSearch}`} emphasis="weaker">
                                                {hotKeys.quickSearch}
                                            </Tag>
                                        }
                                    />
                                )}
                                {hotKeys.overview && (
                                    <MenuItem
                                        text={t("header.keyboardShortcutsModal.title")}
                                        href={"#"}
                                        onClick={(e) => {
                                            if (e) {
                                                e.preventDefault();
                                            }
                                            triggerHotkeyHandler(hotKeys.overview as string);
                                        }}
                                        icon="application-hotkeys"
                                        labelElement={
                                            <Tag htmlTitle={`Hotkey: ${hotKeys.overview}`} emphasis="weaker">
                                                {hotKeys.overview}
                                            </Tag>
                                        }
                                    />
                                )}
                                <MenuItem
                                    text={t("common.action.showApiDoc", "API")}
                                    href={CONTEXT_PATH + "/doc/api"}
                                    icon={"application-homepage"}
                                />
                                <MenuItem
                                    text={t("common.action.listDeprecatedPlugins")}
                                    href={CONTEXT_PATH + "/workbench/deprecatedPlugins"}
                                    icon={"state-warning"}
                                />
                                <ExampleProjectImportMenu />
                                {!!dmBaseUrl && diUserMenuItems && <diUserMenuItems.Component />}
                            </Menu>
                        </MenuModeProvider>
                        {(diUserMenuFooter || version) && <DropdownMenuSeparator />}
                        {diUserMenuFooter ? (
                            <diUserMenuFooter.Component version={version} />
                        ) : (
                            version && (
                                <div className="px-2 py-1.5">
                                    <HtmlContentBlock small>{version}</HtmlContentBlock>
                                </div>
                            )
                        )}
                    </DropdownMenuContent>
                </DropdownMenu>
            </SidebarMenuItem>
        </SidebarMenu>
    );
}
