import { Autocomplete, IAutocompleteProps } from "../../Autocomplete/Autocomplete";
import { FieldItem } from "@gui-elements/index";
import React, { useState } from "react";

interface IProps {
    autocomplete: IAutocompleteProps;

    /**
     * Fire when autocomplete value selected
     * @param value
     */
    onChange(value: string);

    /**
     * Default value
     */
    defaultValue?: string;
}

/**
 * The widget for "select from existing" option
 * @constructor
 */
export function SelectFileFromExisting(props: IProps) {
    const { autocomplete, onChange, defaultValue } = props;

    const selectedValueState = useState(defaultValue);
    const setSelectedValue = selectedValueState[1];
    const [error, setError] = useState(false);

    const handleChange = (value: string) => {
        setError(!value);
        setSelectedValue(value);

        onChange(value);
    };

    return (
        <FieldItem
            labelAttributes={{
                text: "Select file from projects",
                info: "required",
                htmlFor: "autocompleteInput",
            }}
            messageText={error ? "File not specified" : ""}
        >
            <Autocomplete {...autocomplete} onChange={handleChange} />
        </FieldItem>
    );
}
