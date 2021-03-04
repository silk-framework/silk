import React, { useEffect, useState } from "react";
import { HTMLInputProps, IInputGroupProps } from "@blueprintjs/core";
import { Highlighter, IconButton, MenuItem, Suggest } from "@gui-elements/index";
import { IPropertyAutocomplete } from "@ducks/common/typings";
import { useTranslation } from "react-i18next";

type SearchFunction<T extends any> = (value: string) => T[];
type AsyncSearchFunction<T extends any> = (value: string) => Promise<T[]>;

export interface IAutocompleteProps<T extends any, U extends any> {
    /**
     * Autocomplete options, usually received from backend
     * @see IPropertyAutocomplete
     */
    autoCompletion: IPropertyAutocomplete;

    /**
     * Fired when type in input
     * @param value
     */
    onSearch: SearchFunction<T> | AsyncSearchFunction<T>;

    /**
     * Fired when value selected from input
     * @param value
     * @param e the event
     */
    onChange?(value: U, e?: React.SyntheticEvent<HTMLElement>);

    /**
     * The initial value for autocomplete input
     * @default ''
     */
    initialValue?: T;

    /**
     * Either the label of the select option or an option element that should be displayed as option in the selection.
     * If the return value is a string, a default render component will be displayed with search highlighting.
     * @param item  The item that should be displayed as an option in the select list.
     * @param query The current search query
     * @param active If the item is currently active
     * @param handleClick The function that needs to be called when the rendered item gets clicked. Else a selection
     *                    via mouse is not possible. This only needs to be used when returning a JSX.Element.
     * @default (item) => item.label || item.id
     */
    itemRenderer(item: T, query: string, active: boolean, handleClick: () => any): string | JSX.Element;

    /** Renders the string that should be displayed in the input field after the item has been selected.
     * If not defined and itemRenderer returns a string, the value from itemRenderer is used. */
    itemValueRenderer(item: T): string;

    /**
     * Selects the part from the auto-completion item that is called with the onChange callback.
     * @param item
     */
    itemValueSelector(item: T): U;

    /**
     * The values of the parameters this auto-completion depends on.
     */
    dependentValues?: string[];

    /**
     * Props to spread to the query `InputGroup`. To control this input, use
     * `query` and `onQueryChange` instead of `inputProps.value` and
     * `inputProps.onChange`.
     */
    inputProps?: IInputGroupProps & HTMLInputProps;

    /** Defines if a value can be reset, i.e. a reset icon is shown and the value is set to a specific value.
     *  When undefined, a value cannot be reset.
     */
    reset?: {
        /** Returns true if the currently set value can be reset, i.e. set to the resetValue. The reset icon is only shown if true. */
        resettableValue(value: T): boolean;

        /** The value onChange is called with when a reset is triggered. */
        resetValue: U;
    };

    // If enabled the auto completion component will auto focus
    autoFocus?: boolean;

    // Contains methods for new item creation
    createNewItem?: {
        /** Creates a new item from the query. If this is defined, creation of new items will be allowed. */
        itemFromQuery: (query: string) => T;

        /** Renders how newly created items should look like. */
        itemRenderer: (
            query: string,
            active: boolean,
            handleClick: React.MouseEventHandler<HTMLElement>
        ) => JSX.Element | undefined;
    };
}

Autocomplete.defaultProps = {
    dependentValues: [],
    resetPossible: false,
    autoFocus: false,
};

