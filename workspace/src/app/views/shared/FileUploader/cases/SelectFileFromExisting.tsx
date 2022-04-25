import { FieldItem } from "@eccenca/gui-elements";
import {
    AutoCompleteField,
    IAutoCompleteFieldProps,
} from "@eccenca/gui-elements/src/components/AutocompleteField/AutoCompleteField";
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
            <ProjectResourceAutoComplete
                autocomplete={autocomplete}
                handleChange={handleChange}
                initialValue={defaultValue}
            />
        </FieldItem>
    ) : (
        <ProjectResourceAutoComplete
            autocomplete={autocomplete}
            handleChange={handleChange}
            initialValue={defaultValue}
        />
    );
}

const itemStringValue = (item: IProjectResource) => item.name;

interface AutoCompleteProps {
    initialValue?: string;
    autocomplete: IAutoCompleteFieldProps<IProjectResource, string>;
    handleChange: (value: string) => void;
}

const ProjectResourceAutoComplete = ({ autocomplete, handleChange, initialValue }: AutoCompleteProps) => (
    <AutoCompleteField<IProjectResource, string>
        {...autocomplete}
        initialValue={initialValue ? { name: initialValue, modified: "2000-01-01", size: 1 } : undefined}
        onChange={handleChange}
        itemValueSelector={itemStringValue}
        itemValueRenderer={itemStringValue}
        itemValueString={itemStringValue}
    />
);
