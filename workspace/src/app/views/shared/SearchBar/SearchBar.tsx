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

    onSearch(textQuery: string): void;

    /** If defined, the component will warn of queries containing invisible characters that are hard to spot. */
    warnOfInvisibleCharacters?: boolean;

    /** Optional onEnter handler. Default is to refresh the current search. */
    onEnter?: () => any;

    /** Callback to signal when there is a pending (maybe not even executed search). */
    disableEnterDuringPendingSearch?: boolean;
}

/** A simple search bar. */
export function SearchBar({
    textQuery,
    sorters,
    onSearch,
    focusOnCreation = false,
    warnOfInvisibleCharacters = true,
    onEnter,
    disableEnterDuringPendingSearch = false,
    ...otherProps
}: IProps) {
    const [t] = useTranslation();
    const {
        query,
        setQuery,
        onChange,
        onEnter: onEnterRefreshSearch,
        onClear,
        searchPending,
    } = useSearch({ onSearch, searchQuery: textQuery });

    const emptySearchMessage = otherProps.emptySearchInputMessage
        ? otherProps.emptySearchInputMessage
        : t("form.field.searchField", "Enter search term");

    const { iconButton, modalElement, invisibleCharacterWarning } = useInvisibleCharacterCleanUpModal({
        inputString: query,
        setString: setQuery,
        callbackDelay: 200,
    });

    const onEnterExtended = React.useCallback(() => {
        if (disableEnterDuringPendingSearch && searchPending()) {
            // Enter has no effect if there is still a pending search
            return;
        }
        onEnter ? onEnter() : onEnterRefreshSearch();
    }, [onEnterRefreshSearch, onEnter]);

    return (
        <Toolbar className="diapp-searchbar">
            <ToolbarSection canGrow canShrink>
                {modalElement}
                {iconButton}
                <SearchInput
                    data-test-id={"search-bar"}
                    focusOnCreation={focusOnCreation}
                    onFilterChange={onChange}
                    onEnter={onEnterExtended}
                    filterValue={query}
                    onClearanceHandler={onClear}
                    emptySearchInputMessage={emptySearchMessage}
                    invisibleCharacterWarning={invisibleCharacterWarning}
                    {...otherProps}
                />
            </ToolbarSection>
            {!!sorters && !!sorters.list.length && (
                <ToolbarSection>
                    <Spacing size="tiny" vertical />
                    <SortButton sortersList={sorters.list} activeSort={sorters.applied} />
                </ToolbarSection>
            )}
        </Toolbar>
    );
}