/** Auto-complete input widget. */
export function Autocomplete<T extends any, U extends any>(props: IAutocompleteProps<T, U>) {
    const {
        reset,
        itemValueSelector,
        itemRenderer,
        onSearch,
        onChange,
        initialValue,
        dependentValues,
        autoFocus,
        createNewItem,
        itemValueRenderer,
        ...otherProps
    } = props;
    const [selectedItem, setSelectedItem] = useState<T | undefined>(initialValue);

    const [query, setQuery] = useState<string>("");
    const [hasFocus, setHasFocus] = useState<boolean>(false);

    // The suggestions that match the user's input
    const [filtered, setFiltered] = useState<T[]>([]);

    const [t] = useTranslation();

    const SuggestAutocomplete = Suggest.ofType<T>();

    // Sets the query to the item value if it has a valid string value
    const setQueryToSelectedValue = (item: T) => {
        if (item) {
            setQuery(itemValueRenderer(item));
        }
    };

    // The key for the option elements
    const itemKey = (item: T): string => {
        let itemValue: U | string = itemValueSelector(item);
        if (typeof itemValue !== "string") {
            itemValue = itemValueRenderer(item);
        }
        return itemValue;
    };

    useEffect(() => {
        setQueryToSelectedValue(selectedItem);
    }, [selectedItem]);

    useEffect(() => {
        // Don't fetch auto-completion values when
        if (dependentValues.length === props.autoCompletion.autoCompletionDependsOnParameters.length && hasFocus) {
            const timeout: number = window.setTimeout(async () => {
                fetchQueryResults(query);
            }, 200);
            return () => clearTimeout(timeout);
        }
    }, [dependentValues.join("|"), hasFocus, query]);

    // We need to fire some actions when the auto-complete widget gets or loses focus
    const handleOnFocusIn = () => {
        setHasFocus(true);
    };

    const handleOnFocusOut = () => {
        setHasFocus(false);
        // Reset query to selected value when loosing focus, so the selected value can always be edited.
        setQueryToSelectedValue(selectedItem);
        setFiltered([]);
    };

    const onSelectionChange = (value, e) => {
        setSelectedItem(value);
        onItemSelect(value, e);
        setQueryToSelectedValue(value);
    };

    const areEqualItems = (itemA, itemB) => itemValueSelector(itemA) === itemValueSelector(itemB);

    const onItemSelect = (item, e) => {
        onChange(itemValueSelector(item), e);
    };

    // Return the index of the item in the array based on the itemValueRenderer value
    const itemIndexOf = (arr: T[], searchItem: T): number => {
        let idx = -1;
        const searchItemString = itemValueRenderer(searchItem);
        arr.forEach((v, i) => {
            if (itemValueRenderer(v) === searchItemString) {
                idx = i;
            }
        });
        return idx;
    };

    // Fetches the results for the given query
    const fetchQueryResults = async (input: string) => {
        try {
            let result = await onSearch(input);
            if (result.length <= 1 && selectedItem && input.length > 0 && itemValueRenderer(selectedItem) === input) {
                // If the auto-completion only returns no suggestion or the selected item itself, query with empty string.
                const emptyStringResults = await onSearch("");
                // Put selected item at the top, remove it from other places in the result list
                if (result.length === 1 && itemIndexOf(emptyStringResults, selectedItem) > -1) {
                    const idx = itemIndexOf(emptyStringResults, selectedItem);
                    result = [selectedItem, ...emptyStringResults.slice(0, idx), ...emptyStringResults.slice(idx + 1)];
                } else {
                    result = [selectedItem, ...emptyStringResults];
                }
            }
            setFiltered(result);
        } catch (e) {
            console.log(e);
        }
    };

    const optionRenderer = (item, { handleClick, modifiers, query }) => {
        if (!modifiers.matchesPredicate) {
            return null;
        }
        const renderedItem = itemRenderer(item, query, modifiers.active, handleClick);
        if (typeof renderedItem === "string") {
            return (
                <MenuItem
                    active={modifiers.active}
                    disabled={modifiers.disabled}
                    key={itemKey(item)}
                    onClick={handleClick}
                    text={<Highlighter label={renderedItem} searchValue={query} />}
                />
            );
        } else {
            return renderedItem;
        }
    };
    // Resets the selection
    const clearSelection = (resetValue: U) => () => {
        setSelectedItem(undefined);
        onChange(resetValue);
        setQuery("");
    };
    const clearButton = reset &&
        selectedItem !== undefined &&
        selectedItem !== null &&
        reset.resettableValue(selectedItem) && (
            <IconButton
                data-test-id={
                    (otherProps.inputProps.id ? `${otherProps.inputProps.id}-` : "") + "auto-complete-clear-btn"
                }
                name="operation-clear"
                text={t("common.action.resetSelection", "Reset selection")}
                onClick={clearSelection(reset.resetValue)}
            />
        );
    const updatedInputProps: IInputGroupProps & HTMLInputProps = {
        rightElement: clearButton,
        autoFocus: autoFocus,
        onBlur: handleOnFocusOut,
        onFocus: handleOnFocusIn,
        ...otherProps.inputProps,
    };
    return (
        <SuggestAutocomplete
            className="app_di-autocomplete__input"
            items={filtered}
            inputValueRenderer={selectedItem !== undefined ? itemValueRenderer : () => ""}
            itemRenderer={optionRenderer}
            itemsEqual={areEqualItems}
            noResults={<MenuItem disabled={true} text={t("common.messages.noResults", "No results.")} />}
            onItemSelect={onSelectionChange}
            onQueryChange={(q) => setQuery(q)}
            closeOnSelect={true}
            query={query}
            popoverProps={{
                minimal: true,
                position: "bottom-left",
                popoverClassName: "app_di-autocomplete__options",
                wrapperTagName: "div",
            }}
            selectedItem={selectedItem}
            fill
            createNewItemFromQuery={createNewItem?.itemFromQuery}
            createNewItemRenderer={createNewItem?.itemRenderer}
            {...otherProps}
            inputProps={updatedInputProps}
        />
    );
}
