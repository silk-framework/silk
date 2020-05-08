import React, { useEffect, useState } from "react";
import { Toolbar, ToolbarSection } from "@wrappers/index";
import SearchInput from "../SearchBar/SearchInput";

interface IProps {
    textQuery?: string;
    onSearch(textQuery: string): void;
}

/**
 * Simple search widget for the related items widget.
 * @param textQuery The multi-word text query that related items should be filtered by.
 * @param onSearch  The callback to execute if the text query has changed.
 */
export function SimpleSearchBar({ textQuery = "", onSearch }: IProps) {
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

    const clearSearchInput = () => {
        setSearchInput("");
        onSearch("");
    };

    return (
        <Toolbar>
            <ToolbarSection canGrow>
                <SearchInput
                    onFilterChange={handleSearchChange}
                    onEnter={handleSearchEnter}
                    filterValue={searchInput}
                    onClearanceHandler={clearSearchInput}
                />
            </ToolbarSection>
        </Toolbar>
    );
}
