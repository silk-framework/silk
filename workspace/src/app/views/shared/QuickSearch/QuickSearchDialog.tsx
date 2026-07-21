import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { Highlighter, Icon, Notification, Tag, TagList, shadcn } from "@eccenca/gui-elements";

import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { IPageLabels, absolutePageUrl } from "@ducks/router/operations";
import { AppDispatch } from "store/configureStore";
import { DATA_TYPES } from "../../../constants";
import useHotKey from "../HotKeyHandler/HotKeyHandler";
import { ItemDepiction } from "../ItemDepiction/ItemDepiction";
import { Loading } from "../Loading/Loading";
import { ArtefactTag } from "../ArtefactTag";
import { projectTagsRenderer } from "../ProjectTags/ProjectTags";
import { searchTagsRenderer } from "../SearchList/SearchTags";
import { uppercaseFirstChar } from "../../../utils/transformers";
import { QuickSearchItem, groupQuickSearchItems } from "./quickSearchItem";
import { useQuickSearchItems } from "./useQuickSearchItems";

const { Command, CommandDialog, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList, CommandSeparator } =
    shadcn;

/** How many items of a single type are shown. */
const ITEMS_PER_GROUP = 5;
/** How many recently viewed items are listed while nothing has been typed yet. */
const RECENTLY_VIEWED_LIMIT = 8;
const RECENTLY_VIEWED_GROUP = "recentlyViewed";

/**
 * Quick search: opens on the `quickSearch` hotkey (and via the header search bar) and suggests
 * matching workspace items directly below the input, so an item can be reached without going
 * through the search result page first. Pressing "Search in workspace" opens that page instead.
 */
