import React from "react";
import { ISortersState } from "@ducks/workspace/typings";
import { Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import SearchInput, { ISearchInputProps } from "./SearchInput";
import SortButton from "../buttons/SortButton";
import { useTranslation } from "react-i18next";
import { useSearch } from "../../../hooks/useSearch";

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
    const [t] = useTranslation();

    const emptySearchMessage = otherProps.emptySearchInputMessage
        ? otherProps.emptySearchInputMessage
        : t("form.field.searchField", "Enter search term");

    const { query, onChange, onEnter, onClear } = useSearch(onSearch, textQuery);

    return (
        <Toolbar>
            <ToolbarSection canGrow>
                <SearchInput
                    data-test-id={"search-bar"}
                    focusOnCreation={focusOnCreation}
                    onFilterChange={onChange}
                    onEnter={onEnter}
                    filterValue={query}
                    onClearanceHandler={onClear}
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
