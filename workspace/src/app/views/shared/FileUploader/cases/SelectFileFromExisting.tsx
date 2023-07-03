import { FieldItem } from "@eccenca/gui-elements";
import { SuggestField, SuggestFieldProps } from "@eccenca/gui-elements";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { fileValue, IProjectResource } from "@ducks/shared/typings";

interface IProps {
    autocomplete: SuggestFieldProps<IProjectResource, string>;

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

    /** Indicator that there needs to be a value set/selected, else the file selection (from existing files) can e.g. be reset. */
    required: boolean;

    /** When used inside a modal, the behavior of the auto-complete component will be optimized. */
    insideModal: boolean;
}

/**
 * The widget for "select from existing" option
 * @constructor
 */
export function SelectFileFromExisting({
    autocomplete,
    onChange,
    defaultValue,
    labelAttributes,
    required,
    insideModal,
}: IProps) {
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
        <FieldItem labelProps={labelAttributes} messageText={error ? t("FileUploader.fileNotSpecified") : ""}>
            <ProjectResourceAutoComplete
                autocomplete={autocomplete}
                handleChange={handleChange}
                initialValue={defaultValue}
                resettable={!required}
                insideModal={insideModal}
            />
        </FieldItem>
    ) : (
        <ProjectResourceAutoComplete
            autocomplete={autocomplete}
            handleChange={handleChange}
            initialValue={defaultValue}
            resettable={!required}
            insideModal={insideModal}
        />
    );
}

const itemStringValue = (item: IProjectResource) => fileValue(item);

interface ProjectResourceAutoCompleteProps {
    initialValue?: string;
    autocomplete: SuggestFieldProps<IProjectResource, string>;
    handleChange: (value: string) => void;
    /** If true allows to clear the selection. */
    resettable: boolean;
    /** If true the value cannot be edited. */
    readonly?: boolean;
    /** When used inside a modal, the behavior of some components will be optimized. */
    insideModal: boolean;
}

const ProjectResourceAutoComplete = ({
    autocomplete,
    handleChange,
    initialValue,
    resettable,
    insideModal,
}: ProjectResourceAutoCompleteProps) => {
    const [t] = useTranslation();

    return (
        <SuggestField<IProjectResource, string>
            {...autocomplete}
            initialValue={initialValue ? { name: initialValue, modified: "2000-01-01", size: 1 } : undefined}
            onChange={handleChange}
            itemValueSelector={itemStringValue}
            itemValueRenderer={itemStringValue}
            itemValueString={itemStringValue}
            reset={
                resettable
                    ? {
                          resetValue: "",
                          resetButtonText: t("common.action.resetSelection"),
                          resettableValue: () => true,
                      }
                    : undefined
            }
            hasBackDrop={!insideModal}
        />
    );
};
