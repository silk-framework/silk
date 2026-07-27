import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { cn, Icon, shadcn } from "@eccenca/gui-elements";
import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { AppDispatch } from "store/configureStore";
import { triggerHotkeyHandler } from "../../shared/HotKeyHandler/HotKeyHandler";
import { useGridBoardReset } from "../../shared/GridBoard";
import { CONTEXT_PATH, SERVE_PATH } from "../../../constants/path";
import { APPLICATION_DOCUMENTATION_SERVICE_URL } from "../../../constants/base";
import { ExampleProjectImportMenu } from "./ExampleProjectImportMenu";
import { headerActionButtonClass, headerMenuElevation } from "./headerChrome";

const { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuTrigger } = shadcn;

/**
 * Header "Help" menu: the keyboard-shortcuts dialog, documentation/API links, the GridBoard layout
 * reset (when the current page has a board), deprecated-plugins route and the example-project import.
 * Rendered from the header chrome as a ghost icon-button that opens a right-aligned dropdown.
 */
export function HelpMenu() {
    const dispatch = useDispatch<AppDispatch>();
    const [t] = useTranslation();
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);
    const gridBoard = useGridBoardReset();

    // The keyboard-shortcuts help dialog listens for the `overview` hotkey; the Help menu triggers
    // that same handler so mouse and keyboard reach the identical surface.
    const openKeyboardShortcuts = React.useCallback(() => {
        if (hotKeys.overview) {
            triggerHotkeyHandler(hotKeys.overview as string);
        }
    }, [hotKeys.overview]);

    const documentationUrl = APPLICATION_DOCUMENTATION_SERVICE_URL();

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <button type="button" aria-label={t("common.action.help", "Help")} className={headerActionButtonClass}>
                    <Icon name="item-question" small />
                </button>
            </DropdownMenuTrigger>
            {/* `w-max` overrides the shadcn base `w-(--radix-dropdown-menu-trigger-width)` (the trigger is
                the 36px icon button), so the menu sizes to its widest row instead of being pinned to
                `min-w-52` and wrapping long labels onto two lines. */}
            <DropdownMenuContent align="end" sideOffset={4} className={cn("w-max min-w-52", headerMenuElevation)}>
                <DropdownMenuLabel>{t("common.action.help", "Help")}</DropdownMenuLabel>
                <DropdownMenuItem onClick={openKeyboardShortcuts}>
                    <Icon name="item-legend" small />
                    {t("header.keyboardShortcutsModal.title", "Keyboard shortcuts")}
                </DropdownMenuItem>
                {!!documentationUrl && (
                    <DropdownMenuItem onClick={() => window.open(documentationUrl, "_blank", "noopener,noreferrer")}>
                        <Icon name="item-info" small />
                        {t("navigation.side.dm.documentation", "Documentation")}
                    </DropdownMenuItem>
                )}
                {gridBoard.hasBoard && (
                    <DropdownMenuItem onClick={gridBoard.reset} data-test-id="help-reset-layout">
                        <Icon name="operation-undo" small />
                        {t("GridBoard.resetLayout", "Reset layout")}
                    </DropdownMenuItem>
                )}
                {/* Moved here from the sidebar user menu (they are app/help actions). API is a
                    backend-served Swagger doc (outside the SPA), so it stays a real `<a>` full-page link;
                    `text-inherit` keeps it on the theme-aware menu foreground instead of the global
                    `a { color: var(--primary) }` accent-blue. */}
                <DropdownMenuItem asChild>
                    <a href={CONTEXT_PATH + "/doc/api"} className="text-inherit">
                        <Icon name="application-homepage" small />
                        {t("common.action.showApiDoc", "API")}
                    </a>
                </DropdownMenuItem>
                {/* Deprecated plugins is an in-app SPA route registered at
                    `SERVE_PATH + "/deprecatedPlugins"` (RouterOutlet wraps every route path in
                    `getFullRoutePath`). Navigate via the router with the full SERVE_PATH path — identical
                    to how the sidebar routes to the global Activities page — instead of a hard `<a>`
                    reload that cold-boots the app off this route. */}
                <DropdownMenuItem
                    data-test-id="help-deprecated-plugins"
                    onClick={() => dispatch(routerOp.goToPage(SERVE_PATH + "/deprecatedPlugins"))}
                >
                    <Icon name="state-warning" small />
                    {t("common.action.listDeprecatedPlugins")}
                </DropdownMenuItem>
                <ExampleProjectImportMenu />
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
