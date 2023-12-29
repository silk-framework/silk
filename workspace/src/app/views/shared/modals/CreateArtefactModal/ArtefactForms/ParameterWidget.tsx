import React from "react";
import {
    FieldSet,
    Label,
    Markdown,
    StringPreviewContentBlobToggler,
    TitleSubsection,
    WhiteSpaceContainer,
} from "@eccenca/gui-elements";
import { IArtefactItemProperty, ITaskParameter } from "@ducks/common/typings";
import { Intent } from "@eccenca/gui-elements/blueprint/constants";
import { InputMapper, RegisterForExternalChangesFn } from "./InputMapper";
import { defaultValueAsJs } from "../../../../../utils/transformers";
import { INPUT_TYPES } from "../../../../../constants";
import { useTranslation } from "react-i18next";
import { dependentValueIsSet, ParameterAutoCompletion } from "./ParameterAutoCompletion";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../../../plugins/PluginRegistry";
import { ParameterExtensions } from "../../../../plugins/plugin.types";
import { ArtefactFormParameter } from "./ArtefactFormParameter";
import { optionallyLabelledParameterToValue } from "../../../../taskViews/linking/linking.types";

const MAXLENGTH_TOOLTIP = 32;
const MAXLENGTH_SIMPLEHELP = 192;

interface IHookFormParam {
    errors: any;
}

/** Parameter related callbacks. */
export interface ParameterCallbacks {
    /** Register for getting external updates for values. */
    registerForExternalChanges: RegisterForExternalChangesFn;
    /** Set the template flag of a parameter. If set to true the parameter value will be handled as a variable template. */
    setTemplateFlag: (parameterId: string, isTemplate: boolean) => any;
    /** Request template flag for a parameter. */
    templateFlag: (parameterId: string) => boolean;
}

/** Extended parameter callbacks with internal callbacks. */
export interface ExtendedParameterCallbacks extends ParameterCallbacks {
    initialTemplateFlag: (parameterId: string) => boolean;
    /** Returns the label of the parameter with the full parameter ID, i.e. of the form parentParamId.paramId. */
    parameterLabel: (fullParameterId: string) => string;
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
    dependentValues: React.MutableRefObject<Record<string, any>>;
    parameterCallbacks: ExtendedParameterCallbacks;
}

