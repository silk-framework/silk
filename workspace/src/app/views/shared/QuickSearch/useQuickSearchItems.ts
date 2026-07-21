import React from "react";
import { useLocation } from "react-router";

import { recentlyViewedItems, requestSearchList } from "@ducks/workspace/requests";
import { ErrorResponse } from "../../../services/fetch/responseInterceptor";
import {
    QuickSearchItem,
    filterQuickSearchItemsLocally,
    fromRecentlyViewedItem,
    fromSearchResult,
} from "./quickSearchItem";

/** How many recently viewed items are preloaded when the quick search opens. */
const PRELOAD_LIMIT = 50;
/** How many results a single workspace search requests. */
const SEARCH_LIMIT = 25;
const SEARCH_DEBOUNCE_MS = 250;

interface QuickSearchItemsResult {
    /** The items to display for the current query. */
    items: QuickSearchItem[];
    query: string;
    setQuery: (query: string) => void;
    /** True while the initial preload is pending, i.e. there is nothing to show yet. */
    loading: boolean;
    /** Set when the preload failed. A failing search does not surface, the local result is kept. */
    error: ErrorResponse | null;
}

/**
 * Supplies the quick search suggestions.
 *
 * Two sources are combined so that the list is both instant and correct:
 *  - The recently viewed items are preloaded once per opening and filtered locally, which gives
 *    suggestions from the first keystroke on without a round trip.
 *  - Every query additionally runs the actual workspace search (debounced). It is the same request
 *    the search result page issues, so once it answers it replaces the local result and the
 *    suggestions can no longer disagree with the page behind them.
 */
export const useQuickSearchItems = (isOpen: boolean): QuickSearchItemsResult => {
    const [query, setQuery] = React.useState<string>("");
    const [preloadedItems, setPreloadedItems] = React.useState<QuickSearchItem[]>([]);
    const [searchResult, setSearchResult] = React.useState<{ query: string; items: QuickSearchItem[] } | null>(null);
    const [loading, setLoading] = React.useState<boolean>(true);
    const [error, setError] = React.useState<ErrorResponse | null>(null);
    const { pathname } = useLocation();
    // Guards against an out of order response overwriting the result of a newer query.
    const currentQuery = React.useRef<string>("");

    React.useEffect(() => {
        if (!isOpen) {
            // Reset, so that re-opening starts from a clean state instead of the previous search.
            setQuery("");
            setSearchResult(null);
            return;
        }

        let cancelled = false;
        const loadPreloadedItems = async () => {
            setError(null);
            setLoading(true);
            try {
                const recentItems = (await recentlyViewedItems()).data;
                if (
                    recentItems.length > 1 &&
                    recentItems[0].itemLinks.length > 0 &&
                    recentItems[0].itemLinks[0].path.endsWith(pathname)
                ) {
                    // The most recent item is the page we are already on, which is never a useful
                    // first suggestion. Swap it with the next one.
                    [recentItems[0], recentItems[1]] = [recentItems[1], recentItems[0]];
                }
                if (!cancelled) {
                    setPreloadedItems(recentItems.slice(0, PRELOAD_LIMIT).map(fromRecentlyViewedItem));
                }
            } catch (ex) {
                if (cancelled) {
                    return;
                }
                if (ex.isFetchError && ex.errorResponse) {
                    setError(ex.errorResponse);
                } else {
                    throw ex;
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        };
        loadPreloadedItems();

        return () => {
            cancelled = true;
        };
        // `pathname` is deliberately not a dependency: it only decides the initial order and must not
        // re-trigger the preload while the quick search is open.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isOpen]);

    React.useEffect(() => {
        const trimmedQuery = query.trim();
        currentQuery.current = trimmedQuery;
        if (!trimmedQuery) {
            setSearchResult(null);
            return;
        }

        const timeout = setTimeout(async () => {
            try {
                const { results } = await requestSearchList({ textQuery: trimmedQuery, limit: SEARCH_LIMIT });
                if (currentQuery.current === trimmedQuery) {
                    setSearchResult({ query: trimmedQuery, items: results.map(fromSearchResult) });
                }
            } catch (ex) {
                // A failing search is not worth an error state in a suggestion list — the locally
                // filtered preload stays visible and the search result page still reports the error.
            }
        }, SEARCH_DEBOUNCE_MS);

        return () => clearTimeout(timeout);
    }, [query]);

    const items = React.useMemo(() => {
        const trimmedQuery = query.trim();
        if (!trimmedQuery) {
            return preloadedItems;
        }
        if (searchResult?.query === trimmedQuery) {
            return searchResult.items;
        }
        return filterQuickSearchItemsLocally(preloadedItems, trimmedQuery);
    }, [query, preloadedItems, searchResult]);

    return { items, query, setQuery, loading, error };
};
