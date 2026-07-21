import { highlighterUtils } from "@eccenca/gui-elements";

import { IRecentlyViewedItem, ISearchResultsServer, Keywords } from "@ducks/workspace/typings";
import { ItemType } from "@ducks/router/operations";

/**
 * A single suggestion in the quick search. Both data sources of the quick search — the preloaded
 * recently viewed items and the results of the workspace search — are normalized into this shape so
 * that rendering, grouping and navigation exist only once.
 */
export interface QuickSearchItem {
    /** Stable key, unique across both data sources. */
    key: string;
    label: string;
    /** Lower case item type, e.g. "workflow". Not necessarily a known `ItemType`. */
    itemType: string;
    pluginId?: string;
    pluginLabel?: string;
    projectId?: string;
    projectLabel?: string;
    readOnly?: boolean;
    tags?: Keywords;
    searchTags?: string[];
    /** Details page of the item, taken from its first item link. Empty if the item has no links. */
    path: string;
    /** Lower cased haystack for the local filter. */
    searchableText: string;
    /** Additional lower cased tokens an item can be found by, but that are never displayed. */
    hiddenSearchTokens: string[];
}

/**
 * Order the type groups appear in. Types the backend reports but that are missing here are appended
 * afterwards, so an unknown type is still shown instead of silently dropped.
 */
const ITEM_TYPE_ORDER: ItemType[] = ["project", "workflow", "transform", "linking", "dataset", "task"];

const searchableText = (parts: Array<string | undefined>): string => parts.filter(Boolean).join(" ").toLowerCase();

const tagLabels = (tags?: Keywords): string[] => (tags ?? []).map((tag) => tag.label);

/** Normalizes a recently viewed item (preload) into a quick search item. */
export const fromRecentlyViewedItem = (item: IRecentlyViewedItem): QuickSearchItem => {
    const label = item.taskLabel || item.taskId || item.projectLabel || item.projectId;
    const projectLabel = item.projectLabel || item.projectId;
    return {
        key: `recent:${item.projectId}:${item.taskId ?? ""}`,
        label,
        itemType: item.itemType.toLowerCase(),
        pluginId: item.pluginId,
        pluginLabel: item.pluginLabel,
        projectId: item.projectId,
        projectLabel: item.projectLabel,
        readOnly: item.readOnly,
        tags: item.tags,
        searchTags: item.searchTags,
        path: item.itemLinks.length ? item.itemLinks[0].path : "",
        searchableText: searchableText([
            label,
            // Only tasks carry a meaningful project context; for a project the label is already included.
            item.taskId ? projectLabel : undefined,
            item.pluginLabel,
            item.itemType,
            ...(item.searchTags ?? []),
            ...tagLabels(item.tags),
        ]),
        hiddenSearchTokens: (item.hiddenSearchTokens ?? []).map((token) => token.toLowerCase()),
    };
};

/** Normalizes a workspace search result into a quick search item. */
export const fromSearchResult = (item: ISearchResultsServer): QuickSearchItem => ({
    key: `search:${item.projectId ?? ""}:${item.id}`,
    label: item.label || item.id,
    itemType: (item.type ?? "").toLowerCase(),
    pluginId: item.pluginId,
    pluginLabel: item.pluginLabel,
    projectId: item.projectId,
    projectLabel: item.projectLabel,
    readOnly: item.readOnly,
    tags: item.tags,
    searchTags: item.searchTags,
    path: item.itemLinks?.length ? item.itemLinks[0].path : "",
    searchableText: searchableText([
        item.label,
        item.id,
        item.description,
        item.projectLabel,
        item.pluginLabel,
        item.type,
        ...(item.searchTags ?? []),
        ...tagLabels(item.tags),
    ]),
    hiddenSearchTokens: [],
});

/**
 * Filters the preloaded items locally. This only bridges the gap until the debounced workspace
 * search answers for the same query — the server result always wins, so that a suggestion can never
 * disagree with what the search result page shows for the same text.
 *
 * Matching mirrors the previous quick search: every search word must appear somewhere in the
 * item's haystack or in one of its hidden tokens.
 */
export const filterQuickSearchItemsLocally = (items: QuickSearchItem[], query: string): QuickSearchItem[] => {
    const searchWords = highlighterUtils.extractSearchWords(query.toLowerCase());
    if (!searchWords.length) {
        return items;
    }
    return items.filter((item) =>
        searchWords.every((word) => item.hiddenSearchTokens.includes(word) || item.searchableText.includes(word)),
    );
};

export interface QuickSearchItemGroup {
    itemType: string;
    items: QuickSearchItem[];
}

/**
 * Groups items by their type in a stable, meaningful order and caps each group, so that a single
 * type cannot push every other type out of the visible list.
 */
export const groupQuickSearchItems = (items: QuickSearchItem[], itemsPerGroup: number): QuickSearchItemGroup[] => {
    const byType = new Map<string, QuickSearchItem[]>();
    items.forEach((item) => {
        const group = byType.get(item.itemType);
        if (group) {
            group.push(item);
        } else {
            byType.set(item.itemType, [item]);
        }
    });

    const knownTypes = ITEM_TYPE_ORDER.filter((itemType) => byType.has(itemType)) as string[];
    const unknownTypes = [...byType.keys()].filter((itemType) => !ITEM_TYPE_ORDER.includes(itemType as ItemType));

    return [...knownTypes, ...unknownTypes].map((itemType) => ({
        itemType,
        items: byType.get(itemType)!.slice(0, itemsPerGroup),
    }));
};
