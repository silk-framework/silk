import React from "react";
import { MenuItem, Select } from "@eccenca/gui-elements";
import { SuggestionTypeValues } from "../suggestion.typings";

// Select<T> is a generic component to work with your data types.
// In TypeScript, you must first obtain a non-generic reference:
const TypesSelect = Select.ofType<string>();

const TYPES = ["value", "object"];

interface IProps {
    selected: SuggestionTypeValues;

    onChange(value: SuggestionTypeValues);
}

export default function TypesList({ onChange, selected }: IProps) {
    const areTypesEqual = (typeA: string, typeB: string) => {
        return typeA.toLowerCase() === typeB.toLowerCase();
    };

    const itemRenderer = (type: string, { handleClick }) => {
        return <MenuItem text={type} key={type} onClick={handleClick} active={selected === type} />;
    };

    return (
        <TypesSelect
            filterable={false}
            onItemSelect={onChange}
            items={TYPES}
            itemRenderer={itemRenderer}
            itemsEqual={areTypesEqual}
            fill
            text={selected}
        />
    );
}
