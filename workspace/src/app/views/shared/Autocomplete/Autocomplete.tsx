import React, { useDebugValue, useEffect, useState } from "react";
import { Button, MenuItem, Suggest } from "@wrappers/index";
import { IPropertyAutocomplete } from "@ducks/common/typings";
import { Highlighter } from "../Highlighter/Highlighter";

interface IProps {
    autoCompletion: IPropertyAutocomplete;
    onInputChange(value: string): any;
    onChange(value: string);
    value?: string;
    items?: any[];
}

interface IAutocompleteValue {
    label: string;
    value: string;
}

const SuggestAutocomplete = Suggest.ofType<IAutocompleteValue>();

export function Autocomplete(props: IProps) {
    // The suggestions that match the user's input
    const [filtered, setFiltered] = useState<any[]>([]);

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
            let result;
            if (props.onInputChange) {
                result = await props.onInputChange(input);
            } else if (input) {
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
                text={<Highlighter label={item.label || item.id} searchValue={query} />}
            />
        );
    };

    return (
        <SuggestAutocomplete
            items={filtered}
            inputValueRenderer={(item) => item.label}
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
