import React, { useEffect, useState } from "react";
import { MenuItem, Suggest } from "@wrappers/index";
import { IPropertyAutocomplete } from "@ducks/common/typings";
import { Highlighter } from "../Highlighter/Highlighter";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";

export interface IAutocompleteProps {
    /**
     * Autocomplete options, usually it recieved from backend
     * @see IPropertyAutocomplete
     */
    autoCompletion: IPropertyAutocomplete;

    /**
     * Fired when type in input
     * @param value
     */
    onSearch?(value: string): any;

    /**
     * Fired when value selected from input
     * @param value
     */
    onChange?(value: any);

    /**
     * The initial value for autocomplete input
     * @default ''
     */
    initialValue?: string;
    /**
     * The initial items,
     * if empty then fetch from onSearch or keep it empty
     * @default []
     */
    items?: any[];

    /**
     * item label renderer
     * @param item
     * @default (item) => item.label || item.id
     */
    itemLabelRenderer?(item: any): string;

    /**
     * item value renderer
     * @param item
     * @default (item) => item.value || item.id
     */
    itemValueRenderer?(item: any): string;
}

const SuggestAutocomplete = Suggest.ofType<IAutocompleteDefaultResponse>();

Autocomplete.defaultProps = {
    items: [],
    initialValue: "",
    itemLabelRenderer: (item) => item.label || item.id,
    itemValueRenderer: (item) => item.value || item.id,
};

export function Autocomplete(props: IAutocompleteProps) {
    const { itemValueRenderer, itemLabelRenderer, items, onSearch, onChange, initialValue } = props;

    // The suggestions that match the user's input
    const [filtered, setFiltered] = useState<any[]>([]);

    useEffect(() => {
        if (!items.length) {
            handleQueryChange();
        }
    }, [items]);

    const areEqualItems = (itemA, itemB) => itemValueRenderer(itemA) === itemValueRenderer(itemB);

    const onItemSelect = (item) => {
        const actualValue = itemValueRenderer(item);
        onChange(actualValue);
    };

    //@Note: issue https://github.com/palantir/blueprint/issues/2983
    const handleQueryChange = async (input = "") => {
        try {
            let result = [];
            if (onSearch) {
                result = await onSearch(input);
            } else if (items.length && input) {
                // Filter our suggestions that don't contain the user's input
                result = items.filter(
                    ({ label, value }) =>
                        value.toLowerCase().indexOf(input.toLowerCase()) > -1 ||
                        label.toLowerCase().indexOf(input.toLowerCase()) > -1
                );
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
        return (
            <MenuItem
                active={modifiers.active}
                disabled={modifiers.disabled}
                key={itemValueRenderer(item)}
                onClick={handleClick}
                text={<Highlighter label={itemLabelRenderer(item)} searchValue={query} />}
            />
        );
    };

    return (
        <SuggestAutocomplete
            items={filtered}
            inputValueRenderer={itemValueRenderer}
            itemRenderer={optionRenderer}
            itemsEqual={areEqualItems}
            noResults={<MenuItem disabled={true} text="No results." />}
            onItemSelect={onItemSelect}
            onQueryChange={handleQueryChange}
            query={initialValue}
            popoverProps={{
                minimal: true,
            }}
        />
    );
}
