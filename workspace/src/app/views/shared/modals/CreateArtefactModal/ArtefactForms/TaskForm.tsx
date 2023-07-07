import React, { useCallback, useEffect, useMemo, useState } from "react";
import { IArtefactItemProperty, IPluginDetails, IPropertyAutocomplete } from "@ducks/common/typings";
import { DATA_TYPES, INPUT_TYPES } from "../../../../../constants";
import { FieldItem, Spacing, Switch, TextArea, TextField } from "@eccenca/gui-elements";
import { AdvancedOptionsArea } from "../../../AdvancedOptionsArea/AdvancedOptionsArea";
import { errorMessage, ParameterCallbacks, ParameterWidget } from "./ParameterWidget";
import { defaultValueAsJs, existingTaskValuesToFlatParameters } from "../../../../../utils/transformers";
import { useTranslation } from "react-i18next";
import CustomIdentifierInput, { handleCustomIdValidation } from "./CustomIdentifierInput";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import Loading from "../../../Loading";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../../../plugins/PluginRegistry";
import { DataPreviewProps, IDatasetConfigPreview } from "../../../../plugins/plugin.types";
import { URI_PROPERTY_PARAMETER_ID, UriAttributeParameterInput } from "./UriAttributeParameterInput";
import { Keyword } from "@ducks/workspace/typings";
import { SelectedParamsType } from "@eccenca/gui-elements/src/components/MultiSelect/MultiSelect";
import { ArtefactFormParameter } from "./ArtefactFormParameter";
import { MultiTagSelect } from "../../../MultiTagSelect";

export const READ_ONLY_PARAMETER = "readOnly";

export interface IProps {
    form: any;

    detectChange: (key: string, val: any, oldValue: any) => void;

    artefact: IPluginDetails;

    projectId: string;

    taskId?: string;

    // This is set if this is an update form instead of a create form.
    updateTask?: UpdateTaskProps;

