import { selectOptionProps } from "../../../../../../../utils/selectOptionProps";
import React from "react";
import { FieldItem, MenuItem, Select } from "@eccenca/gui-elements";
import { SuggestionTypeValues } from "../suggestion.typings";

const TYPES: SuggestionTypeValues[] = ["value", "object"];

interface IProps {
    selected: SuggestionTypeValues;
    elementLabel: string;

    onChange(value: SuggestionTypeValues);
}

export default function TypesList({ onChange, selected, elementLabel }: IProps) {
    const [activeType, setActiveType] = React.useState<SuggestionTypeValues>(selected);

    React.useEffect(() => setActiveType(selected), [selected]);

    const areTypesEqual = (typeA: SuggestionTypeValues, typeB: SuggestionTypeValues) => {
        return typeA.toLowerCase() === typeB.toLowerCase();
    };

    return (
        <FieldItem
            labelProps={{
                text: `Mapping type for ${elementLabel}`,
                hidden: true,
            }}
        >
            <Select<SuggestionTypeValues>
                filterable={false}
                activeItem={activeType}
                onActiveItemChange={(type) => {
                    if (type != null) {
                        setActiveType(type);
                    }
                }}
                contextOverlayProps={{
                    // Navigation may move away from the selection without committing it.
                    onClosing: () => setActiveType(selected),
                }}
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
