import React, { useCallback, useEffect, useState } from "react";
import { ISortersState } from "@ducks/workspace/typings";
import {
    Toolbar,
    ToolbarSection,
} from "@wrappers/index";
import SearchInput from "./SearchInput";
import SortButton from "../buttons/SortButton";

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
        // when input is empty then apply filter
        if (e.target.value === '' && searchInput) {
            setSearchInput('');
            onApplyFilters({
                textQuery: ''
            });
        } else {
            setSearchInput(e.target.value);
        }
    };

    const handleSearchEnter = () => {
        onApplyFilters({
            textQuery: searchInput
        });
    };

    return (
        <Toolbar>
            <ToolbarSection canGrow>
                <SearchInput
                    onFilterChange={handleSearchChange}
                    onEnter={handleSearchEnter}
                    filterValue={searchInput}
                />
            </ToolbarSection>
            <ToolbarSection>
                {
                    !!sorters && <SortButton sortersList={sorters.list} onSort={onSort} activeSort={sorters.applied}/>
                }
            </ToolbarSection>
        </Toolbar>
    )
}
