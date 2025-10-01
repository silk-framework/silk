import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
    IArtefactItemProperty,
    IPluginDetails,
    IPropertyAutocomplete,
    TaskPreConfiguration,
} from "@ducks/common/typings";
import { DATA_TYPES, INPUT_TYPES } from "../../../../../constants";
import { CodeEditor, FieldItem, MultiSuggestFieldSelectionProps, Spacing, Switch, TextField } from "@eccenca/gui-elements";
import { AdvancedOptionsArea } from "../../../AdvancedOptionsArea/AdvancedOptionsArea";
import { errorMessage, ExtendedParameterCallbacks, ParameterCallbacks, ParameterWidget } from "./ParameterWidget";
import { defaultValueAsJs, existingTaskValuesToFlatParameters } from "../../../../../utils/transformers";
import { useTranslation } from "react-i18next";
import CustomIdentifierInput, { handleCustomIdValidation } from "./CustomIdentifierInput";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import Loading from "../../../Loading";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../../../plugins/PluginRegistry";
import { DataPreviewProps, IDatasetConfigPreview, TaskParameters } from "../../../../plugins/plugin.types";
import { URI_PROPERTY_PARAMETER_ID, UriAttributeParameterInput } from "./UriAttributeParameterInput";
import { Keyword } from "@ducks/workspace/typings";
import { ArtefactFormParameter } from "./ArtefactFormParameter";
import { MultiTagSelect } from "../../../MultiTagSelect";
import useHotKey from "../../../HotKeyHandler/HotKeyHandler";
import utils from "@eccenca/gui-elements/src/cmem/markdown/markdown.utils";
import { commonOp } from "@ducks/common";
import { DependsOnParameterValueAny } from "./ParameterAutoCompletion";
import {FieldValues, FormContextValues} from "react-hook-form";

export const READ_ONLY_PARAMETER = "readOnly";

export interface IProps {
    form: FormContextValues<FieldValues>;

    detectChange: (key: string, val: any, oldValue: any) => void;

    artefact: IPluginDetails;

    projectId: string;

    taskId?: string;

    // This is set if this is an update form instead of a create form.
    updateTask?: UpdateTaskProps;

    parameterCallbacks: ParameterCallbacks;

    /** Called when no changes were done in the form and the ESC key is pressed. */
    goBackOnEscape?: () => any;

    /** Allows to set some config/parameters for a newly created task. */
    newTaskPreConfiguration?: Pick<
        TaskPreConfiguration,
        "metaData" | "preConfiguredParameterValues" | "preConfiguredDataParameters"
    >;

    /** If a parameter value is changed in a way that did not use the parameter widget, this must be called in order to update the value in the widget itself. */
    propagateExternallyChangedParameterValue: (fullParamId: string, value: string) => any;

    /** Shows a warning notification with the following message in the dialog popup that can be removed by the user. */
    showWarningMessage: (message: string) => void;
}

export interface UpdateTaskProps {
    // The existing parameter values
    parameterValues: {
        [key: string]: string | object;
    };
    variableTemplateValues: Record<string, any>;
    dataParameters?: {
        [key: string]: string;
    };
}

const LABEL = "label";
const DESCRIPTION = "description";
const IDENTIFIER = "id";
const TAGS = "tags";

const datasetConfigPreview = (
    projectId: string,
    pluginId: string,
    parameterValues: TaskParameters,
): IDatasetConfigPreview => {
    return {
        project: projectId,
        datasetInfo: {
            type: pluginId,
            parameters: parameterValues,
        },
    };
};

const intRegex = /^(0|-?[1-9][0-9]*)$/;
const isInt = (value) => {
    return intRegex.test(`${value}`);
};

export const PARAMETER_DOC_PREFIX = "parameter_doc_";

const extractDefaultValues = (pluginDetails: IPluginDetails): Map<string, string | null> => {
    const m = new Map<string, string | null>();
    const traverse = (parameterId: string, parameter: IArtefactItemProperty, prefix: string = "") => {
        m.set(
            prefix + parameterId,
            parameter.value && typeof parameter.value === "object"
                ? parameter.value.value
                : (parameter.value as string),
        );
        if (parameter.properties) {
            Object.entries(parameter.properties).forEach(([id, prop]) =>
                traverse(id, prop, `${prefix}${parameterId}.`),
            );
        }
    };
    Object.entries(pluginDetails.properties).forEach(([id, prop]) => traverse(id, prop));
    return m;
};

