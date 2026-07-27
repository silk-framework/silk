import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { cn, Icon, shadcn } from "@eccenca/gui-elements";
import { commonOp, commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { triggerHotkeyHandler } from "../../shared/HotKeyHandler/HotKeyHandler";
import { NotificationsMenu } from "../../shared/ApplicationNotifications/NotificationsMenu";
import {
    APPLICATION_CORPORATION_NAME,
    APPLICATION_DOCUMENTATION_SERVICE_URL,
    APPLICATION_NAME,
    APPLICATION_SUITE_NAME,
} from "../../../constants/base";
import { APP_VIEWHEADER_ID } from "../../shared/PageHeader/PageHeader";
import { useKeyboardHeaderShortcuts } from "./useKeyBoardHeaderShortcuts";
import { useGridBoardReset } from "../../shared/GridBoard";
import { CONTEXT_PATH, SERVE_PATH } from "../../../constants/path";
import { ExampleProjectImportMenu } from "./ExampleProjectImportMenu";
import { AppDispatch } from "store/configureStore";

const {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuTrigger,
    Kbd,
    SidebarTrigger,
} = shadcn;

/**
 * Split "Create" action (2A design): the brand-orange primary opens the create dialog for all
 * artefact types; the caret opens a dropdown that pre-selects a category. Both dispatch
 * `setSelectedArtefactDType`, which sets the category and opens the dialog in one reducer.
 * Each category carries its artefact icon so the menu reads at a glance.
 */
const createTypes: Array<{ dtype: string; label: string; defaultLabel: string; icon: string }> = [
    { dtype: "project", label: "navigation.side.di.projects", defaultLabel: "Project", icon: "artefact-project" },
    { dtype: "dataset", label: "navigation.side.di.datasets", defaultLabel: "Dataset", icon: "artefact-dataset" },
    { dtype: "workflow", label: "navigation.side.di.workflows", defaultLabel: "Workflow", icon: "artefact-workflow" },
    {
        dtype: "transform",
        label: "widget.Filterbar.subsections.valueLabels.itemType.transform",
        defaultLabel: "Transform",
        icon: "artefact-transform",
    },
    {
        dtype: "linking",
        label: "widget.Filterbar.subsections.valueLabels.itemType.linking",
        defaultLabel: "Linking",
        icon: "artefact-linking",
    },
    {
        dtype: "task",
        label: "widget.Filterbar.subsections.valueLabels.itemType.task",
        defaultLabel: "Task",
        icon: "artefact-task",
    },
];

/**
 * Ghost icon-button style shared by the header chrome actions (Help, Notifications) so they read as
 * one 36px control group aligned with the Create button. Kept in sync with the identical string in
 * `NotificationsMenu.tsx`; icons inside are rendered `small` (16px) to match the dense header chrome.
 */
export const headerActionButtonClass = cn(
    "relative flex size-9 cursor-pointer items-center justify-center rounded-lg text-foreground transition-colors",
    "hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40",
    "data-[state=open]:bg-muted",
);

/**
 * Menus opened from header triggers must sit above the header chrome. Radix copies the content's
 * computed z-index onto its popper wrapper, and the header is `z-[8000]` (8002 over modals), so the
 * default shadcn `z-50` content is painted behind the header where the menu overlaps the header band.
 * This lifts header menus above that so they connect to their trigger instead of reading as detached.
 */
const headerMenuElevation = "z-[8100]";

/**
 * Sticky header inside the `SidebarInset` (shadcn sidebar-07 pattern), restyled to the 2A brand
 * chrome: sidebar trigger, the page-header portal target (breadcrumbs + title, filled per page via
 * `PageHeader`), a global quick-search box, and the global actions (split create, notifications).
 */
export function Header() {
    const dispatch = useDispatch<AppDispatch>();
    const [t] = useTranslation();
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);
    const gridBoard = useGridBoardReset();

    //general keyboard shortcuts
    useKeyboardHeaderShortcuts();

    const openCreateDialog = React.useCallback(
        (dtype: string) => {
            dispatch(commonOp.setSelectedArtefactDType(dtype));
        },
        [dispatch],
    );

    const openQuickSearch = React.useCallback(() => {
        if (hotKeys.quickSearch) {
            triggerHotkeyHandler(hotKeys.quickSearch as string);
        }
    }, [hotKeys.quickSearch]);

    // The keyboard-shortcuts help dialog listens for the `overview` hotkey; the Help menu triggers
    // that same handler so mouse and keyboard reach the identical surface.
    const openKeyboardShortcuts = React.useCallback(() => {
        if (hotKeys.overview) {
            triggerHotkeyHandler(hotKeys.overview as string);
        }
    }, [hotKeys.overview]);

    const documentationUrl = APPLICATION_DOCUMENTATION_SERVICE_URL();

    // The caret is only ~32px wide, so a menu anchored to it (align="end") would be much wider than
    // its anchor and spill left past the Create button, reading as detached. Measuring the whole
    // split-button on open and using it as the menu's min-width makes the menu match the button's
    // footprint: right edges stay flush (align="end") and the left edges line up too.
    //
    // Anchoring gotcha: Radix copies the *content's* computed z-index onto its fixed popper wrapper.
    // The shadcn content ships `z-50`, but this header is `z-[8000]` (elevated to 8002 over modals),
    // so the top slice of any header menu — the part that overlaps the header band before it clears
    // the bottom edge — was painted *behind* the header, making the menu read as detached from the
    // button and "stuck to the header". `headerMenuElevation` lifts header menus above the chrome.
    const createSplitButtonRef = React.useRef<HTMLDivElement>(null);
    const [createMenuMinWidth, setCreateMenuMinWidth] = React.useState<number | undefined>(undefined);

    const brandingSuffix =
        APPLICATION_CORPORATION_NAME() || APPLICATION_SUITE_NAME()
            ? ` @ ${APPLICATION_CORPORATION_NAME()} ${APPLICATION_SUITE_NAME()}`
            : "";

    return (
        <header
            className={cn(
                // 60px brand chrome height (design 2A). The fixed notifications panel/action are
                // pinned to this height in `NotificationsMenu` (top-15 / size-15).
                "sticky top-0 z-[8000] flex h-15 shrink-0 items-center gap-2 border-b border-border bg-card",
                // notifications-over-modals elevation, see `useApplicationHeaderOverModals`
                "[.eccgui-application--topheader_&]:z-[8002]",
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
                    <div ref={createSplitButtonRef} className="flex items-stretch">
                        <button
                            type="button"
                            data-test-id="create-item-btn"
                            onClick={() => openCreateDialog("all")}
                            className={cn(
                                "flex h-9 cursor-pointer items-center gap-1.5 rounded-l-lg bg-brand pl-3 pr-3.5 text-sm font-semibold",
                                "text-brand-foreground transition-[filter] hover:brightness-95 focus-visible:outline-none",
                                "focus-visible:ring-2 focus-visible:ring-brand/50",
                            )}
                        >
                            <Icon name="item-add-artefact" small />
                            <span>{t("common.action.create", "Create")}</span>
                        </button>
                        <DropdownMenu
                            onOpenChange={(open) => {
                                if (open && createSplitButtonRef.current) {
                                    setCreateMenuMinWidth(createSplitButtonRef.current.offsetWidth);
                                }
                            }}
                        >
                            <DropdownMenuTrigger asChild>
                                <button
                                    type="button"
                                    aria-label={t("common.action.create", "Create") + " …"}
                                    className={cn(
                                        "flex h-9 w-8 cursor-pointer items-center justify-center rounded-r-lg border-l border-brand-foreground/25",
                                        "bg-brand text-brand-foreground transition-[filter] hover:brightness-95",
                                        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/50",
                                    )}
                                >
                                    <Icon name="toggler-caretdown" small />
                                </button>
                            </DropdownMenuTrigger>
                            {/* Brand-orange menu so the caret dropdown reads as one control with the
                                Create button. On the orange surface the text/icons are the dark
                                `brand-foreground`; the highlighted (hover/keyboard) row inverts to a
                                solid `brand-foreground` fill with the label + icons flipped back to the
                                orange `brand` — a high-contrast active state. The label is a text node
                                so it just inherits the row's `focus:text-brand`, but the icon is an
                                `<svg>` element, and shadcn's base recolours descendant svgs on focus via
                                `not-data-[variant=destructive]:focus:**:text-accent-foreground`. We must
                                reuse that EXACT variant prefix (`not-data-[variant=destructive]:focus:**:`)
                                so tailwind-merge replaces shadcn's class outright — a plain `focus:**:`
                                is a different variant set, stays alongside it, and loses on specificity. */}
                            <DropdownMenuContent
                                align="end"
                                sideOffset={4}
                                style={createMenuMinWidth ? { minWidth: createMenuMinWidth } : undefined}
                                className={cn(
                                    "min-w-44 bg-brand text-brand-foreground ring-brand-foreground/15",
                                    headerMenuElevation,
                                )}
                            >
                                {createTypes.map((type) => (
                                    <DropdownMenuItem
                                        key={type.dtype}
                                        onClick={() => openCreateDialog(type.dtype)}
                                        className="text-brand-foreground focus:bg-brand-foreground focus:text-brand not-data-[variant=destructive]:focus:**:text-brand"
                                    >
                                        <Icon name={[type.icon]} small className="text-brand-foreground" />
                                        {t(type.label, type.defaultLabel)}
                                    </DropdownMenuItem>
                                ))}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <button
                                type="button"
                                aria-label={t("common.action.help", "Help")}
                                className={headerActionButtonClass}
                            >
                                <Icon name="item-question" small />
                            </button>
                        </DropdownMenuTrigger>
                        {/* `w-max` overrides the shadcn base `w-(--radix-dropdown-menu-trigger-width)`
                            (the trigger is the 36px icon button), so the menu sizes to its widest row
                            instead of being pinned to `min-w-52` and wrapping long labels onto two lines. */}
                        <DropdownMenuContent
                            align="end"
                            sideOffset={4}
                            className={cn("w-max min-w-52", headerMenuElevation)}
                        >
                            <DropdownMenuLabel>{t("common.action.help", "Help")}</DropdownMenuLabel>
                            <DropdownMenuItem onClick={openKeyboardShortcuts}>
                                <Icon name="item-legend" small />
                                {t("header.keyboardShortcutsModal.title", "Keyboard shortcuts")}
                            </DropdownMenuItem>
                            {!!documentationUrl && (
                                <DropdownMenuItem
                                    onClick={() => window.open(documentationUrl, "_blank", "noopener,noreferrer")}
                                >
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
                            {/* Moved here from the sidebar user menu (they are app/help actions).
                                API is a backend-served Swagger doc (outside the SPA), so it stays a real
                                `<a>` full-page link; `text-inherit` keeps it on the theme-aware menu
                                foreground instead of the global `a { color: var(--primary) }` accent-blue. */}
                            <DropdownMenuItem asChild>
                                <a href={CONTEXT_PATH + "/doc/api"} className="text-inherit">
                                    <Icon name="application-homepage" small />
                                    {t("common.action.showApiDoc", "API")}
                                </a>
                            </DropdownMenuItem>
                            {/* Deprecated plugins is an in-app SPA route registered at
                                `SERVE_PATH + "/deprecatedPlugins"` (RouterOutlet wraps every route path in
                                `getFullRoutePath`). Navigate via the router with the full SERVE_PATH path —
                                identical to how the sidebar routes to the global Activities page — instead
                                of a hard `<a>` reload that cold-boots the app off this route. */}
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

                    <NotificationsMenu />
                </div>
            </div>
        </header>
    );
}
