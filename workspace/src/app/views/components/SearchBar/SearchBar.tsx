import React, { useCallback, useEffect, useState } from "react";
import SearchInput from "./SearchInput";
import SortButton from "../buttons/SortButton";
import { ISortersState } from "@ducks/workspace/typings";

interface IProps {
    textQuery? : string;
    sorters?: ISortersState;
    onSort(sortBy: string): void;
    onApplyFilters({textQuery: string}): void;
}

export function SearchBar({textQuery = '', sorters, onSort, onApplyFilters}: IProps) {
    const [searchInput, setSearchInput] = useState(textQuery);

    useEffect(() => {
        setSearchInput(textQuery);
    }, [textQuery]);

    const handleSearchChange = (e) => {
        setSearchInput(e.target.value);
    };

    const handleSearchEnter = useCallback(() => {
        onApplyFilters({
            textQuery: searchInput
        });
    }, []);

    return (
        <>
            <SearchInput
                onFilterChange={handleSearchChange}
                onEnter={handleSearchEnter}
                filterValue={searchInput}
            />
            {
                !!sorters && <SortButton sortersList={sorters.list} onSort={onSort} activeSort={sorters.applied}/>
            }
        </>
    )
}
