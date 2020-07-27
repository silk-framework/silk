import React, { useEffect, useState } from "react";
import { ISortersState } from "@ducks/workspace/typings";
import { Spacing, Toolbar, ToolbarSection } from "@wrappers/index";
import SearchInput from "./SearchInput";
import SortButton from "../buttons/SortButton";
import { useTranslation } from "react-i18next";

interface IProps {
    textQuery?: string;
    sorters?: ISortersState;

    onSort?(sortBy: string): void;

    onSearch(textQuery: string): void;
    focusOnCreation?: boolean;
}

/** A simple search bar. */
export function SearchBar({ textQuery = "", sorters, onSort, onSearch, focusOnCreation = false }: IProps) {
    const [searchInput, setSearchInput] = useState(textQuery);
    const [t] = useTranslation();

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

    const onClearanceHandler = () => {
        setSearchInput("");
        onSearch("");
    };

    const handleSearchEnter = () => {
        onSearch(searchInput);
    };

    return (
        <Toolbar>
            <ToolbarSection canGrow>
                <SearchInput
                    data-test-id={"search-bar"}
                    focusOnCreation={focusOnCreation}
                    onFilterChange={handleSearchChange}
                    onEnter={handleSearchEnter}
                    filterValue={searchInput}
                    onClearanceHandler={onClearanceHandler}
                    emptySearchInputMessage={t("form.field.searchField", "Enter search term")}
                />
            </ToolbarSection>
            <ToolbarSection>
                {!!sorters?.list.length && onSort && <Spacing size="tiny" vertical />}
                {!!sorters?.list.length && onSort && (
                    <SortButton sortersList={sorters.list} onSort={onSort} activeSort={sorters.applied} />
                )}
            </ToolbarSection>
        </Toolbar>
    );
}
