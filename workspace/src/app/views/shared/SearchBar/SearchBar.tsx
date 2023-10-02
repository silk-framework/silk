import React from "react";
import { ISortersState } from "@ducks/workspace/typings";
import { Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import SearchInput, { ISearchInputProps } from "./SearchInput";
import SortButton from "../buttons/SortButton";
import { useTranslation } from "react-i18next";
import { useInvisibleCharacterCleanUpModal } from "../modals/InvisibleCharacterCleanUpModal";
import { useSearch } from "../../../hooks/useSearch";
import { useDispatch, useSelector } from "react-redux";
import { workspaceSel } from "@ducks/workspace";
import { DATA_TYPES } from "../../../constants";
import { routerOp } from "@ducks/router";

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
    /** Optional property to select the first result item when Enter key is pressed. this doesn't override the existing OnEnter */
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
    selectFirstResultItemOnEnter,
    ...otherProps
}: IProps) {
    const [t] = useTranslation();
    const { query, setQuery, onChange, onEnter: onEnterRefreshSearch, onClear } = useSearch(onSearch, textQuery);
    const workspaceSearchResult = useSelector(workspaceSel.resultsSelector);
    const dispatch = useDispatch();

    const emptySearchMessage = otherProps.emptySearchInputMessage
        ? otherProps.emptySearchInputMessage
        : t("form.field.searchField", "Enter search term");

    const { iconButton, modalElement, invisibleCharacterWarning } = useInvisibleCharacterCleanUpModal({
        inputString: query,
        setString: setQuery,
        callbackDelay: 200,
    });

    const searchOnEnter = React.useCallback(() => {
        const firstResult = workspaceSearchResult[0];
        const labels: Record<string, string> = {};
        if (firstResult && firstResult.itemLinks) {
            if (firstResult.type === DATA_TYPES.PROJECT) {
                labels.projectLabel = firstResult.label;
            } else {
                labels.taskLabel = firstResult.label;
            }
            dispatch(routerOp.goToPage(firstResult.itemLinks[0].path, labels));
        }
    }, [workspaceSearchResult]);

    const handleEnterPress = () => {
        if (selectFirstResultItemOnEnter) {
            searchOnEnter();
        }
        onEnter ? onEnter() : onEnterRefreshSearch();
    };

    return (
        <Toolbar>
            <ToolbarSection canGrow>
                {modalElement}
                {iconButton}
                <SearchInput
                    data-test-id={"search-bar"}
                    focusOnCreation={focusOnCreation}
                    onFilterChange={onChange}
                    onEnter={handleEnterPress}
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
