import React from "react";
import { INPUT_TYPES } from "../../../../../constants";
import { Switch, TextField, TextArea } from "@wrappers/index";
import { QueryEditor } from "../../../QueryEditor/QueryEditor";
import { ITaskParameter } from "@ducks/common/typings";
import { Intent } from "@blueprintjs/core";
import { defaultValueAsJs, stringValueAsJs } from "./TaskForm";
import { FileUploader } from "../../../FileUploader/FileUploader";
import { AppToaster } from "../../../../../services/toaster";
import { requestResourcesList } from "@ducks/shared/requests";

interface IProps {
    projectId: string;
    parameter: ITaskParameter;
    // Blueprint intent
    intent: Intent;
    onChange: (value) => void;
    // Initial values in a flat form, e.g. "nestedParam.param1". This is either set for all parameters or not set for none.
    // The prefixed values can be addressed with help of the 'formParamId' parameter.
    initialValues: {
        [key: string]: string;
    };
}

/** The attributes for the GUI components. */
interface IInputAttributes {
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
export function InputMapper({ projectId, parameter, intent, onChange, initialValues }: IProps) {
    const { paramId, param } = parameter;
    const initialValue =
        initialValues[paramId] !== undefined
            ? stringValueAsJs(parameter.param.parameterType, initialValues[paramId])
            : defaultValueAsJs(param);
    const inputAttributes: IInputAttributes = {
        id: paramId,
        name: paramId,
        intent: intent,
        onChange: onChange,
        defaultValue: initialValue,
    };

    const handleFileSearch = async (input: string) => {
        try {
            return await requestResourcesList(projectId, {
                searchText: input,
            });
        } catch (e) {
            AppToaster.show({
                message: e.detail,
                intent: Intent.DANGER,
                timeout: 0,
            });
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
        case INPUT_TYPES.MULTILINE_STRING:
            return <QueryEditor {...inputAttributes} />;
        case INPUT_TYPES.PASSWORD:
            return <TextField {...inputAttributes} type={"password"} />;
        case INPUT_TYPES.RESOURCE:
            return (
                <FileUploader
                    projectId={projectId}
                    advanced={true}
                    autocomplete={{
                        autoCompletion: {
                            allowOnlyAutoCompletedValues: true,
                            autoCompleteValueWithLabels: true,
                            autoCompletionDependsOnParameters: [],
                        },
                        onSearch: handleFileSearch,
                        itemLabelRenderer: (item) => item.name,
                        itemValueRenderer: (item) => item.name,
                    }}
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
