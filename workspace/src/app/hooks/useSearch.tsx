import { debounce } from "lodash";
import React from "react";

interface SearchHandlerReturnType {
    isSearching: boolean;
    setQuery: (textQuery: string) => void;
    query: string;
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onClear: () => void;
    onEnter: () => void;
}

export const useSearch = (onSearch: (query: string) => void, delay = 500): SearchHandlerReturnType => {
    const [query, setQuery] = React.useState<string>("");
    const [isSearching, setIsSearching] = React.useState<boolean>(false);

    const debouncedSearch = React.useCallback(
        debounce(async (query) => {
            setIsSearching(true);
            try {
                await onSearch(query);
            } catch (err) {
            } finally {
                setIsSearching(false);
            }
        }, delay),
        []
    );

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
        if (inputValue.length) {
            setQuery(inputValue);
        } else {
            setQuery("");
            onSearch("");
        }
    }, []);

    const onClear = React.useCallback(() => {
        setQuery("");
        onSearch("");
    }, []);

    const onEnter = React.useCallback(() => {
        onSearch(query);
    }, [query]);

    return { isSearching, setQuery, query, onChange, onClear, onEnter };
};
