import { ParentElement } from "./ParentElement";
import React from "react";
import { FieldItem, RadioButton } from "@eccenca/gui-elements";

interface Props {
    initialValue: AllowedValues;
    onChange: (value: AllowedValues) => any;
}

type AllowedValues = "from" | "to";

/** Input component for the entity relationship direction. */
export const EntityRelationshipSelection = ({ initialValue, onChange }: Props) => {
    const [value, setValue] = React.useState<string>(initialValue);
    const changeValue = React.useCallback(
        (value: AllowedValues) => {
            onChange(value);
            setValue(value);
        },
        [onChange]
    );

    return (
        <FieldItem data-test-id={"entity-relationship-selection"}>
            <RadioButton
                checked={value === "from"}
                labelElement={
                    <span>
                        Connect from <ParentElement parent={parent} />
                    </span>
                }
                onChange={() => changeValue("from")}
            />
            <RadioButton
                checked={value === "to"}
                labelElement={
                    <span>
                        Connect to <ParentElement parent={parent} />
                    </span>
                }
                onChange={() => changeValue("to")}
            />
        </FieldItem>
    );
};
