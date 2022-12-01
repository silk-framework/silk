import { ISortersState } from "@ducks/workspace/typings";
import { Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import SortButton from "../buttons/SortButton";
import SearchInput, { ISearchInputProps } from "./SearchInput";

/** The omitted properties are only set by this component and not propagated to SearchInput. */
type ISearchBarSearchInputProps = Omit<
    ISearchInputProps,
    "onFilterChange" | "onEnter" | "filterValue" | "onClearanceHandler"
>;

interface IProps extends ISearchBarSearchInputProps {
    textQuery?: string;
    sorters?: ISortersState;

    onSort?(sortBy: string): void;

    onSearch(textQuery: string): void;
}

/** A simple search bar. */
export function SearchBar({
    textQuery = "",
    sorters,
    onSort,
    onSearch,
    focusOnCreation = false,
    ...otherProps
}: IProps) {
    const [searchInput, setSearchInput] = useState(textQuery);
    const [t] = useTranslation();

    const emptySearchMessage = otherProps.emptySearchInputMessage
        ? otherProps.emptySearchInputMessage
        : t("form.field.searchField", "Enter search term");
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
                    emptySearchInputMessage={emptySearchMessage}
                    {...otherProps}
                />
            </ToolbarSection>
            {!!sorters && !!sorters.list.length && onSort && (
                <ToolbarSection>
                    <Spacing size="tiny" vertical />
                    <SortButton sortersList={sorters.list} onSort={onSort} activeSort={sorters.applied} />
                </ToolbarSection>
            )}
        </Toolbar>
    );
}
