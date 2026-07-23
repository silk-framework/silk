import React from "react";
import { useTranslation } from "react-i18next";
import { Highlighter, TagList, shadcn } from "@eccenca/gui-elements";

import { ISearchResultsServer } from "@ducks/workspace/typings";
import { ItemDepiction } from "../ItemDepiction/ItemDepiction";
import { searchItemTagsRenderer } from "../SearchList/SearchItemTags";
import { QuickSearchItem } from "./quickSearchItem";

const { CommandItem } = shadcn;

interface IProps {
    item: QuickSearchItem;
    /** The current query, for match highlighting. */
    query: string;
    /**
     * Whether to render the item's type badge. Used for the flat "recently viewed" list; when the
     * results are grouped by type the group heading already conveys it, so it is suppressed there.
     */
    showType: boolean;
    onSelect: (item: QuickSearchItem) => void;
}

/**
 * A single result row in the quick search: a muted icon, the label, and the item's badge cluster.
 *
 * The badges are produced by {@link searchItemTagsRenderer} — the exact renderer the search result
 * page uses — so the quick search shares its per-type colour coding (via `ArtefactTag`) and can
 * never disagree with the page behind it.
 */
export function QuickSearchResultItem({ item, query, showType, onSelect }: IProps) {
    const [t] = useTranslation();

    // Adapt to the search-result shape the shared badge renderer expects. Only the fields the
    // renderer reads are populated; `readOnly` is typed as the literal `true`, so map falsy to undefined.
    const asSearchResult: ISearchResultsServer = {
        id: item.key,
        label: item.label,
        type: item.itemType,
        projectId: item.projectId,
        projectLabel: item.projectLabel,
        pluginId: item.pluginId,
        pluginLabel: item.pluginLabel,
        tags: item.tags,
        searchTags: item.searchTags,
        readOnly: item.readOnly ? true : undefined,
    };
    const tags = searchItemTagsRenderer({ item: asSearchResult, searchValue: query, includeType: showType, t });

    return (
        <CommandItem
            // cmdk uses the value for its own bookkeeping; the key is already unique per item.
            value={item.key}
            onSelect={() => onSelect(item)}
            // Row hover/selection fill is handled in quicksearch.css (the shadcn `data-selected:`
            // presence selector otherwise fills every row — see that file).
            className="mb-0.5 cursor-pointer! items-start gap-3 rounded-lg px-2 py-2"
            data-test-id="quick-search-item"
        >
            <span className="flex shrink-0 pt-0.5 text-muted-foreground group-data-[selected]/command-item:text-foreground">
                <ItemDepiction itemType={item.itemType} pluginId={item.pluginId} size={{ small: true }} />
            </span>
            <span className="flex min-w-0 flex-1 flex-col gap-1">
                <span className="truncate text-sm font-medium">
                    <Highlighter label={item.label} searchValue={query} />
                </span>
                {tags.length > 0 && <TagList>{tags}</TagList>}
            </span>
        </CommandItem>
    );
}
