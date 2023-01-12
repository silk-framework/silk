import React, { useEffect, useState } from "react";
import { ISortersState } from "@ducks/workspace/typings";
import { Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import SearchInput, { ISearchInputProps } from "./SearchInput";
import SortButton from "../buttons/SortButton";
import { useTranslation } from "react-i18next";
import { useInvisibleCharacterCleanUpModal } from "../modals/InvisibleCharacterCleanUpModal";

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
}

/** A simple search bar. */
export function SearchBar({
    textQuery,
    sorters,
    onSort,
    onSearch,
    focusOnCreation = false,
    warnOfInvisibleCharacters = true,
    ...otherProps
}: IProps) {
    const [searchInput, setSearchInput] = useState(textQuery);
    const [detectedCodePoints, setDetectedCodePoints] = useState<Set<number>>(new Set());
    const [t] = useTranslation();

    useEffect(() => {
        setSearchInput(textQuery);
    }, [textQuery]);

    const emptySearchMessage = otherProps.emptySearchInputMessage
        ? otherProps.emptySearchInputMessage
        : t("form.field.searchField", "Enter search term");

    const handleSearchChange = (e) => {
        const value = e.target.value;
        setDetectedCodePoints(new Set());
        // when input is empty then apply filter
        if (value === "" && searchInput) {
            setSearchInput("");
            onSearch("");
        } else {
            setSearchInput(value);
        }
    };

    const onClearanceHandler = () => {
        setDetectedCodePoints(new Set());
        setSearchInput("");
        onSearch("");
    };

    const handleSearchEnter = () => {
        onSearch(searchInput);
    };

    const invisibleCharacterWarningCallback = React.useCallback((detectedCodePoints: Set<number>) => {
        setDetectedCodePoints(detectedCodePoints);
    }, []);

    const onClear = React.useCallback(() => setDetectedCodePoints(new Set()), []);

    const { iconButton, modalElement } = useInvisibleCharacterCleanUpModal({
        inputString: searchInput,
        setString: setSearchInput,
        detectedCodePoints,
        onClear,
    });

    return (
        <Toolbar>
            <ToolbarSection canGrow>
                {modalElement}
                {iconButton}
                <SearchInput
                    data-test-id={"search-bar"}
                    focusOnCreation={focusOnCreation}
                    onFilterChange={handleSearchChange}
                    onEnter={handleSearchEnter}
                    filterValue={searchInput}
                    onClearanceHandler={onClearanceHandler}
                    emptySearchInputMessage={emptySearchMessage}
                    invisibleCharacterWarningCallback={invisibleCharacterWarningCallback}
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
