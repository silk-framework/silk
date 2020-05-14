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
    // All change handlers
    changeHandlers: Record<string, (value) => void>;
}

/** Renders the errors message based on the error type. */
export const errorMessage = (title: string, errors: any) => {
    if (!errors) {
        return "";
    } else if (errors.type === "pattern") {
        return `${title} ${errors.message}.`;
    } else if (errors.type === "required") {
        return `${title} must be specified.`;
    } else {
        return "";
    }
};

/** Widget for a single parameter of a task. */
export const ParameterWidget = ({
    projectId,
    pluginId,
    paramId,
    required,
    propertyDetails,
    onFileUploadClick,
    formHooks,
    changeHandlers,
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
            messageText={errorMessage(title, errors)}
            hasStateDanger={errors}
        >
            {isFileInput(parameterType) ? (
                <Button onClick={onFileUploadClick}>Upload new {title}</Button>
            ) : !!autoCompletion ? (
                <Autocomplete
                    autoCompletion={autoCompletion}
                    onInputChange={handleAutoCompleteInput(paramId)}
                    onChange={changeHandlers[paramId]}
                    value={defaultValueAsJs(propertyDetails)}
                />
            ) : (
                <InputMapper
                    parameter={{ paramId: paramId, param: propertyDetails }}
                    intent={errors ? Intent.DANGER : Intent.NONE}
                    onChange={changeHandlers[paramId]}
                />
            )}
        </FieldItem>
    );
};
