import React, { useContext } from "react";
import { Button, MenuItem, Select } from "@gui-elements/index";
import { SuggestionListContext } from "../SuggestionContainer";

// Select<T> is a generic component to work with your data types.
// In TypeScript, you must first obtain a non-generic reference:
const TypesSelect = Select.ofType<string>();

export default function TypesList({onChange, selected}) {
    const context = useContext(SuggestionListContext);

    const areTypesEqual = (typeA: string, typeB: string) => {
        return typeA.toLowerCase() === typeB.toLowerCase();
    }

    const itemRenderer = (type: string, {handleClick}) => {
        return <MenuItem
            label={type}
            key={type}
            onClick={handleClick}
        />
    }
    return <TypesSelect
        filterable={false}
        onItemSelect={onChange}
        items={['value', 'object']}
        itemRenderer={itemRenderer}
        itemsEqual={areTypesEqual}
        popoverProps={{
            minimal: true,
            portalContainer: context.portalContainer
        }}
    >
        <Button
            rightIcon="select-caret"
            text={selected}
        />
    </TypesSelect>

}
