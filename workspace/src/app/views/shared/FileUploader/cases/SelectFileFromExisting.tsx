import { FieldItem } from "gui-elements";
import {
    AutoCompleteField,
    IAutoCompleteFieldProps,
} from "gui-elements/src/components/AutocompleteField/AutoCompleteField";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { IProjectResource } from "@ducks/shared/typings";

interface IProps {
    autocomplete: IAutoCompleteFieldProps<IProjectResource, string>;

    /**
     * Fire when autocomplete value selected
     * @param value
     */
    onChange(value: string);

    /**
     * Default value
     */
    defaultValue?: string;

    labelAttributes?: {
        text: string;
        info: string;
        htmlFor: string;
    };
}

/**
 * The widget for "select from existing" option
 * @constructor
 */
export function SelectFileFromExisting({ autocomplete, onChange, defaultValue, labelAttributes }: IProps) {
    const selectedValueState = useState(defaultValue);
    const setSelectedValue = selectedValueState[1];
    const [error, setError] = useState(false);
    const [t] = useTranslation();

    const handleChange = (value: string) => {
        setError(!value);
        setSelectedValue(value);

        onChange(value);
    };

    return labelAttributes ? (
        <FieldItem labelAttributes={labelAttributes} messageText={error ? t("FileUploader.fileNotSpecified") : ""}>
            <AutoComplete autocomplete={autocomplete} handleChange={handleChange} />
        </FieldItem>
    ) : (
        <AutoComplete autocomplete={autocomplete} handleChange={handleChange} />
    );
}

const itemStringValue = (item: IProjectResource) => item.name;

interface AutoCompleteProps {
    autocomplete: IAutoCompleteFieldProps<IProjectResource, string>;
    handleChange: (value: string) => void;
}

const AutoComplete = ({ autocomplete, handleChange }: AutoCompleteProps) => (
    <AutoCompleteField<IProjectResource, string>
        {...autocomplete}
        onChange={handleChange}
        itemValueSelector={itemStringValue}
        itemValueRenderer={itemStringValue}
    />
);
