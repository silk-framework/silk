import { IArtefactItemProperty } from "@ducks/common/typings";
import { Button, FieldItem } from "@wrappers/index";
import { Autocomplete } from "../../../Autocomplete/Autocomplete";
import { InputMapper } from "./InputMapper";
import { Intent } from "@wrappers/blueprint/constants";
import React from "react";
import { INPUT_TYPES } from "../../../../../constants";
import { sharedOp } from "@ducks/shared";
import { AppToaster } from "../../../../../services/toaster";
import { defaultValueAsJs } from "./TaskForm";

const isFileInput = (type: string) => type === INPUT_TYPES.RESOURCE;

const MAXLENGTH_TOOLTIP = 40;

interface IHookFormParam {
    errors: any;
}
interface IParam {
    projectId: string;
    // The ID of the parent object
    pluginId: string;
    // ID of the parameter
    paramId: string;
    // Marked as required parameter
    required: boolean;
    // The details of this parameter
    propertyDetails: IArtefactItemProperty;
    // Handler for file upload buttons
    onFileUploadClick: (e) => void;
    // from react-hook-form
    formHooks: IHookFormParam;
    // Called when the value changes
    onChange: (value) => void;
}

/** Widget for a single parameter of a task. */
export const ParameterWidget = ({
    projectId,
    pluginId,
    paramId,
    required,
    propertyDetails,
    onFileUploadClick,
    formHooks,
    onChange,
}: IParam) => {
    const errors = formHooks.errors[paramId];
    const { title, description, parameterType, autoCompletion } = propertyDetails;
    const handleAutoCompleteInput = (key: string) => async (input: string = "") => {
        try {
            return await sharedOp.getAutocompleteResultsAsync({
                pluginId: pluginId,
                parameterId: key,
                projectId,
                dependsOnParameterValues: autoCompletion.autoCompletionDependsOnParameters,
                textQuery: input,
            });
        } catch (e) {
            AppToaster.show({
                message: e.detail,
                intent: Intent.DANGER,
                timeout: 0,
            });
        }
    };
    return (
        <FieldItem
            labelAttributes={{
                text: title,
                info: required ? "required" : "",
                htmlFor: paramId,
                tooltip: description && description.length <= MAXLENGTH_TOOLTIP ? description : "",
            }}
            helperText={description && description.length > MAXLENGTH_TOOLTIP ? description : ""}
            messageText={errors ? title + " not specified" : ""}
            hasStateDanger={errors}
        >
            {isFileInput(parameterType) ? (
                <Button onClick={onFileUploadClick}>Upload new {title}</Button>
            ) : !!autoCompletion ? (
                <Autocomplete
                    autoCompletion={autoCompletion}
                    onInputChange={handleAutoCompleteInput(paramId)}
                    onChange={() => {}}
                    value={defaultValueAsJs(propertyDetails)}
                />
            ) : (
                <InputMapper
                    parameter={{ paramId: paramId, param: propertyDetails }}
                    intent={errors ? Intent.DANGER : Intent.NONE}
                    onChange={onChange}
                />
            )}
        </FieldItem>
    );
};
