import React, { useEffect, useState } from "react";
import { MenuItem, Suggest } from "@wrappers/index";
import { IPropertyAutocomplete } from "@ducks/common/typings";
import { Highlighter } from "../Highlighter/Highlighter";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import { init } from "ramda";

export interface IAutocompleteProps {
    /**
     * Autocomplete options, usually received from backend
     * @see IPropertyAutocomplete
     */
    autoCompletion: IPropertyAutocomplete;

    /**
     * Fired when type in input
     * @param value
     */
    onSearch(value: string): any;

    /**
     * Fired when value selected from input
     * @param value
     */
    onChange?(value: any);

    /**
     * The initial value for autocomplete input
     * @default ''
     */
    initialValue?: IAutocompleteDefaultResponse;

    /**
     * item label renderer
     * @param item
     * @default (item) => item.label || item.id
     */
    itemLabelRenderer?(item: any): string;

    /**
     * The part from the auto-completion item that is called with the onChange callback.
     * @param item
     * @default (item) => item.value
     */
    itemValueSelector?(item: any): string;

    /**
     * The values of the parameters this auto-completion depends on.
     */
    dependentValues?: string[];
}

const SuggestAutocomplete = Suggest.ofType<IAutocompleteDefaultResponse>();

Autocomplete.defaultProps = {
    initialValue: "",
    itemLabelRenderer: (item) => item.label || item.value,
    itemValueSelector: (item) => item.value,
    dependentValues: [],
};

export function Autocomplete(props: IAutocompleteProps) {
    const { itemValueSelector, itemLabelRenderer, onSearch, onChange, initialValue, dependentValues } = props;
    const [selectedItem, setSelectedItem] = useState<IAutocompleteDefaultResponse>(initialValue);

    const [query, setQuery] = useState<string>("");

    // The suggestions that match the user's input
    const [filtered, setFiltered] = useState<any[]>([]);

    useEffect(() => {
        // Don't fetch auto-completion values when
        if (dependentValues.length === props.autoCompletion.autoCompletionDependsOnParameters.length) {
            handleQueryChange(query);
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
    const handleQueryChange = async (input = "") => {
        setQuery(input);
        try {
            const result = await onSearch(input);
            setFiltered(result);
        } catch (e) {
            console.log(e);
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
                key={itemValueSelector(item)}
                onClick={handleClick}
                text={<Highlighter label={itemLabelRenderer(item)} searchValue={query} />}
            />
        );
    };
    return (
        <SuggestAutocomplete
            items={filtered}
            inputValueRenderer={itemLabelRenderer}
            itemRenderer={optionRenderer}
            itemsEqual={areEqualItems}
            noResults={<MenuItem disabled={true} text="No results." />}
            onItemSelect={onSelectionChange}
            onQueryChange={handleQueryChange}
            query={query}
            popoverProps={{
                minimal: true,
            }}
            selectedItem={selectedItem}
        />
    );
}
