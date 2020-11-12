import React, { useEffect, useState } from "react";
import { HTMLInputProps, IInputGroupProps } from "@blueprintjs/core";
import { MenuItem, Suggest } from "@gui-elements/index";
import { IPropertyAutocomplete } from "@ducks/common/typings";
import { Highlighter } from "../Highlighter/Highlighter";
import IconButton from "@gui-elements/src/components/Icon/IconButton";
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
     * @param item  The item that should be displayed as an option in the selectiong.
     * @param query The current search query
     * @param active If the item is currently active
     * @default (item) => item.label || item.id
     */
    itemRenderer?(item: T, query: string, active: boolean): string | JSX.Element;

    /** Renders the string that should be displayed in the input field after the item has been selected.
     * If not defined and itemRenderer returns a string, the value from itemRenderer is used. */
    itemValueRenderer?(item: T): string;

    /**
     * The part from the auto-completion item that is called with the onChange callback.
     * @param item
     * @default (item) => item.value
     */
    itemValueSelector?(item: T): U;

    /** Generates the key of the item. This needs to be a unique string. */
    itemKey?(item: T): string;

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

    /** If true, then a selection can be reset.
     * Both emptySelectedValue and emptyValue must also be defined in order to reset to function correctly. */
    resetPossible?: boolean;

    /** Returns if the selected value is "empty". This must be defined if resetPossible is true. */
    emptySelectedValue?: (T) => boolean;

    /** The value onChange is called with when a reset is triggered. */
    resetValue?: U;

    // If enabled the auto completion component will auto focus
    autoFocus?: boolean;

    /** Creates a new item from the query. If this is defined, creation of new items will be allowed. */
    createNewItemFromQuery?: (query: string) => T;

    /** Renders how newly created items should look like. */
    createNewItemRenderer?: (
        query: string,
        active: boolean,
        handleClick: React.MouseEventHandler<HTMLElement>
    ) => JSX.Element | undefined;
}

const defaultItemLabelFunction = (item) => {
    const label = item.label || item.value;
    if (label === "") {
        return "\u00A0";
    } else {
        return label;
    }
};

Autocomplete.defaultProps = {
    itemRenderer: (item) => {
        return defaultItemLabelFunction(item);
    },
    itemValueRenderer: (item) => {
        return defaultItemLabelFunction(item);
    },
    itemValueSelector: (item) => {
        return item.value;
    },
    dependentValues: [],
    resetPossible: false,
    itemKey: (item) => {
        if (typeof item === "string") {
            return item;
        } else {
            console.warn(
                `Application error: Auto-completion item is not of type string, but of type ${typeof item}, and no custom 'itemKey' method defined.`
            );
            return "" + Math.random();
        }
    },
    autoFocus: false,
    resetValue: null,
};

export function Autocomplete<T extends any, U extends any>(props: IAutocompleteProps<T, U>) {
    const {
        resetPossible,
        itemValueSelector,
        itemRenderer,
        onSearch,
        onChange,
        initialValue,
        dependentValues,
        emptySelectedValue,
        resetValue,
        autoFocus,
        itemKey,
        createNewItemFromQuery,
        createNewItemRenderer,
        itemValueRenderer,
        ...otherProps
    } = props;
    const [selectedItem, setSelectedItem] = useState<T | undefined>(initialValue);

    const [query, setQuery] = useState<string>("");

    // The suggestions that match the user's input
    const [filtered, setFiltered] = useState<T[]>([]);

    const [t] = useTranslation();

    const SuggestAutocomplete = Suggest.ofType<T>();

    useEffect(() => {
        // Don't fetch auto-completion values when
        if (dependentValues.length === props.autoCompletion.autoCompletionDependsOnParameters.length) {
            handleQueryChange(query, {});
        }
    }, [dependentValues.join("|")]);

    const onSelectionChange = (value, e) => {
        setSelectedItem(value);
        onItemSelect(value, e);
    };

    const areEqualItems = (itemA, itemB) => itemValueSelector(itemA) === itemValueSelector(itemB);

    const onItemSelect = (item, e) => {
        onChange(itemValueSelector(item), e);
    };

    //@Note: issue https://github.com/palantir/blueprint/issues/2983
    const handleQueryChange = async (input = "", event) => {
        // This function is fired twice because of above-mentioned issue, only allow the call with defined event.
        if (event) {
            setQuery(input);
            try {
                const result = await onSearch(input);
                setFiltered(result);
            } catch (e) {
                console.log(e);
            }
        }
    };

    const optionRenderer = (item, { handleClick, modifiers, query }) => {
        if (!modifiers.matchesPredicate) {
            return null;
        }
        const renderedItem = itemRenderer(item, query, modifiers.active);
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
    const clearSelection = () => {
        setSelectedItem(undefined);
        onChange(resetValue);
        setQuery("");
    };
    const clearButton = resetPossible &&
        selectedItem !== undefined &&
        selectedItem !== null &&
        emptySelectedValue &&
        !emptySelectedValue(selectedItem) && (
            <IconButton
                name="operation-clear"
                text={t("common.action.resetSelection", "Reset selection")}
                onClick={clearSelection}
            />
        );
    const updatedInputProps = {
        rightElement: clearButton,
        autoFocus: autoFocus,
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
            onQueryChange={handleQueryChange}
            query={query}
            popoverProps={{
                minimal: true,
                position: "bottom-right",
                popoverClassName: "app_di-autocomplete__options",
                wrapperTagName: "div",
            }}
            selectedItem={selectedItem}
            fill
            createNewItemFromQuery={createNewItemFromQuery}
            createNewItemRenderer={createNewItemRenderer}
            {...otherProps}
            inputProps={updatedInputProps}
        />
    );
}
