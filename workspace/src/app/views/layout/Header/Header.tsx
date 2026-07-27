import React from "react";
import { useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { cn, Icon, shadcn } from "@eccenca/gui-elements";
import { commonSel } from "@ducks/common";
import { triggerHotkeyHandler } from "../../shared/HotKeyHandler/HotKeyHandler";
import { NotificationsMenu } from "../../shared/ApplicationNotifications/NotificationsMenu";
import { APPLICATION_CORPORATION_NAME, APPLICATION_NAME, APPLICATION_SUITE_NAME } from "../../../constants/base";
import { APP_VIEWHEADER_ID } from "../../shared/PageHeader/PageHeader";
import { useKeyboardHeaderShortcuts } from "./useKeyBoardHeaderShortcuts";
import { CreateSplitButton } from "./CreateSplitButton";
import { HelpMenu } from "./HelpMenu";
import { headerActionButtonClass } from "./headerChrome";

const { Kbd, SidebarTrigger } = shadcn;

/**
 * Sticky header inside the `SidebarInset` (shadcn sidebar-07 pattern), restyled to the 2A brand
 * chrome: sidebar trigger, the page-header portal target (breadcrumbs + title, filled per page via
 * `PageHeader`), a global quick-search box, and the global actions (split create, help,
 * notifications). Each action is its own widget; this shell just wires them together.
 */
export function Header() {
    const [t] = useTranslation();
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);

    //general keyboard shortcuts
    useKeyboardHeaderShortcuts();

    const openQuickSearch = React.useCallback(() => {
        if (hotKeys.quickSearch) {
            triggerHotkeyHandler(hotKeys.quickSearch as string);
        }
    }, [hotKeys.quickSearch]);

    const brandingSuffix =
        APPLICATION_CORPORATION_NAME() || APPLICATION_SUITE_NAME()
            ? ` @ ${APPLICATION_CORPORATION_NAME()} ${APPLICATION_SUITE_NAME()}`
            : "";

    return (
        <header
            className={cn(
                // 60px brand chrome height (design 2A) via the shared `--header-height` token. The fixed
                // notifications panel in `NotificationsMenu` is pinned to this height (top-[var(...)]).
                "sticky top-0 z-[var(--z-app-header)] flex h-[var(--header-height)] shrink-0 items-center gap-2 border-b border-border bg-card",
                // notifications-over-modals elevation, see `useApplicationHeaderOverModals`
                "[.eccgui-application--topheader_&]:z-[var(--z-app-header-over-modals)]",
            )}
            aria-label={`${APPLICATION_NAME()}${brandingSuffix}`}
        >
            <div className="flex w-full min-w-0 items-center gap-2 px-4">
                {/* Collapse / expand the navigation sidebar (also Cmd/Ctrl+B). */}
                <SidebarTrigger
                    className="-ml-1 size-9 shrink-0 rounded-lg text-foreground [&_svg:not([class*='size-'])]:size-4"
                    aria-label={t("navigation.side.toggle", "Toggle navigation")}
                    title={t("navigation.side.toggle", "Toggle navigation")}
                />
                <div className="mr-1 h-5 w-px shrink-0 bg-border" aria-hidden />

                {/* Breadcrumb / page-title area: each page fills this portal via `PageHeader`, whose
                    `BreadcrumbList` shows the trail to the current page. */}
                <div id={APP_VIEWHEADER_ID} className="flex min-w-0 grow flex-col justify-center" />

                {hotKeys.quickSearch && (
                    <>
                        <button
                            type="button"
                            onClick={openQuickSearch}
                            aria-label={t("quickSearch.title", "Quick search")}
                            className={cn(
                                "hidden shrink-0 cursor-pointer items-center gap-2.5 rounded-lg border border-input bg-muted/60 px-3",
                                "h-9 w-[340px] max-w-[34vw] text-sm text-muted-foreground transition-colors lg:flex",
                                "hover:border-ring/40 hover:bg-card focus-visible:border-ring focus-visible:outline-none",
                            )}
                        >
                            <Icon name="operation-search" small />
                            <span className="flex-1 truncate text-left">
                                {t("navigation.search.placeholder", "Search or jump to anything…")}
                            </span>
                            <Kbd className="border border-border bg-card">
                                {(hotKeys.quickSearch as string).toUpperCase()}
                            </Kbd>
                        </button>
                        {/* Below `lg` the search box above is hidden; this icon-only button keeps quick
                            search reachable by mouse (the '/' hotkey alone is undiscoverable). */}
                        <button
                            type="button"
                            onClick={openQuickSearch}
                            aria-label={t("quickSearch.title", "Quick search")}
                            title={t("quickSearch.title", "Quick search")}
                            className={cn(headerActionButtonClass, "shrink-0 lg:hidden")}
                        >
                            <Icon name="operation-search" small />
                        </button>
                    </>
                )}

                <div className="ml-auto flex shrink-0 items-center gap-2">
                    <CreateSplitButton />
                    <HelpMenu />
                    <NotificationsMenu />
                </div>
            </div>
        </header>
    );
}
