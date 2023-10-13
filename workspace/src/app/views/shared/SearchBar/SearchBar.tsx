import React from "react";
import { ISortersState } from "@ducks/workspace/typings";
import { Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import SearchInput, { ISearchInputProps } from "./SearchInput";
import SortButton from "../buttons/SortButton";
import { useTranslation } from "react-i18next";
import { useInvisibleCharacterCleanUpModal } from "../modals/InvisibleCharacterCleanUpModal";
import { useSearch } from "../../../hooks/useSearch";

/** The omitted properties are only set by this component and not propagated to SearchInput. */
type ISearchBarSearchInputProps = Omit<
    ISearchInputProps,
    "onFilterChange" | "onEnter" | "filterValue" | "onClearanceHandler"
>;

interface IProps extends ISearchBarSearchInputProps {
    textQuery: string;
    sorters?: ISortersState;

    onSort?(sortBy: string): void;

    onSearch(textQuery: string): void;

    /** If defined, the component will warn of queries containing invisible characters that are hard to spot. */
    warnOfInvisibleCharacters?: boolean;

    /** Optional onEnter handler. Default is to refresh the current search. */
    onEnter?: () => any;

    /** optional property to select the first result item when enter key is  pressed */
    selectFirstResultItemOnEnter?: boolean;
}

/** A simple search bar. */
export function SearchBar({
    textQuery,
    sorters,
    onSort,
    onSearch,
    focusOnCreation = false,
    warnOfInvisibleCharacters = true,
    onEnter,
    selectFirstResultItemOnEnter = false,
    ...otherProps
}: IProps) {
    const [t] = useTranslation();
    const {
        query,
        setQuery,
        onChange,
        onEnter: onEnterRefreshSearch,
        onClear,
    } = useSearch({ onSearch, searchQuery: textQuery, selectFirstResultItemOnEnter });

    const emptySearchMessage = otherProps.emptySearchInputMessage
        ? otherProps.emptySearchInputMessage
        : t("form.field.searchField", "Enter search term");

    const { iconButton, modalElement, invisibleCharacterWarning } = useInvisibleCharacterCleanUpModal({
        inputString: query,
        setString: setQuery,
        callbackDelay: 200,
    });

    return (
        <Toolbar>
            <ToolbarSection canGrow>
                {modalElement}
                {iconButton}
                <SearchInput
                    data-test-id={"search-bar"}
                    focusOnCreation={focusOnCreation}
                    onFilterChange={onChange}
                    onEnter={onEnter ? onEnter : onEnterRefreshSearch}
                    filterValue={query}
                    onClearanceHandler={onClear}
                    emptySearchInputMessage={emptySearchMessage}
                    invisibleCharacterWarning={invisibleCharacterWarning}
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
