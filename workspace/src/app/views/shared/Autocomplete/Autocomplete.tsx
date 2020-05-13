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
    onChange(value: string);

    /**
     * The initial value for autocomplete input
     */
    initialValue?: string;
    /**
     * The initial items,
     * if it's not provided then fetch from onSearch or keep it empty
     */
    items?: any[];

    /**
     * item renderer
     * @param item
     */
    itemRenderer?(item: any): string;
}

const SuggestAutocomplete = Suggest.ofType<IAutocompleteDefaultResponse>();

export function Autocomplete(props: IAutocompleteProps) {
    // The suggestions that match the user's input
    const [filtered, setFiltered] = useState<any[]>([]);
    const itemRenderer = props.itemRenderer ? props.itemRenderer : (item) => item.label || item.id;

    useEffect(() => {
        if (!props.items) {
            handleQueryChange();
        }
    }, [props.items]);

    const areEqualItems = (itemA, itemB) => itemA.value === itemB.value;

    const onItemSelect = (item) => props.onChange(item.value);

    //@Note: issue https://github.com/palantir/blueprint/issues/2983
    const handleQueryChange = async (input = "") => {
        try {
            let result = [];
            if (props.onSearch) {
                result = await props.onSearch(input);
            } else if (props.items && input) {
                // Filter our suggestions that don't contain the user's input
                result = props.items.filter(
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
                key={item.value}
                onClick={handleClick}
                text={<Highlighter label={itemRenderer(item)} searchValue={query} />}
            />
        );
    };

    return (
        <SuggestAutocomplete
            items={filtered}
            inputValueRenderer={itemRenderer}
            itemRenderer={optionRenderer}
            itemsEqual={areEqualItems}
            noResults={<MenuItem disabled={true} text="No results." />}
            onItemSelect={onItemSelect}
            onQueryChange={handleQueryChange}
            query=""
            popoverProps={{
                minimal: true,
            }}
        />
    );
}
