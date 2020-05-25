import React from "react";
import { sharedOp } from "@ducks/shared";
import { ITaskParameter } from "@ducks/common/typings";
import { FieldItem, FieldSet, TitleSubsection, Label } from "@wrappers/index";
import { Intent } from "@wrappers/blueprint/constants";
import { Autocomplete } from "../../../Autocomplete/Autocomplete";
import { InputMapper } from "./InputMapper";
import { AppToaster } from "../../../../../services/toaster";
import Spacing from "@wrappers/src/components/Separation/Spacing";
import { defaultValueAsJs } from "../../../../../utils/transformers";
import { INPUT_TYPES } from "../../../../../constants";

const MAXLENGTH_TOOLTIP = 40;

interface IHookFormParam {
    errors: any;
}
interface IProps {
    projectId: string;
    // The ID of the parent object
    pluginId: string;
    // ID of the form parameter. For nested parameters this has the form of 'parentParam.param'.
    formParamId: string;
    // Marked as required parameter
    required: boolean;
    // The details of this parameter
    taskParameter: ITaskParameter;
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
    formHooks,
    changeHandlers,
    initialValues,
}: IProps) => {
    const errors = formHooks.errors[formParamId];
    const propertyDetails = taskParameter.param;
    const { title, description, autoCompletion } = propertyDetails;

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
            <FieldSet
                boxed
                title={
                    <Label
                        isLayoutForElement="span"
                        text={<TitleSubsection useHtmlElement="span">{title}</TitleSubsection>}
                        info={required ? "required" : ""}
                        tooltip={description && description.length <= MAXLENGTH_TOOLTIP ? description : ""}
                    />
                }
                helperText={description && description.length > MAXLENGTH_TOOLTIP ? description : ""}
            >
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
                            formHooks={formHooks}
                            changeHandlers={changeHandlers}
                            initialValues={initialValues}
                        />
                    );
                })}
            </FieldSet>
        );
    } else if (propertyDetails.parameterType === INPUT_TYPES.RESOURCE) {
        return (
            <FieldSet
                boxed
                title={
                    <Label
                        isLayoutForElement="span"
                        text={<TitleSubsection useHtmlElement="span">{title}</TitleSubsection>}
                        info={required ? "required" : ""}
                        tooltip={description && description.length <= MAXLENGTH_TOOLTIP ? description : ""}
                    />
                }
                helperText={description && description.length > MAXLENGTH_TOOLTIP ? description : ""}
                hasStateDanger={errorMessage(title, errors) ? true : false}
                messageText={errorMessage(title, errors)}
            >
                <InputMapper
                    projectId={projectId}
                    parameter={{ paramId: formParamId, param: propertyDetails }}
                    intent={errors ? Intent.DANGER : Intent.NONE}
                    onChange={changeHandlers[formParamId]}
                    initialValues={initialValues}
                />
            </FieldSet>
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
                hasStateDanger={errorMessage(title, errors)}
                messageText={errorMessage(title, errors)}
            >
                {!!autoCompletion ? (
                    <Autocomplete
                        autoCompletion={autoCompletion}
                        onSearch={handleAutoCompleteInput}
                        onChange={changeHandlers[formParamId]}
                        initialValue={
                            initialValues[formParamId] ? initialValues[formParamId] : defaultValueAsJs(propertyDetails)
                        }
                    />
                ) : (
                    <InputMapper
                        projectId={projectId}
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
