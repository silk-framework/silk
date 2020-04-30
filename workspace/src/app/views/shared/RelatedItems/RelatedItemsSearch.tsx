import { ISortersState } from "@ducks/workspace/typings";
import React, { useEffect, useState } from "react";
import { Toolbar, ToolbarSection } from "@wrappers/index";
import SearchInput from "../SearchBar/SearchInput";
import SortButton from "../buttons/SortButton";

interface IProps {
    textQuery?: string;
    sorters?: ISortersState;

    onSort?(sortBy: string): void;

    onSearch(textQuery: string): void;
}

export function RelatedItemsSearch({ textQuery = "", onSearch }: IProps) {
    const [searchInput, setSearchInput] = useState(textQuery);

    useEffect(() => {
        setSearchInput(textQuery);
    }, [textQuery]);

    const handleSearchChange = (e) => {
        // when input is empty then apply filter
        if (e.target.value === "" && searchInput) {
            setSearchInput("");
            onSearch("");
        } else {
            setSearchInput(e.target.value);
        }
    };

    const handleSearchEnter = () => {
        onSearch(searchInput);
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
        </Toolbar>
    );
}