/** Renders the errors message based on the error type. */
export const errorMessage = (title: string, errors: any) => {
    if (!errors) {
        return "";
    } else if (errors.type === "pattern") {
        return `${title} ${errors.message}.`;
    } else if (errors.type === "required") {
        return `${title} must be specified.`;
    } else if (errors.type === "manual") {
        return errors.message;
    } else if (errors.type) {
        // Errors should always have an error message, define fallback message though
        return errors.message ? `${title} ${errors.message}` : `Invalid value for ${title}`;
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
        parameterCallbacks,
    } = props;
    const parameterExtensions = pluginRegistry.pluginComponent<ParameterExtensions>(
        SUPPORTED_PLUGINS.DI_PARAMETER_EXTENSIONS
    );
    const errors = formHooks.errors[taskParameter.paramId];
    const propertyDetails = parameterExtensions ? parameterExtensions.extend(taskParameter.param) : taskParameter.param;
    const { title, description, autoCompletion } = propertyDetails;
    const [t] = useTranslation();

    const formParameterPrefix = formParamId.substring(0, formParamId.length - taskParameter.paramId.length);

    const dependentValue = (paramId: string) => {
        const prefixedParamId = formParameterPrefix + paramId;
        return dependentValues.current[prefixedParamId];
    };

    const missingParameterLabels = missingDependentParameters(
        taskParameter.param,
        dependentValues.current,
        formParameterPrefix
    ).map((paramId) => parameterCallbacks.parameterLabel(formParameterPrefix + paramId));
    /** Text that should be displayed below the input element for this parameter as long as there is no error message displayed. */
    const infoHelperText =
        missingParameterLabels.length > 0
            ? t("ParameterWidget.AutoComplete.missingDependsOnValuesInfo", {
                  parameterLabels: missingParameterLabels.join(", "),
              })
            : undefined;

    let propertyHelperText: JSX.Element | undefined = undefined;
    if (description && description.length > MAXLENGTH_TOOLTIP) {
        propertyHelperText = (
            <StringPreviewContentBlobToggler
                className="di__parameter_widget__description"
                content={description}
                previewMaxLength={MAXLENGTH_SIMPLEHELP}
                fullviewContent={
                    <WhiteSpaceContainer
                        marginTop="tiny"
                        marginRight="xlarge"
                        marginBottom="small"
                        marginLeft="regular"
                    >
                        <Markdown>{description ?? ""}</Markdown>
                    </WhiteSpaceContainer>
                }
                toggleExtendText={t("common.words.more", "more")}
                toggleReduceText={t("common.words.less", "less")}
                firstNonEmptyLineOnly={true}
            />
        );
    }
    const errorText = errorMessage(title, errors);

    if (propertyDetails.type === "object") {
        const requiredNestedParams = propertyDetails.required ? propertyDetails.required : [];
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
                {Object.entries(propertyDetails.properties as Record<string, IArtefactItemProperty>).map(
                    ([nestedParamId, nestedParam]) => {
                        const nestedFormParamId = `${formParamId}.${nestedParamId}`;
                        return (
                            <ParameterWidget
                                key={nestedFormParamId}
                                projectId={projectId}
                                pluginId={propertyDetails.pluginId as string}
                                formParamId={nestedFormParamId}
                                required={requiredNestedParams.includes(nestedParamId)}
                                taskParameter={{ paramId: nestedParamId, param: nestedParam }}
                                formHooks={{ errors: errors ? errors : {} }}
                                changeHandlers={changeHandlers}
                                initialValues={initialValues}
                                dependentValues={dependentValues}
                                parameterCallbacks={parameterCallbacks}
                            />
                        );
                    }
                )}
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
                hasStateDanger={!!errorMessage(title, errors)}
                messageText={errorText ? errorText : infoHelperText}
            >
                <InputMapper
                    projectId={projectId}
                    parameter={{ paramId: formParamId, param: propertyDetails }}
                    intent={errors ? Intent.DANGER : Intent.NONE}
                    onChange={changeHandlers[formParamId]}
                    initialParameterValue={initialValues[formParamId]}
                    required={required}
                    parameterCallbacks={parameterCallbacks}
                />
            </FieldSet>
        );
    } else {
        const isTemplateParameter = parameterCallbacks.initialTemplateFlag(formParamId);
        const initialValue = autoCompletion
            ? initialValues[formParamId]
                ? initialValues[formParamId].value
                : defaultValueAsJs(propertyDetails, true)
            : initialValues[formParamId]?.value ?? optionallyLabelledParameterToValue(propertyDetails.value);
        return (
            <ArtefactFormParameter
                projectId={projectId}
                label={title}
                parameterId={formParamId}
                // For boolean parameters the default is always set to false even if a default value was missing
                required={required && propertyDetails.parameterType !== "boolean"}
                tooltip={description && description.length <= MAXLENGTH_TOOLTIP ? description : undefined}
                helperText={propertyHelperText}
                infoMessage={errorText ? errorText : infoHelperText}
                infoMessageDanger={!!errorText}
                parameterType={propertyDetails.parameterType}
                supportVariableTemplateElement={{
                    onChange: changeHandlers[formParamId],
                    startWithTemplateView: isTemplateParameter,
                    showTemplatePreview: taskParameter.param.parameterType !== INPUT_TYPES.PASSWORD,
                    parameterCallbacks,
                    initialValue,
                    defaultValue: defaultValueAsJs(propertyDetails, !!autoCompletion),
                }}
                inputElementFactory={(initialValueReplace, onChange) => {
                    if (autoCompletion) {
                        const currentInitialValue = initialValueReplace ? initialValueReplace : undefined;
                        return (
                            <ParameterAutoCompletion
                                projectId={projectId}
                                paramId={taskParameter.paramId}
                                pluginId={pluginId}
                                onChange={(value) =>
                                    onChange ? onChange(value.value) : changeHandlers[formParamId](value.value)
                                }
                                initialValue={
                                    currentInitialValue ||
                                    // Only set initial value when this was not a template value
                                    (initialValues[formParamId] && !isTemplateParameter
                                        ? initialValues[formParamId]
                                        : defaultValueAsJs(propertyDetails, true))
                                }
                                autoCompletion={autoCompletion}
                                intent={errors ? Intent.DANGER : Intent.NONE}
                                formParamId={formParamId}
                                dependentValue={dependentValue}
                                required={required}
                                registerForExternalChanges={parameterCallbacks.registerForExternalChanges}
                            />
                        );
                    } else {
                        const initialParameterValue: string =
                            initialValueReplace ??
                            // Only set initial value when this was not a template value
                            (!isTemplateParameter ? initialValues[formParamId]?.value : undefined);
                        return (
                            <InputMapper
                                projectId={projectId}
                                parameter={{ paramId: formParamId, param: propertyDetails }}
                                intent={errors ? Intent.DANGER : Intent.NONE}
                                onChange={onChange ?? changeHandlers[formParamId]}
                                initialParameterValue={initialParameterValue}
                                required={required}
                                parameterCallbacks={parameterCallbacks}
                            />
                        );
                    }
                }}
            />
        );
    }
};

/** Returns an array of parameter IDs of missing dependent values. */
export const missingDependentParameters = (
    propertyDetails: IArtefactItemProperty,
    dependentValues: Record<string, any>,
    parameterPrefix: string
): string[] => {
    const dependsOnParameters = propertyDetails.autoCompletion?.autoCompletionDependsOnParameters ?? [];
    return dependsOnParameters.filter((paramId) => !dependentValueIsSet(dependentValues[parameterPrefix + paramId]));
};