    parameterCallbacks: ParameterCallbacks;
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
    parameterValues: Record<string, string>
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

/** The task creation/update form. */
export function TaskForm({ form, projectId, artefact, updateTask, taskId, detectChange, parameterCallbacks }: IProps) {
    const { properties, required: requiredRootParameters } = artefact;
    const { register, errors, getValues, setValue, unregister, triggerValidation } = form;
    const [formValueKeys, setFormValueKeys] = useState<string[]>([]);
    const dependentValues: React.MutableRefObject<Record<string, any>> = React.useRef<Record<string, any>>({});
    const [doChange, setDoChange] = useState<boolean>(false);
    const { registerError } = useErrorHandler();

    const visibleParams = Object.entries(properties).filter(([key, param]) => param.visibleInDialog);
    /** Initial values, these can be reified as {label, value} or directly set. */
    const initialValues = existingTaskValuesToFlatParameters(updateTask);
    const [t] = useTranslation();
    const parameterLabels = React.useRef(new Map<string, string>());
    const { label, description } = form.watch([LABEL, DESCRIPTION]);
    const dataPreviewPlugin = pluginRegistry.pluginReactComponent<DataPreviewProps>(SUPPORTED_PLUGINS.DATA_PREVIEW);

    const initialTemplateFlag = React.useCallback(
        (fullParameterId: string) => {
            return updateTask?.variableTemplateValues[fullParameterId] != null;
        },
        [updateTask]
    );

    const parameterLabel = React.useCallback((fullParameterId: string) => {
        return parameterLabels.current.get(fullParameterId) ?? "N/A";
    }, []);

    const extendedCallbacks = React.useMemo(() => {
        return {
            ...parameterCallbacks,
            initialTemplateFlag,
            parameterLabel,
        };
    }, [initialTemplateFlag]);

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
            requiredParameters: string[]
        ) => {
            // Construct array of parameter keys that other parameters depend on
            const autoCompletionParams = params.filter(([key, propertyDetails]) => propertyDetails.autoCompletion);
            const dependsOnParameters = autoCompletionParams.flatMap(
                ([key, propertyDetails]) =>
                    (propertyDetails.autoCompletion as IPropertyAutocomplete).autoCompletionDependsOnParameters
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
                                ? parameterValues[paramId].value
                                : {},
                            param.required ? param.required : []
                        );
                    } else {
                        console.warn(
                            `Parameter '${fullParameterId}' is of type "object", but has no parameters object defined!`
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
                        }
                    );
                    // Set default value
                    let currentValue = value;
                    if (updateTask && parameterValues[paramId] !== undefined) {
                        // Set existing value, either parameter value or variable template value
                        if (updateTask.variableTemplateValues[fullParameterId] != null) {
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
                        dependentValues.current[fullParameterId] = currentValue;
                    }
                }
            });
        };

        if (!updateTask) {
            register({ name: LABEL }, { required: true });
            register({ name: DESCRIPTION });
            register({ name: IDENTIFIER });
            register({ name: TAGS });
        }
        if (artefact.taskType === "Dataset") {
            register({ name: URI_PROPERTY_PARAMETER_ID });
            register({ name: READ_ONLY_PARAMETER });
        }
        registerParameters("", visibleParams, updateTask ? updateTask.parameterValues : {}, requiredRootParameters);
        setFormValueKeys(returnKeys);

        // Unsubscribe
        return () => {
            if (!updateTask) {
                unregister(LABEL);
                unregister(DESCRIPTION);
                unregister(IDENTIFIER);
                unregister({ name: TAGS });
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
                dependentValues.current[key] = value;
            }
            const oldValue = getValues()[key];
            setValue(key, value);
            detectChange(key, value, oldValue);
            await triggerValidation(key);
            //verify task identifier
            if (key === IDENTIFIER) handleCustomIdValidation(t, form, registerError, value, projectId);
        },
        []
    );

    const handleTagSelectionChange = React.useCallback(
        (params: SelectedParamsType<Keyword>) => setValue(TAGS, params),
        []
    );

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

    return doChange ? (
        <Loading />
    ) : (
        <div data-test-id="task-form">
            <form>
                {updateTask ? null : (
                    <>
                        <ArtefactFormParameter
                            key={LABEL}
                            parameterId={LABEL}
                            label={t("form.field.label")}
                            required={true}
                            infoMessage={errorMessage("Label", errors.label)}
                            inputElementFactory={() => (
                                <TextField
                                    id={LABEL}
                                    name={LABEL}
                                    value={label ?? ""}
                                    onChange={handleChange(LABEL)}
                                    hasStateDanger={!!errors.label}
                                    onKeyDown={(e) => {
                                        if (e.keyCode === 13) {
                                            e.preventDefault();
                                            return false;
                                        }
                                    }}
                                />
                            )}
                        />
                        <ArtefactFormParameter
                            key={DESCRIPTION}
                            parameterId={DESCRIPTION}
                            label={t("form.field.description")}
                            inputElementFactory={() => (
                                <TextArea
                                    id={DESCRIPTION}
                                    name={DESCRIPTION}
                                    value={description ?? ""}
                                    onChange={handleChange(DESCRIPTION)}
                                />
                            )}
                        />
                        <ArtefactFormParameter
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
                                defaultChecked={(updateTask?.dataParameters?.readOnly ?? "false") === "true"}
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
                            initialValue={updateTask?.dataParameters?.uriProperty}
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
                                preview={datasetConfigPreview(projectId, artefact.pluginId, getValues())}
                                externalValidation={{
                                    validate: triggerValidation,
                                    errorMessage: t(
                                        "form.validations.parameter",
                                        "Parameter validation failed. Please fix the issues first."
                                    ),
                                }}
                                datasetConfigValues={getValues}
                            />
                        )}
                    </>
                )}
            </form>
        </div>
    );
}