/** The task creation/update form. */
export function TaskForm({
    form,
    projectId,
    artefact,
    updateTask,
    taskId,
    detectChange,
    parameterCallbacks,
    goBackOnEscape = () => {},
    newTaskPreConfiguration,
    propagateExternallyChangedParameterValue,
    showWarningMessage
}: IProps) {
    const { properties, required: requiredRootParameters } = artefact;
    const { register, errors, getValues, setValue, unregister, triggerValidation } = form;
    const [formValueKeys, setFormValueKeys] = useState<string[]>([]);
    const dependentValues: React.MutableRefObject<Record<string, DependsOnParameterValueAny | undefined>> =
        React.useRef<Record<string, DependsOnParameterValueAny | undefined>>({});
    const dependentParameters = React.useRef<Map<string, Set<string>>>(new Map());
    const [doChange, setDoChange] = useState<boolean>(false);
    const { registerError } = useErrorHandler();
    const parameterDefaultValues = React.useRef<Map<string, string | null>>(new Map());
    parameterDefaultValues.current = extractDefaultValues(artefact);

    const addDependentParameter = React.useCallback((dependentParameter: string, dependsOn: string) => {
        const m = dependentParameters.current!;
        if (m.has(dependsOn)) {
            m.get(dependsOn)!.add(dependentParameter);
        } else {
            m.set(dependsOn, new Set([dependentParameter]));
        }
    }, []);

    const visibleParams = Object.entries(properties).filter(([key, param]) => param.visibleInDialog);
    /** Initial values, these can be reified as {label, value} or directly set. */
    const initialValues = existingTaskValuesToFlatParameters(
        updateTask
            ? updateTask
            : newTaskPreConfiguration && newTaskPreConfiguration.preConfiguredParameterValues
              ? {
                    parameterValues: newTaskPreConfiguration.preConfiguredParameterValues,
                    variableTemplateValues: {},
                }
              : undefined,
    );
    const [t] = useTranslation();
    const parameterLabels = React.useRef(new Map<string, string>());
    const { label, description } = form.watch([LABEL, DESCRIPTION]);
    const dataPreviewPlugin = pluginRegistry.pluginReactComponent<DataPreviewProps>(SUPPORTED_PLUGINS.DATA_PREVIEW);
    const escapeKeyDisabled = React.useRef(false);

    const initialTemplateFlag = React.useCallback(
        (fullParameterId: string) => {
            return updateTask?.variableTemplateValues[fullParameterId] != null;
        },
        [updateTask],
    );

    const handleEscapeKey = React.useCallback(() => {
        if (!escapeKeyDisabled.current) {
            goBackOnEscape();
        }
    }, []);

    useHotKey({ hotkey: "escape", handler: handleEscapeKey });

    const parameterLabel = React.useCallback((fullParameterId: string) => {
        return parameterLabels.current.get(fullParameterId) ?? "N/A";
    }, []);

    const extendedCallbacks: ExtendedParameterCallbacks = React.useMemo(() => {
        const parameterDefaultValueFn = (fullParameterId: string): string | null | undefined => {
            return parameterDefaultValues.current.get(fullParameterId);
        };
        let namedAnchors: string[] = [];
        if (artefact.markdownDocumentation) {
            namedAnchors = utils
                .extractNamedAnchors(artefact.markdownDocumentation)
                .filter((a) => a.startsWith(PARAMETER_DOC_PREFIX))
                .map((a) => a.substring(PARAMETER_DOC_PREFIX.length));
        }
        return {
            ...parameterCallbacks,
            initialTemplateFlag,
            parameterLabel,
            namedAnchors,
            defaultValue: parameterDefaultValueFn,
        };
    }, [initialTemplateFlag, artefact.markdownDocumentation]);

    /** Additional restrictions/validation for the form parameter values
     *  The returned error messages must be defined in such a way that the parameter label can be prefixed in front of it.
     **/
    const valueRestrictions = (fullParameterId: string, param: IArtefactItemProperty) => {
        const wrapTemplateValidation = (validationFunction: (value) => true | string): ((value) => true | string) => {
            return (value): true | string => {
                if (parameterCallbacks.templateFlag(fullParameterId)) {
                    return true; // Templates are validated via auto-completion
                } else {
                    return validationFunction(value);
                }
            };
        };
        if (param.parameterType === INPUT_TYPES.INTEGER) {
            return {
                validate: {
                    isInt: wrapTemplateValidation((value) => {
                        return isInt(value)
                            ? true
                            : (t("form.validations.integer", "must be an integer number") as string);
                    }),
                },
            };
        } else {
            return {};
        }
    };

    /** Set doChange in order to re-render/reset the task form when the project has changed. */
    useEffect(() => {
        setDoChange(true);
    }, [projectId]);

    useEffect(() => {
        if (doChange) {
            setDoChange(false);
        }
    }, [doChange]);

    /** Initialize: register parameters, set default/existing values etc. */
    useEffect(() => {
        // All keys (also nested ones are stores in here)
        const returnKeys: string[] = [];
        // Register all parameters
        const registerParameters = (
            prefix: string,
            params: [string, IArtefactItemProperty][],
            parameterValues: Record<string, any>,
            requiredParameters: string[],
        ) => {
            // Construct array of parameter keys that other parameters depend on
            const autoCompletionParams = params.filter(([key, propertyDetails]) => propertyDetails.autoCompletion);
            const dependsOnParameters = autoCompletionParams.flatMap(
                ([key, propertyDetails]) =>
                    (propertyDetails.autoCompletion as IPropertyAutocomplete).autoCompletionDependsOnParameters,
            );
            params.forEach(([paramId, param]) => {
                const fullParameterId = prefix + paramId;
                parameterLabels.current.set(fullParameterId, param.title);
                if (param.type === "object") {
                    // Nested type, only register nested atomic values
                    if (param.properties) {
                        // nested object
                        const nestedParams = Object.entries(param.properties);
                        registerParameters(
                            fullParameterId + ".",
                            nestedParams,
                            parameterValues && parameterValues[paramId] !== undefined
                                ? typeof parameterValues[paramId].value === "object"
                                    ? parameterValues[paramId].value
                                    : parameterValues[paramId]
                                : {},
                            param.required ? param.required : [],
                        );
                    } else {
                        console.warn(
                            `Parameter '${fullParameterId}' is of type "object", but has no parameters object defined!`,
                        );
                    }
                } else {
                    let value = defaultValueAsJs(param, false);
                    returnKeys.push(fullParameterId);
                    register(
                        {
                            name: fullParameterId,
                        },
                        {
                            // Boolean is by default set to false
                            required: requiredParameters.includes(paramId) && param.parameterType !== "boolean",
                            ...valueRestrictions(fullParameterId, param),
                        },
                    );
                    // Set default value
                    let currentValue = value;
                    if ((updateTask || newTaskPreConfiguration) && parameterValues[paramId] !== undefined) {
                        // Set existing value, either parameter value or variable template value
                        if (updateTask && updateTask.variableTemplateValues[fullParameterId] != null) {
                            parameterCallbacks.setTemplateFlag(fullParameterId, true);
                            currentValue = updateTask.variableTemplateValues[fullParameterId];
                        } else {
                            currentValue =
                                parameterValues[paramId].value !== undefined
                                    ? parameterValues[paramId].value
                                    : parameterValues[paramId];
                        }
                    }
                    setValue(fullParameterId, currentValue);
                    // Add dependent values
                    if (dependsOnParameters.includes(paramId)) {
                        dependentValues.current[fullParameterId] = {
                            value: currentValue,
                            isTemplate: parameterCallbacks.templateFlag(fullParameterId),
                        };
                    }
                    // Add dependent parameters
                    (param.autoCompletion?.autoCompletionDependsOnParameters ?? []).forEach((dependsOn) =>
                        addDependentParameter(fullParameterId, prefix + dependsOn),
                    );
                }
            });
        };

        if (!updateTask) {
            register({ name: LABEL }, { required: true });
            register({ name: DESCRIPTION });
            register({ name: IDENTIFIER });
            register({ name: TAGS });
        }
        if (newTaskPreConfiguration) {
            newTaskPreConfiguration.metaData?.label && setValue(LABEL, newTaskPreConfiguration.metaData?.label);
            newTaskPreConfiguration.metaData?.description &&
                setValue(DESCRIPTION, newTaskPreConfiguration.metaData?.description);
        }
        if (artefact.taskType === "Dataset") {
            register({ name: URI_PROPERTY_PARAMETER_ID });
            register({ name: READ_ONLY_PARAMETER });
            if (newTaskPreConfiguration?.preConfiguredDataParameters) {
                const dataParameters = newTaskPreConfiguration.preConfiguredDataParameters;
                if (dataParameters.readOnly) {
                    setValue(READ_ONLY_PARAMETER, dataParameters.readOnly);
                }
                if (dataParameters.uriProperty) {
                    setValue(URI_PROPERTY_PARAMETER_ID, dataParameters.uriProperty);
                }
            }
        }

        registerParameters(
            "",
            visibleParams,
            updateTask ? updateTask.parameterValues : (newTaskPreConfiguration?.preConfiguredParameterValues ?? {}),
            requiredRootParameters,
        );
        setFormValueKeys(returnKeys);

        // Unsubscribe
        return () => {
            if (!updateTask) {
                unregister(LABEL);
                unregister(DESCRIPTION);
                unregister(IDENTIFIER);
                unregister(TAGS);
            }
            returnKeys.forEach((key) => unregister(key));
        };
    }, [properties, register, projectId]);

    /** Change handler for a specific parameter. */
    const handleChange = useCallback(
        (key: string) => async (e) => {
            const { triggerValidation } = form;
            const value = e.target ? e.target.value : e;

            if (dependentValues.current[key] !== undefined) {
                // We need to update the state with a new object to trigger re-render.
                dependentValues.current[key] = {
                    value: value,
                    isTemplate: parameterCallbacks.templateFlag(key),
                };
            }
            const oldValue = getValues()[key];
            setValue(key, value);
            detectChange(key, value, oldValue);
            await triggerValidation(key);
            //verify task identifier
            if (key === IDENTIFIER) handleCustomIdValidation(t, form, registerError, value, projectId);
            if (!escapeKeyDisabled.current) {
                escapeKeyDisabled.current = true;
            }
            if (dependentParameters.current.has(key)) {
                // collect all dependent parameters
                const dependentParametersTransitiveSet = new Set<string>();
                // Dependent parameters that were actually reset
                const resetDependentParameters: string[] = []
                const collect = (currentParamId: string) => {
                    const params = dependentParameters.current?.get(currentParamId) ?? [];
                    params.forEach((p: string) => {
                        if(!dependentParametersTransitiveSet.has(p)) {
                            dependentParametersTransitiveSet.add(p);
                            collect(p);
                        }
                    });
                };
                collect(key);
                dependentParametersTransitiveSet.forEach((paramId) => {
                    const currentValue = getValues(paramId)
                    if(currentValue && paramId !== key) {
                        resetDependentParameters.push(parameterLabels.current.get(paramId) ?? paramId)
                        handleChange(paramId)("");
                        propagateExternallyChangedParameterValue(paramId, "");
                    }
                });
                if(resetDependentParameters.length) {
                    showWarningMessage(t("form.taskForm.resetMessage", {
                        parameters: resetDependentParameters.join(", "),
                        dependOn: parameterLabels.current.get(key) ?? key
                    }))
                }
            }
        },
        [],
    );

    const handleTagSelectionChange = React.useCallback(
        (params: MultiSuggestFieldSelectionProps<Keyword>) => setValue(TAGS, params),
        [],
    );
    const preConfiguredFileAndLabel =
        newTaskPreConfiguration?.preConfiguredParameterValues?.file && newTaskPreConfiguration?.metaData?.label;
    const showPreviewAutomatically =
        !!preConfiguredFileAndLabel && ["csv", "text", "json", "xml"].includes(artefact.pluginId);
    const showRawView = ["text", "json", "xml"].includes(artefact.pluginId);

    /**
     * All change handlers that will be passed to the ParameterWidget components.
     */
    const changeHandlers = useMemo(() => {
        const handlers = Object.create(null);
        formValueKeys.forEach((key) => {
            handlers[key] = handleChange(key);
        });
        return handlers;
    }, [formValueKeys]);

    const normalParams = visibleParams.filter(([k, param]) => !param.advanced);
    const advancedParams = visibleParams.filter(([k, param]) => param.advanced);
    const formHooks = { errors };

    const CodeEditorMemoed = React.useMemo(
        () => (
            <CodeEditor
                id={DESCRIPTION}
                preventLineNumbers
                name={DESCRIPTION}
                mode="markdown"
                defaultValue={description}
                onChange={handleChange(DESCRIPTION)}
            />
        ),
        [],
    );

    const datasetConfigValues = React.useCallback(() => {
        return commonOp.buildNestedTaskParameterObject(getValues());
    }, []);

    return doChange ? (
        <Loading />
    ) : (
        <div data-test-id="task-form">
            <form>
                {updateTask ? null : (
                    <>
                        <ArtefactFormParameter
                            key={LABEL}
                            projectId={projectId}
                            parameterId={LABEL}
                            label={t("form.field.label")}
                            required={true}
                            infoMessage={errorMessage("Label", errors.label)}
                            inputElementFactory={() => (
                                <TextField
                                    id={LABEL}
                                    name={LABEL}
                                    autoFocus={true}
                                    value={label ?? ""}
                                    onChange={handleChange(LABEL)}
                                    intent={!!errors.label ? "danger" : undefined}
                                    onKeyDown={(e) => {
                                        if (e.keyCode === 13) {
                                            e.preventDefault();
                                            return false;
                                        }
                                    }}
                                    escapeToBlur={true}
                                />
                            )}
                        />
                        <ArtefactFormParameter
                            projectId={projectId}
                            key={DESCRIPTION}
                            parameterId={DESCRIPTION}
                            label={t("form.field.description")}
                            helperText={
                                <p>
                                    {t("Metadata.markdownHelperText")}{" "}
                                    <a href="https://www.markdownguide.org/cheat-sheet" target="_blank">
                                        {t("Metadata.markdownHelperLinkText")}
                                    </a>
                                    .
                                </p>
                            }
                            inputElementFactory={() => CodeEditorMemoed}
                        />
                        <ArtefactFormParameter
                            projectId={projectId}
                            key={TAGS}
                            parameterId={TAGS}
                            label={t("form.field.tags")}
                            inputElementFactory={() => (
                                <MultiTagSelect
                                    projectId={projectId}
                                    handleTagSelectionChange={handleTagSelectionChange}
                                />
                            )}
                        />
                    </>
                )}
                {normalParams.map(([key, param]) => (
                    <ParameterWidget
                        key={key}
                        projectId={projectId}
                        pluginId={artefact.pluginId}
                        formParamId={key}
                        required={requiredRootParameters.includes(key)}
                        taskParameter={{
                            paramId: key,
                            param: properties[key],
                        }}
                        formHooks={formHooks}
                        changeHandlers={changeHandlers}
                        initialValues={initialValues}
                        dependentValues={dependentValues}
                        parameterCallbacks={extendedCallbacks}
                    />
                ))}
                {
                    // The read-only parameter
                    artefact.taskType === "Dataset" ? (
                        <FieldItem
                            labelProps={{
                                text: t("CreateModal.ReadOnlyParameter.label"),
                                htmlFor: READ_ONLY_PARAMETER,
                            }}
                            helperText={t("CreateModal.ReadOnlyParameter.description")}
                        >
                            <Switch
                                id={READ_ONLY_PARAMETER}
                                onChange={handleChange(READ_ONLY_PARAMETER)}
                                defaultChecked={
                                    (updateTask?.dataParameters?.readOnly ??
                                        newTaskPreConfiguration?.preConfiguredDataParameters?.readOnly ??
                                        "false") === "true"
                                }
                            />
                        </FieldItem>
                    ) : null
                }

                <AdvancedOptionsArea>
                    <CustomIdentifierInput
                        form={form}
                        onValueChange={handleChange}
                        taskId={taskId}
                        projectId={projectId}
                    />
                    {artefact.taskType === "Dataset" ? (
                        <UriAttributeParameterInput
                            onValueChange={handleChange(URI_PROPERTY_PARAMETER_ID)}
                            initialValue={
                                updateTask?.dataParameters?.uriProperty ??
                                newTaskPreConfiguration?.preConfiguredDataParameters?.uriProperty
                            }
                        />
                    ) : null}
                    {advancedParams.map(([key, param]) => (
                        <ParameterWidget
                            key={key}
                            projectId={projectId}
                            pluginId={artefact.pluginId}
                            formParamId={key}
                            required={requiredRootParameters.includes(key)}
                            taskParameter={{
                                paramId: key,
                                param: properties[key],
                            }}
                            formHooks={formHooks}
                            changeHandlers={changeHandlers}
                            initialValues={initialValues}
                            dependentValues={dependentValues}
                            parameterCallbacks={extendedCallbacks}
                        />
                    ))}
                </AdvancedOptionsArea>
                {artefact.taskType?.toLowerCase() === DATA_TYPES.DATASET && (
                    <>
                        <Spacing />
                        {dataPreviewPlugin && (
                            <dataPreviewPlugin.Component
                                title={t("pages.dataset.title")}
                                preview={datasetConfigPreview(projectId, artefact.pluginId, datasetConfigValues())}
                                externalValidation={{
                                    validate: triggerValidation,
                                    errorMessage: t(
                                        "form.validations.parameter",
                                        "Parameter validation failed. Please fix the issues first.",
                                    ),
                                }}
                                datasetConfigValues={datasetConfigValues}
                                autoLoad={showPreviewAutomatically}
                                startWithRawView={showRawView}
                            />
                        )}
                    </>
                )}
            </form>
        </div>
    );
}
