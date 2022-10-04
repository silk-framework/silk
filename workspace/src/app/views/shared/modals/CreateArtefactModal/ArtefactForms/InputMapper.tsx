import React from "react";
import { INPUT_TYPES } from "../../../../../constants";
import { Switch, TextArea, TextField, CodeEditor } from "@eccenca/gui-elements";
import { ITaskParameter } from "@ducks/common/typings";
import { Intent } from "@blueprintjs/core";
import FileSelectionMenu from "../../../FileUploader/FileSelectionMenu";
import { AppToaster } from "../../../../../services/toaster";
import { requestResourcesList } from "@ducks/shared/requests";
import { defaultValueAsJs, stringValueAsJs } from "../../../../../utils/transformers";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { useTranslation } from "react-i18next";
import { DefaultTargetVocabularySelection } from "../../../TargetVocabularySelection/DefaultTargetVocabularySelection";

interface IProps {
    projectId: string;
    parameter: ITaskParameter;
    // Blueprint intent
    intent: Intent;
    onChange: (value) => void;
    // Initial values in a flat form, e.g. "nestedParam.param1". This is either set for all parameters or not set for none.
    // The prefixed values can be addressed with help of the 'formParamId' parameter.
    initialValues: {
        [key: string]: {
            label: string;
            value: string;
        };
    };
    /** This is a required parameter. */
    required: boolean;
}

/** The attributes for the GUI components. */
export interface IInputAttributes {
    id: string;
    name: string;
    intent: Intent;
    onChange: (value) => void;
    value?: any;
    defaultValue?: any;
    inputRef?: (e) => void;
    defaultChecked?: boolean;
}

/** Maps an atomic value to the corresponding value type widget. */
export function InputMapper({ projectId, parameter, intent, onChange, initialValues, required }: IProps) {
    const [t] = useTranslation();
    const { maxFileUploadSize } = useSelector(commonSel.initialSettingsSelector);
    const { paramId, param } = parameter;
    const initialValue =
        initialValues[paramId] !== undefined
            ? stringValueAsJs(parameter.param.parameterType, initialValues[paramId].value)
            : defaultValueAsJs(param, false);

    const inputAttributes: IInputAttributes = {
        id: paramId,
        name: paramId,
        intent: intent,
        onChange: onChange,
        defaultValue: initialValue,
    };

    const handleFileSearch = async (input: string) => {
        try {
            return (
                await requestResourcesList(projectId, {
                    searchText: input,
                })
            ).data;
        } catch (e) {
            AppToaster.show({
                message: e.detail,
                intent: Intent.DANGER,
                timeout: 0,
            });
            return [];
        }
    };

    if (param.parameterType === INPUT_TYPES.BOOLEAN) {
        inputAttributes.defaultChecked = initialValue;
    }

    switch (param.parameterType) {
        case INPUT_TYPES.BOOLEAN:
            return <Switch {...inputAttributes} />;
        // NumericInput does not support onChange, see https://github.com/palantir/blueprint/issues/3943
        case INPUT_TYPES.INTEGER:
            return <TextField {...inputAttributes} />;
        case INPUT_TYPES.TEXTAREA:
            return <TextArea {...inputAttributes} />;
        case INPUT_TYPES.RESTRICTION:
            return <CodeEditor mode="sparql" {...inputAttributes} />;
        case INPUT_TYPES.MULTILINE_STRING:
            return <CodeEditor mode="undefined" {...inputAttributes} />;
        case INPUT_TYPES.PASSWORD:
            return <TextField {...inputAttributes} type={"password"} />;
        case INPUT_TYPES.TARGET_VOCABULARY:
            return <DefaultTargetVocabularySelection {...inputAttributes} />;
        case INPUT_TYPES.RESOURCE:
            const resourceNameFn = (item) => item.name;
            return (
                <FileSelectionMenu
                    projectId={projectId}
                    advanced={{
                        autocomplete: {
                            onSearch: handleFileSearch,
                            itemRenderer: resourceNameFn,
                            itemValueRenderer: resourceNameFn,
                            itemValueSelector: resourceNameFn,
                            noResultText: t("common.messages.noResults", "No results."),
                        },
                    }}
                    allowMultiple={false}
                    maxFileUploadSizeBytes={maxFileUploadSize}
                    onUploadSuccess={(file) => {
                        // FIXME: the onChange function is not called on upload success, so this is a workaround
                        if (file) {
                            onChange(file.name);
                        }
                    }}
                    required={required}
                    {...inputAttributes}
                />
            );
        case INPUT_TYPES.ENUMERATION:
        case INPUT_TYPES.OPTION_INT:
        case INPUT_TYPES.STRING:
        default:
            return <TextField {...inputAttributes} />;
    }
}
