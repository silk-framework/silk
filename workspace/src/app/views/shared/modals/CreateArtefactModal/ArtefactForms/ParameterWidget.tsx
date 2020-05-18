import { ITaskParameter } from "@ducks/common/typings";
import { Button, FieldItem, Icon, TitleSubsection } from "@wrappers/index";
import { Autocomplete } from "../../../Autocomplete/Autocomplete";
import { InputMapper } from "./InputMapper";
import { Intent } from "@wrappers/blueprint/constants";
import React from "react";
import { INPUT_TYPES } from "../../../../../constants";
import { sharedOp } from "@ducks/shared";
import { AppToaster } from "../../../../../services/toaster";
import { defaultValueAsJs } from "./TaskForm";
import Spacing from "@wrappers/src/components/Separation/Spacing";

const isFileInput = (type: string) => type === INPUT_TYPES.RESOURCE;

const MAXLENGTH_TOOLTIP = 40;

interface IHookFormParam {
    errors: any;
}
interface IParam {
    projectId: string;
    // The ID of the parent object
    pluginId: string;
    // ID of the form parameter. For nested parameters this has the form of 'parentParam.param'.
    formParamId: string;
    // Marked as required parameter
    required: boolean;
    // The details of this parameter
    taskParameter: ITaskParameter;
    // Handler for file upload buttons
    onFileUploadClick: (e) => void;
    // from react-hook-form
    formHooks: IHookFormParam;
    // All change handlers
    changeHandlers: Record<string, (value) => void>;
    // Initial values in a flat form, e.g. "nestedParam.param1". This is either set for all parameters or not set for none.
    // The prefixed values can be addressed with help of the 'formParamId' parameter.
    initialValues: {
        [key: string]: string;
    };
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
    formParamId,
    required,
    taskParameter,
    onFileUploadClick,
    formHooks,
    changeHandlers,
    initialValues,
}: IParam) => {
    const errors = formHooks.errors[formParamId];
    const propertyDetails = taskParameter.param;
    const { title, description, parameterType, autoCompletion } = propertyDetails;
    const handleAutoCompleteInput = async (input: string = "") => {
        try {
            return await sharedOp.getAutocompleteResultsAsync({
                pluginId: pluginId,
                parameterId: taskParameter.paramId,
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
    if (propertyDetails.type === "object") {
        return (
            <div className={"parameterGroup"}>
                <Spacing />
                <TitleSubsection>
                    {" "}
                    {/* TODO: Improve sub heading */}
                    <h3>
                        {propertyDetails.title}
                        <Spacing size="tiny" vertical />
                        <Icon name="item-info" small tooltipText={propertyDetails.description} />
                    </h3>
                </TitleSubsection>
                <Spacing />
                {Object.entries(propertyDetails.properties).map(([nestedParamId, nestedParam]) => {
                    const nestedFormParamId = `${formParamId}.${nestedParamId}`;
                    return (
                        <ParameterWidget
                            key={formParamId}
                            projectId={projectId}
                            pluginId={propertyDetails.pluginId}
                            formParamId={nestedFormParamId}
                            required={false /* TODO: Get this information*/}
                            taskParameter={{ paramId: nestedParamId, param: nestedParam }}
                            onFileUploadClick={onFileUploadClick}
                            formHooks={formHooks}
                            changeHandlers={changeHandlers}
                            initialValues={initialValues}
                        />
                    );
                })}
                <Spacing />
            </div>
        );
    } else {
        return (
            <FieldItem
                labelAttributes={{
                    text: title,
                    info: required ? "required" : "",
                    htmlFor: formParamId,
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
                        onInputChange={handleAutoCompleteInput}
                        onChange={changeHandlers[formParamId]}
                        value={
                            initialValues[formParamId] ? initialValues[formParamId] : defaultValueAsJs(propertyDetails)
                        }
                    />
                ) : (
                    <InputMapper
                        parameter={{ paramId: formParamId, param: propertyDetails }}
                        intent={errors ? Intent.DANGER : Intent.NONE}
                        onChange={changeHandlers[formParamId]}
                        initialValues={initialValues}
                    />
                )}
            </FieldItem>
        );
    }
};
