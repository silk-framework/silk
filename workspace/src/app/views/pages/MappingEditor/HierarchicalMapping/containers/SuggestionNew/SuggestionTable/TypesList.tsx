import { selectOptionProps } from "../../../../../../../utils/selectOptionProps";
import React from "react";
import { FieldItem, MenuItem, Select } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { SuggestionTypeValues } from "../suggestion.typings";

const TYPES: SuggestionTypeValues[] = ["value", "object"];

interface IProps {
    selected: SuggestionTypeValues;
    elementLabel: string;

    onChange(value: SuggestionTypeValues);
}

export default function TypesList({ onChange, selected, elementLabel }: IProps) {
    const [t] = useTranslation();
    const areTypesEqual = (typeA: SuggestionTypeValues, typeB: SuggestionTypeValues) => {
        return typeA.toLowerCase() === typeB.toLowerCase();
    };
    const selectedValueLabel = t("MappingSuggestion.selection.selectedFieldValue", {
        field: t("MappingSuggestion.selection.mappingTypeFor", { element: elementLabel }),
        value: selected,
    });

    return (
        <FieldItem
            labelProps={{
                text: selectedValueLabel,
                hidden: true,
            }}
        >
            <Select<SuggestionTypeValues>
                filterable={false}
                onItemSelect={onChange}
                items={TYPES}
                itemRenderer={(type, { handleClick, ...optionState }) => (
                    <MenuItem {...selectOptionProps(optionState)} text={type} key={type} onClick={handleClick} />
                )}
                itemsEqual={areTypesEqual}
                fill
                text={selected}
            />
        </FieldItem>
    );
}
