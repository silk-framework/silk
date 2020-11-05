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
     */
    onChange?(value: U);

    /**
     * The initial value for autocomplete input
     * @default ''
     */
    initialValue?: T;

    /**
     * item label renderer
     * @param item
     * @default (item) => item.label || item.id
     */
    itemLabelRenderer?(item: T): string;

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
}

Autocomplete.defaultProps = {
    itemLabelRenderer: (item) => {
        const label = item.label || item.value;
        if (label === "") {
            return "\u00A0";
        } else {
            return label;
        }
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
        itemLabelRenderer,
        onSearch,
        onChange,
        initialValue,
        dependentValues,
        emptySelectedValue,
        resetValue,
        autoFocus,
        itemKey,
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

    const onSelectionChange = (value) => {
        setSelectedItem(value);
        onItemSelect(value);
    };

    const areEqualItems = (itemA, itemB) => itemValueSelector(itemA) === itemValueSelector(itemB);

    const onItemSelect = (item) => {
        onChange(itemValueSelector(item));
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
        return (
            <MenuItem
                active={modifiers.active}
                disabled={modifiers.disabled}
                key={itemKey(item)}
                onClick={handleClick}
                text={<Highlighter label={itemLabelRenderer(item)} searchValue={query} />}
            />
        );
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
            inputValueRenderer={selectedItem !== undefined ? itemLabelRenderer : () => ""}
            itemRenderer={optionRenderer}
            itemsEqual={areEqualItems}
            noResults={<MenuItem disabled={true} text={t("common.messages.noResults", "No results.")} />}
            onItemSelect={onSelectionChange}
            onQueryChange={handleQueryChange}
            query={query}
            popoverProps={{
                minimal: true,
                popoverClassName: "app_di-autocomplete__options",
                wrapperTagName: "div",
            }}
            selectedItem={selectedItem}
            fill
            {...otherProps}
            inputProps={updatedInputProps}
        />
    );
}
