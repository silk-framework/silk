import { Autocomplete, IAutocompleteProps } from "../../Autocomplete/Autocomplete";
import { Button, FieldItem } from "@wrappers/index";
import React, { useState } from "react";

interface IProps {
    autocomplete: IAutocompleteProps;
    /**
     * Fire when autocomplete value selected
     * @param value
     */
    onChange(value: string);

    /**
     * Show Change button for extra confirmation
     */
    confirmationButton?: boolean;

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
    const { autocomplete, onChange, confirmationButton, defaultValue } = props;

    const [selectedValue, setSelectedValue] = useState(defaultValue);
    const [error, setError] = useState(false);

    const handleChange = (value: string) => {
        setError(!value);
        setSelectedValue(value);

        if (!confirmationButton) {
            onChange(value);
        }
    };

    const handleConfirm = () => {
        onChange(selectedValue);
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
            {!!confirmationButton && (
                <Button affirmative onClick={handleConfirm}>
                    Change
                </Button>
            )}
        </FieldItem>
    );
}
