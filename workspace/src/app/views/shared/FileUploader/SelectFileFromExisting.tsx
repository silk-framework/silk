import { Autocomplete, IAutocompleteProps } from "../Autocomplete/Autocomplete";
import { FieldItem } from "@wrappers/index";
import React, { useState } from "react";

interface IProps {
    autocomplete: IAutocompleteProps;
    /**
     * Fire when autocomplete value selected
     * @param value
     */
    onChange(value: string);
}

/**
 * The widget for "select from existing" option
 * @param autocomplete
 * @param onChange
 * @constructor
 */
export function SelectFileFromExisting({ autocomplete, onChange }: IProps) {
    const [error, setError] = useState(false);

    const handleChange = (value: string) => {
        if (!value) {
            setError(true);
        } else {
            setError(false);
        }
        onChange(value);
    };

    return (
        <FieldItem
            labelAttributes={{
                text: "Select available file from projects upload",
                info: "required",
                htmlFor: "autocompleteInput",
            }}
            messageText={error ? "File not specified" : ""}
        >
            <Autocomplete {...autocomplete} onChange={handleChange} />
        </FieldItem>
    );
}