export function QuickSearchDialog() {
    const [isOpen, setIsOpen] = React.useState(false);
    const { t } = useTranslation();
    const dispatch = useDispatch<AppDispatch>();
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);
    const { items, query, setQuery, loading, error } = useQuickSearchItems(isOpen);

    useHotKey({
        hotkey: hotKeys.quickSearch,
        handler: () => {
            setIsOpen(true);
            return false; // prevent default
        },
    });

    const close = React.useCallback(() => {
        setIsOpen(false);
        // On e.g. German keyboards the SHIFT keyup of the hotkey is sent from the quick search input
        // and therefore never reaches the page, which leaves consumers like react-flow believing
        // SHIFT is still held down. Replaying it on the body resolves that.
        const shiftUpEvent = new KeyboardEvent("keyup", { key: "Shift", bubbles: true });
        document.querySelector("body")?.dispatchEvent(shiftUpEvent);
    }, []);

    const goToItem = React.useCallback(
        (item: QuickSearchItem) => {
            close();
            if (!item.path) {
                return;
            }
            const labels: IPageLabels = Object.create(null);
            if (item.itemType === DATA_TYPES.PROJECT) {
                labels.projectLabel = item.label;
            } else {
                labels.taskLabel = item.label;
            }
            labels.itemType = item.itemType;
            dispatch(routerOp.goToPage(item.path, labels));
        },
        [close, dispatch],
    );

    const goToWorkspaceSearch = React.useCallback(() => {
        close();
        dispatch(routerOp.goToPage(absolutePageUrl(`?textQuery=${encodeURIComponent(query.trim())}`)));
    }, [close, dispatch, query]);

    const itemTypeLabel = (itemType: string): string => uppercaseFirstChar(t(`common.dataTypes.${itemType}`, itemType));

    const trimmedQuery = query.trim();
    // Without a query the list shows the recently viewed items, which are about recency and are
    // therefore kept in one flat group. Only a real query groups by item type.
    const groups = React.useMemo(
        () =>
            trimmedQuery
                ? groupQuickSearchItems(items, ITEMS_PER_GROUP)
                : [{ itemType: RECENTLY_VIEWED_GROUP, items: items.slice(0, RECENTLY_VIEWED_LIMIT) }],
        [items, trimmedQuery],
    );

    const quickSearchItem = (item: QuickSearchItem) => (
        <CommandItem
            key={item.key}
            // cmdk uses the value for its own bookkeeping; the key is already unique per item.
            value={item.key}
            onSelect={() => goToItem(item)}
            className="gap-2.5 py-2"
            data-test-id="quick-search-item"
        >
            <ItemDepiction itemType={item.itemType} pluginId={item.pluginId} />
            <span className="flex min-w-0 flex-col gap-0.5">
                <span className="truncate font-medium">
                    <Highlighter label={item.label} searchValue={trimmedQuery} />
                </span>
                <TagList>
                    {item.readOnly && item.itemType === "dataset" && (
                        <Tag>
                            <Icon name="state-locked" small tooltipText={t("common.tooltips.dataset.readOnly")} />
                        </Tag>
                    )}
                    {item.pluginLabel && (
                        <ArtefactTag artefactType={`${item.pluginLabel.toLowerCase()}-node`}>
                            <Highlighter label={item.pluginLabel} searchValue={trimmedQuery} />
                        </ArtefactTag>
                    )}
                    {/* A project is its own context, so only tasks show the project they live in. */}
                    {item.itemType !== DATA_TYPES.PROJECT && (item.projectLabel || item.projectId) && (
                        <Tag emphasis="weak">
                            <Highlighter label={item.projectLabel || item.projectId!} searchValue={trimmedQuery} />
                        </Tag>
                    )}
                    {projectTagsRenderer({ tags: item.tags, query: trimmedQuery })}
                    {searchTagsRenderer({ searchTags: item.searchTags, searchText: trimmedQuery })}
                </TagList>
            </span>
        </CommandItem>
    );

    return (
        <CommandDialog
            open={isOpen}
            onOpenChange={(open) => (open ? setIsOpen(true) : close())}
            title={t("RecentlyViewedModal.title")}
            description={t("RecentlyViewedModal.placeholder")}
            className="top-[12vh] sm:max-w-2xl"
        >
            {/* Filtering happens in `useQuickSearchItems` — against the workspace search, not cmdk's
                own scoring — so cmdk must not filter the items a second time. */}
            <Command shouldFilter={false} data-test-id="quick-search-modal">
                <CommandInput
                    autoFocus
                    value={query}
                    onValueChange={setQuery}
                    placeholder={t("navigation.search.placeholder")}
                    data-test-id="quick-search-input"
                />
                <CommandList>
                    {loading ? (
                        <Loading delay={0} />
                    ) : error ? (
                        <Notification intent="danger">
                            <span>
                                {error.title}
                                {error.detail ? `. Details: ${error.detail}` : ""}
                            </span>
                        </Notification>
                    ) : (
                        <>
                            <CommandEmpty>{t("common.messages.noResults")}</CommandEmpty>
                            {groups.map((group) => (
                                <CommandGroup
                                    key={group.itemType}
                                    heading={
                                        group.itemType === RECENTLY_VIEWED_GROUP
                                            ? t("quickSearch.recentlyViewed", "Recently viewed")
                                            : itemTypeLabel(group.itemType)
                                    }
                                >
                                    {group.items.map(quickSearchItem)}
                                </CommandGroup>
                            ))}
                            {!!trimmedQuery && (
                                <>
                                    <CommandSeparator />
                                    <CommandGroup>
                                        <CommandItem
                                            value="quick-search-workspace-search"
                                            onSelect={goToWorkspaceSearch}
                                            className="gap-2.5 py-2"
                                            data-test-id="quick-search-workspace-search"
                                        >
                                            <Icon name="operation-search" small />
                                            <span className="truncate">
                                                {t("RecentlyViewedModal.globalSearch", { query: trimmedQuery })}
                                            </span>
                                        </CommandItem>
                                    </CommandGroup>
                                </>
                            )}
                        </>
                    )}
                </CommandList>
            </Command>
        </CommandDialog>
    );
}
