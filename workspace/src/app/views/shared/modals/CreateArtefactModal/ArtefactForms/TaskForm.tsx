import React, { useCallback, useEffect, useMemo, useState } from "react";
import { IArtefactItemProperty, IDetailedArtefactItem, IPropertyAutocomplete } from "@ducks/common/typings";
import { Intent } from "gui-elements/blueprint/constants";
import { DATA_TYPES, INPUT_TYPES } from "../../../../../constants";
import { FieldItem, Spacing, TextArea, TextField } from "gui-elements";
import { AdvancedOptionsArea } from "../../../AdvancedOptionsArea/AdvancedOptionsArea";
import { errorMessage, ParameterWidget } from "./ParameterWidget";
import { defaultValueAsJs, existingTaskValuesToFlatParameters } from "../../../../../utils/transformers";
import { useTranslation } from "react-i18next";
import CustomIdentifierInput, { handleCustomIdValidation } from "./CustomIdentifierInput";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import Loading from "../../../Loading";
import { SUPPORTED_PLUGINS, pluginRegistry } from "../../../../plugins/PluginRegistry";
import {DataPreviewProps, IDatasetConfigPreview} from "../../../../plugins/plugin.types";

export interface IProps {
    form: any;

    detectChange: (key: string, val: any, oldValue: any) => void;

    artefact: IDetailedArtefactItem;

    projectId: string;

    taskId?: string;

    // This is set if this is an update form instead of a create form.
    updateTask?: {
        // The existing parameter values
        parameterValues: {
            [key: string]: string | object;
        };
    };
}

const LABEL = "label";
const DESCRIPTION = "description";
const IDENTIFIER = "id";

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

/** The task creation/update form. */
export function TaskForm({ form, projectId, artefact, updateTask, taskId, detectChange }: IProps) {
    const { properties, required: requiredRootParameters } = artefact;
    const { register, errors, getValues, setValue, unregister, triggerValidation } = form;
    const [formValueKeys, setFormValueKeys] = useState<string[]>([]);
    const [dependentValues, setDependentValues] = useState<Record<string, any>>({});
    const [doChange, setDoChange] = useState<boolean>(false);
    const { registerError } = useErrorHandler();

    const visibleParams = Object.entries(properties).filter(([key, param]) => param.visibleInDialog);
    const initialValues = existingTaskValuesToFlatParameters(updateTask);
    const [t] = useTranslation();
    const { label, description } = form.watch([LABEL, DESCRIPTION]);
    const dataPreviewPlugin = pluginRegistry.pluginComponent<DataPreviewProps>(SUPPORTED_PLUGINS.DATA_PREVIEW);

    // addition restriction for the hook form parameter values
    const valueRestrictions = (param: IArtefactItemProperty) => {
        if (param.parameterType === INPUT_TYPES.INTEGER) {
            return {
                pattern: {
                    value: /^[0-9]*$/,
                    message: t("form.validations.integer", "must be an integer number"),
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
                const key = prefix + paramId;
                if (param.type === "object") {
                    // Nested type, only register nested atomic values
                    if (param.properties) {
                        // nested object
                        const nestedParams = Object.entries(param.properties);
                        registerParameters(
                            key + ".",
                            nestedParams,
                            parameterValues && parameterValues[paramId] !== undefined
                                ? parameterValues[paramId].value
                                : {},
                            param.required ? param.required : []
                        );
                    } else {
                        console.warn(`Parameter '${key}' is of type "object", but has no parameters object defined!`);
                    }
                } else {
                    let value = defaultValueAsJs(param);
                    returnKeys.push(key);
                    register(
                        {
                            name: key,
                        },
                        {
                            required: requiredParameters.includes(paramId),
                            ...valueRestrictions(param),
                        }
                    );
                    // Set default value
                    let currentValue = value;
                    if (updateTask && parameterValues[paramId] !== undefined) {
                        // Set existing value
                        currentValue = parameterValues[paramId].value;
                    }
                    setValue(key, currentValue);
                    // Add dependent values, the object state needs to be mutably changed, see comments in handleChange()
                    if (dependsOnParameters.includes(paramId)) {
                        dependentValues[key] = currentValue;
                    }
                }
            });
        };

        if (!updateTask) {
            register({ name: LABEL }, { required: true });
            register({ name: DESCRIPTION });
            register({ name: IDENTIFIER });
        }
        registerParameters("", visibleParams, updateTask ? updateTask.parameterValues : {}, requiredRootParameters);
        setFormValueKeys(returnKeys);

        // Unsubscribe
        return () => {
            if (!updateTask) {
                unregister(LABEL);
                unregister(DESCRIPTION);
                unregister(IDENTIFIER);
            }
            returnKeys.forEach((key) => unregister(key));
        };
    }, [properties, register, projectId]);

    /** Change handler for a specific parameter. */
    const handleChange = useCallback(
        (key) => async (e) => {
            const { triggerValidation } = form;
            const value = e.target ? e.target.value : e;

            if (dependentValues[key] !== undefined) {
                // This is rather a hack, since the callback is memoized the clojure always captures the initial (empty) object, thus we need the state object to be mutable.
                dependentValues[key] = value;
                // We still need to update the state with a new object to trigger re-render though.
                setDependentValues({
                    ...dependentValues,
                });
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

    /**
     * All change handlers that will be passed to the ParameterWidget components.
     */
    const changeHandlers = useMemo(() => {
        const handlers = {};
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
                        <FieldItem
                            key={LABEL}
                            labelAttributes={{
                                text: t("form.field.label"),
                                info: t("common.words.required"),
                                htmlFor: LABEL,
                            }}
                            hasStateDanger={!!errorMessage("Label", errors.label)}
                            messageText={errorMessage("Label", errors.label)}
                        >
                            <TextField
                                id={LABEL}
                                name={LABEL}
                                value={label ?? ""}
                                onChange={handleChange(LABEL)}
                                intent={errors.label ? Intent.DANGER : Intent.NONE}
                                onKeyDown={(e) => {
                                    if (e.keyCode === 13) {
                                        e.preventDefault();
                                        return false;
                                    }
                                }}
                            />
                        </FieldItem>
                        <FieldItem
                            key={DESCRIPTION}
                            labelAttributes={{
                                text: t("form.field.description"),
                                htmlFor: DESCRIPTION,
                            }}
                        >
                            <TextArea
                                id={DESCRIPTION}
                                name={DESCRIPTION}
                                value={description ?? ""}
                                onChange={handleChange(DESCRIPTION)}
                            />
                        </FieldItem>
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
                    />
                ))}

                <AdvancedOptionsArea>
                    <CustomIdentifierInput
                        form={form}
                        onValueChange={handleChange}
                        taskId={taskId}
                        projectId={projectId}
                    />
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
