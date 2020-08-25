import React from "react";
import ReactMarkdown from "react-markdown";
import { sharedOp } from "@ducks/shared";
import { ITaskParameter } from "@ducks/common/typings";
import {
    Accordion,
    AccordionItem,
    FieldItem,
    FieldSet,
    HtmlContentBlock,
    OverflowText,
    TitleSubsection,
    Label,
} from "@gui-elements/index";
import { Intent } from "@gui-elements/blueprint/constants";
import { Autocomplete } from "../../../Autocomplete/Autocomplete";
import { InputMapper } from "./InputMapper";
import { AppToaster } from "../../../../../services/toaster";
import { defaultValueAsJs } from "../../../../../utils/transformers";
import { INPUT_TYPES } from "../../../../../constants";
import { useTranslation } from "react-i18next";

const MAXLENGTH_TOOLTIP = 40;
const MAXLENGTH_SIMPLEHELP = 288;

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
        [key: string]: any;
    };
    // Values that the auto-completion of other parameters depends on
    dependentValues: {
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
export const ParameterWidget = (props: IProps) => {
    const {
        projectId,
        pluginId,
        formParamId,
        required,
        taskParameter,
        formHooks,
        changeHandlers,
        initialValues,
        dependentValues,
    } = props;
    const errors = formHooks.errors[taskParameter.paramId];
    const propertyDetails = taskParameter.param;
    const { title, description, autoCompletion } = propertyDetails;
    const [t] = useTranslation();

    const selectDependentValues = (): string[] => {
        return autoCompletion.autoCompletionDependsOnParameters.flatMap((paramId) => {
            const prefixedParamId =
                formParamId.substring(0, formParamId.length - taskParameter.paramId.length) + paramId;
            if (dependentValues[prefixedParamId]) {
                return [dependentValues[prefixedParamId]];
            } else {
                return [];
            }
        });
    };

    const handleAutoCompleteInput = async (input: string = "") => {
        try {
            return await sharedOp.getAutocompleteResultsAsync({
                pluginId: pluginId,
                parameterId: taskParameter.paramId,
                projectId,
                dependsOnParameterValues: selectDependentValues(),
                textQuery: input,
                limit: 100, // The auto-completion is only showing the first n values TODO: Make auto-completion list scrollable?
            });
        } catch (e) {
            if (e.isHttpError && e.httpStatus !== 400) {
                // For now hide 400 errors from user, since they are not helpful.
                AppToaster.show({
                    message: e.errorResponse.detail,
                    intent: Intent.DANGER,
                    timeout: 0,
                });
            } else {
                console.warn(e);
            }
            return [];
        }
    };

    let propertyHelperText = null;
    if (description && description.length > MAXLENGTH_TOOLTIP) {
        propertyHelperText =
            description.length > MAXLENGTH_SIMPLEHELP ? (
                <Accordion align="end">
                    <AccordionItem
                        title={<OverflowText inline>{description}</OverflowText>}
                        fullWidth
                        condensed
                        noBorder
                    >
                        <HtmlContentBlock>
                            <ReactMarkdown source={description} />
                        </HtmlContentBlock>
                    </AccordionItem>
                </Accordion>
            ) : (
                description
            );
    }

    if (propertyDetails.type === "object") {
        const requiredNestedParams = taskParameter.param.required ? taskParameter.param.required : [];
        return (
            <FieldSet
                boxed
                title={
                    <Label
                        isLayoutForElement="span"
                        text={<TitleSubsection useHtmlElement="span">{title}</TitleSubsection>}
                        info={required ? t("common.words.required") : ""}
                        tooltip={description && description.length <= MAXLENGTH_TOOLTIP ? description : ""}
                    />
                }
                helperText={propertyHelperText}
            >
                {Object.entries(propertyDetails.properties).map(([nestedParamId, nestedParam]) => {
                    const nestedFormParamId = `${formParamId}.${nestedParamId}`;
                    return (
                        <ParameterWidget
                            key={nestedFormParamId}
                            projectId={projectId}
                            pluginId={propertyDetails.pluginId}
                            formParamId={nestedFormParamId}
                            required={requiredNestedParams.includes(nestedParamId)}
                            taskParameter={{ paramId: nestedParamId, param: nestedParam }}
                            formHooks={{ errors: errors ? errors : {} }}
                            changeHandlers={changeHandlers}
                            initialValues={initialValues}
                            dependentValues={dependentValues}
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
                        info={required ? t("common.words.required") : ""}
                        tooltip={description && description.length <= MAXLENGTH_TOOLTIP ? description : ""}
                    />
                }
                helperText={propertyHelperText}
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
                    info: required ? t("common.words.required") : "",
                    htmlFor: formParamId,
                    tooltip: description && description.length <= MAXLENGTH_TOOLTIP ? description : "",
                }}
                helperText={propertyHelperText}
                hasStateDanger={errorMessage(title, errors) ? true : false}
                messageText={errorMessage(title, errors)}
            >
                {!!autoCompletion ? (
                    <Autocomplete
                        autoCompletion={autoCompletion}
                        onSearch={handleAutoCompleteInput}
                        onChange={changeHandlers[formParamId]}
                        initialValue={
                            initialValues[formParamId]
                                ? initialValues[formParamId]
                                : { value: defaultValueAsJs(propertyDetails) }
                        }
                        dependentValues={selectDependentValues()}
                        inputProps={{
                            name: formParamId,
                            id: formParamId,
                            intent: errors ? Intent.DANGER : Intent.NONE,
                        }}
                        resetPossible={!required}
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
