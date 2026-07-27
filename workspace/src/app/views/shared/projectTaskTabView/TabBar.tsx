import React from "react";
import { cn } from "@eccenca/gui-elements";
import { IItemLink } from "@ducks/shared/typings";
import { IProjectTaskView } from "../../plugins/PluginRegistry";

type TabEntry = Partial<IProjectTaskView & IItemLink>;

interface TabBarProps {
    /** All tab entries (task views and item links), in display order. */
    tabs: TabEntry[];
    /** The currently selected tab: an item link object or a view ID string. */
    selectedTab: IItemLink | string | undefined;
    /** aria-label for the tablist (already translated). */
    ariaLabel: string;
    /** Builds the in-app bookmark href for a given tab route id. */
    bookmarkHref: (tabRouteId: string) => string;
    /** Translates a (possibly dynamic, backend-provided) tab label. */
    tLabel: (label: string) => string;
    /** Tooltip for item links that open in a new browser tab (already translated). */
    openInNewTabTooltip: string;
    /** Invoked for an in-app tab switch. Not called for open-in-new-tab links. */
    onSelect: (tabItem: string | IItemLink) => void;
}

/**
 * Shadcn-styled segmented tab bar. Presentational only — the anchors keep the existing
 * bookmark-href navigation, unsaved-changes prompt via {@link onSelect}, open-in-new-tab item links
 * and data-test-ids. The active tab is expressed with `aria-selected`/styling instead of the old
 * disabled grey.
 */
const TabBar: React.FC<TabBarProps> = ({
    tabs,
    selectedTab,
    ariaLabel,
    bookmarkHref,
    tLabel,
    openInNewTabTooltip,
    onSelect,
}) => {
    // Only used to disambiguate the data-test-id of unlabeled item-link iframes.
    let tabNr = 1;
    return (
        <div
            role="tablist"
            aria-label={ariaLabel}
            className={cn(
                "inline-flex h-8 w-fit items-center justify-center rounded-lg",
                "bg-muted p-[3px] text-muted-foreground",
            )}
        >
            {tabs.map((tabItem) => {
                const openInNewTab = tabItem.openInNewTab;
                const isActive =
                    !!selectedTab && (tabItem.path ?? tabItem.id) === ((selectedTab as any)?.path ?? selectedTab);
                return (
                    <a
                        role="tab"
                        aria-selected={isActive}
                        data-test-id={"taskView-" + (tabItem.id ?? `-iframe-${tabNr++}`)}
                        key={tabItem.id ?? tabItem.path}
                        onClick={(e) => {
                            if (isActive) {
                                e.preventDefault();
                                return;
                            }
                            if (!openInNewTab) {
                                e.preventDefault();
                                onSelect(tabItem.id ?? (tabItem as IItemLink));
                            }
                        }}
                        title={openInNewTab ? openInNewTabTooltip : ""}
                        href={openInNewTab ? tabItem.path : bookmarkHref(tabItem.id ?? "")}
                        target={openInNewTab ? "_blank" : undefined}
                        rel="noopener noreferrer"
                        className={cn(
                            "relative inline-flex h-[calc(100%-1px)] items-center justify-center gap-1.5",
                            "cursor-pointer rounded-md border border-transparent px-1.5 py-0.5",
                            "text-sm font-medium whitespace-nowrap no-underline transition-all",
                            "hover:no-underline focus-visible:outline-1 focus-visible:outline-ring",
                            "[&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
                            // `--background` equals `--muted` in this theme, so an active
                            // `bg-background` was invisible against the muted tab bar. Use the
                            // card surface (white in light, a lighter slate in dark) + shadow so
                            // the selected tab reads as a distinct raised pill.
                            isActive
                                ? "bg-card text-foreground shadow-sm dark:border-input dark:bg-input"
                                : "text-muted-foreground hover:text-foreground",
                        )}
                    >
                        {tLabel(tabItem.label as string)}
                    </a>
                );
            })}
        </div>
    );
};

export default TabBar;
