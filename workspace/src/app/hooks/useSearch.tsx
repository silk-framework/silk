import { routerOp } from "@ducks/router";
import { IPageLabels } from "@ducks/router/operations";
import { workspaceSel } from "@ducks/workspace";
import { DATA_TYPES } from "../constants";
import { debounce } from "lodash";
import React from "react";
import { useDispatch, useSelector } from "react-redux";

interface SearchHandlerReturnType {
    isSearching: boolean;
    setQuery: (textQuery: string) => void;
    query: string;
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onClear: () => void;
    onEnter: () => void;
}

export const useSearch = ({
    onSearch,
    searchQuery,
    delay = 500,
    selectFirstResultItemOnEnter,
}: {
    onSearch: (query: string) => void;
    searchQuery: string;
    delay?: number;
    selectFirstResultItemOnEnter: boolean;
}): SearchHandlerReturnType => {
    const [query, setQuery] = React.useState<string>(searchQuery);
    const [isSearching, setIsSearching] = React.useState<boolean>(false);

    const dispatch = useDispatch();
    const data = useSelector(workspaceSel.resultsSelector);
    const dataArrayRef = React.useRef(data);

    React.useEffect(() => {
        dataArrayRef.current = data;
    }, [data]);

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
        setQuery(inputValue);
        !inputValue.length && debouncedSearch("");
    }, []);

    const onClear = React.useCallback(() => {
        setQuery("");
        onSearch("");
    }, []);

    const onEnter = React.useCallback(() => {
        if (selectFirstResultItemOnEnter) {
            const firstResult = dataArrayRef.current[0];
            const labels: IPageLabels = {};
            if (firstResult && firstResult.itemLinks?.length) {
                if (firstResult.type === DATA_TYPES.PROJECT) {
                    labels.projectLabel = firstResult.label;
                } else {
                    labels.taskLabel = firstResult.label;
                }
                labels.itemType = firstResult.type;
                onSearch("");
                setTimeout(() => dispatch(routerOp.goToPage(firstResult.itemLinks![0].path, labels)), 0);
            }
        } else {
            onSearch(query);
        }
    }, [query]);

    return { isSearching, setQuery, query, onChange, onClear, onEnter };
};
