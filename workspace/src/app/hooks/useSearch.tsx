import { debounce } from "lodash";
import React from "react";

interface SearchHandlerReturnType {
    isSearching: boolean;
    setQuery: (textQuery: string) => void;
    query: string;
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onClear: () => void;
    onEnter: () => void;
    /** Returns if there is a pending search. */
    searchPending: () => boolean;
}

export const useSearch = ({
    onSearch,
    searchQuery,
    delay = 500,
}: {
    onSearch: (query: string) => void;
    searchQuery: string;
    delay?: number;
}): SearchHandlerReturnType => {
    const [query, setQuery] = React.useState<string>(searchQuery);
    const [isSearching, setIsSearching] = React.useState<boolean>(false);
    const searchState = React.useRef<{ currentQueryString: string; finishedQuery: string | undefined }>({
        currentQueryString: searchQuery,
        finishedQuery: undefined,
    });

    React.useEffect(() => {
        setQuery(searchQuery);
    }, [searchQuery]);

    const debouncedSearch = React.useCallback(
        debounce(async (query) => {
            setIsSearching(true);
            try {
                await onSearch(query);
            } catch (err) {
            } finally {
                setIsSearching(false);
                searchState.current.finishedQuery = query;
            }
        }, delay),
        []
    );

    const searchPending = React.useCallback(() => {
        return searchState.current.currentQueryString !== searchState.current.finishedQuery;
    }, []);

    React.useEffect(() => {
        let shouldCancel = false;

        if (!shouldCancel && query) {
            debouncedSearch(query);
        }

        return () => {
            shouldCancel = true;
        };
    }, [query]);

    const onChange = React.useCallback((e) => {
        const inputValue = e.target.value;
        searchState.current.currentQueryString = inputValue;
        setQuery(inputValue);
        !inputValue.length && debouncedSearch("");
    }, []);

    const onClear = React.useCallback(() => {
        setQuery("");
        onSearch("");
    }, []);

    const onEnter = React.useCallback(() => {
        onSearch(query);
    }, [query]);

    return { isSearching, setQuery, query, onChange, onClear, onEnter, searchPending };
};
