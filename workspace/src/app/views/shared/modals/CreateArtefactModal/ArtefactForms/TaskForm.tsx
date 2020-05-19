import React, { useCallback, useEffect, useMemo, useState } from "react";
import { IArtefactItemProperty, IDetailedArtefactItem } from "@ducks/common/typings";
import { Intent } from "@wrappers/blueprint/constants";
import { INPUT_TYPES } from "../../../../../constants";
import { FieldItem, Spacing, TextArea, TextField } from "@wrappers/index";
import { FileUploadModal } from "../../FileUploadModal/FileUploadModal";
import { AdvancedOptionsArea } from "../../../AdvancedOptionsArea/AdvancedOptionsArea";
import { errorMessage, ParameterWidget } from "./ParameterWidget";
import { DataPreview } from "../../../DataPreview/DataPreview";
import { IDatasetConfigPreview } from "@ducks/shared/typings";
import { InputMapper } from "./InputMapper";
import { FieldItem } from "@wrappers/index";
import { Autocomplete } from "../../../Autocomplete/Autocomplete";
import { sharedOp } from "@ducks/shared";
import { AppToaster } from "../../../../../services/toaster";
import { requestResourcesList } from "@ducks/shared/requests";
import { FileUploader } from "../../../FileUploader/FileUploader";

export interface IProps {
    form: any;

    artefact: IDetailedArtefactItem;

    projectId: string;

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
const MAXLENGTH_TOOLTIP = 40;

/** Converts the default value to a JS value */
export const defaultValueAsJs = (property: IArtefactItemProperty): any => {
    return stringValueAsJs(property.parameterType, property.value);
};

/** Converts a string value to its typed equivalent based on the given value type. */
export const stringValueAsJs = (valueType: string, value: string | null): any => {
    let v: any = value || "";

    if (valueType === INPUT_TYPES.BOOLEAN) {
        // cast to boolean from string
        v = value === "true";
    }

    if (valueType === INPUT_TYPES.INTEGER) {
        v = +value;
    }
    return v;
};

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

/** Extracts the initial values from the parameter values of an existing task and turns them into a flat object, e.g. obj["nestedParam.param1"]. */
export const existingTaskValuesToFlatParameters = (updateTask: any) => {
    if (updateTask) {
        const result: any = {};
        const objToFlatRec = (obj: object, prefix: string) => {
            Object.entries(obj).forEach(([paramName, paramValue]) => {
                if (typeof paramValue === "object" && paramValue !== null) {
                    objToFlatRec(paramValue, paramName + ".");
                } else {
                    result[prefix + paramName] = paramValue;
                }
            });
        };
        objToFlatRec(updateTask.parameterValues, "");
        return result;
    } else {
        return {};
    }
};

/** The task creation/update form. */
export function TaskForm({ form, projectId, artefact, updateTask }: IProps) {
    const { properties, required } = artefact;
    const [selectedFileField, setSelectedFileField] = useState<string>("");
    const { register, errors, getValues, setValue, unregister, triggerValidation } = form;
    const visibleParams = Object.entries(properties).filter(([key, param]) => param.visibleInDialog);
    const [formValueKeys, setFormValueKeys] = useState<string[]>([]);
    const [fieldValues, setFieldValues] = useState<any>({});

    const initialValues = existingTaskValuesToFlatParameters(updateTask);

    // addition restriction for the hook form parameter values
    const valueRestrictions = (param: IArtefactItemProperty) => {
        if (param.parameterType === INPUT_TYPES.INTEGER) {
            return {
                pattern: {
                    value: /^[0-9]*$/,
                    message: "must be an integer number",
                },
            };
        } else {
            return {};
        }
    };

    useEffect(() => {
        // All keys (also nested ones are stores in here)
        const returnKeys: string[] = [];
        // Register all parameters
        const registerParameters = (prefix: string, params: [string, IArtefactItemProperty][]) => {
            params.forEach(([paramId, param]) => {
                const key = prefix + paramId;
                if (param.type === "object") {
                    // Nested type, only register nested atomic values
                    if (param.properties) {
                        // nested object
                        const nestedParams = Object.entries(param.properties);
                        registerParameters(key + ".", nestedParams);
                    } else {
                        console.warn(`Parameter '${key}' is of type "object", but has no parameters object defined!`);
                    }
                } else {
                    let value = defaultValueAsJs(param);
                    returnKeys.push(key);
                    register({ name: key }, { required: required.includes(key), ...valueRestrictions(param) });
                    if (updateTask) {
                        // Set existing value
                        setValue(key, updateTask.parameterValues[key]);
                    } else {
                        // Set default value
                        setValue(key, value);
                    }
                }
            });
        };
        if (!updateTask) {
            register({ name: LABEL }, { required: true });
            register({ name: DESCRIPTION });
        }
        registerParameters("", visibleParams);
        setFormValueKeys(returnKeys);

        // Unsubscribe
        return () => {
            if (!updateTask) {
                unregister(LABEL);
                unregister(DESCRIPTION);
            }
            returnKeys.forEach((key) => unregister(key));
        };
    }, [properties, register]);

    const handleChange = useCallback(
        (key) => (e) => {
            const { triggerValidation } = form;
            const value = e.target ? e.target.value : e;

            setValue(key, value);
            triggerValidation(key);
        },
        []
    );

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

    const handleAutoCompleteInput = (key: string) => async (input = "") => {
        try {
            const { autoCompletion } = properties[key];

            const list = await sharedOp.getAutocompleteResultsAsync({
                pluginId,
                parameterId: key,
                projectId,
                dependsOnParameterValues: autoCompletion.autoCompletionDependsOnParameters,
                textQuery: input,
            });

            return list;
        } catch (e) {
            AppToaster.show({
                message: e.detail,
                intent: Intent.DANGER,
                timeout: 0,
            });
        }
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

    const isFileInput = (type: string) => type === INPUT_TYPES.RESOURCE;

    const isAutocomplete = (property) => !!property.autoCompletion;

    return (
        <>
            <form>
                {updateTask ? null : (
                    <>
                        <FieldItem
                            key={LABEL}
                            labelAttributes={{
                                text: "Label",
                                info: "required",
                                htmlFor: LABEL,
                            }}
                            hasStateDanger={errorMessage("Label", errors.label) ? true : false}
                            messageText={errorMessage("Label", errors.label)}
                        >
                            <TextField
                                id={LABEL}
                                name={LABEL}
                                onChange={handleChange(LABEL)}
                                intent={errors.label ? Intent.DANGER : Intent.NONE}
                            />
                        </FieldItem>
                        <FieldItem
                            key={DESCRIPTION}
                            labelAttributes={{
                                text: "Description",
                                htmlFor: DESCRIPTION,
                            }}
                        >
                            <TextArea id={DESCRIPTION} name={DESCRIPTION} onChange={handleChange(DESCRIPTION)} />
                        </FieldItem>
                    </>
                )}
                {normalParams.map(([key, param]) => (
                    <ParameterWidget
                        key={key}
                        projectId={projectId}
                        pluginId={artefact.pluginId}
                        formParamId={key}
                        required={required.includes(key)}
                        taskParameter={{ paramId: key, param: properties[key] }}
                        onFileUploadClick={() => toggleFileUploader(key)}
                        formHooks={formHooks}
                        changeHandlers={changeHandlers}
                        initialValues={initialValues}
                    />
                ))}
                {advancedParams.length > 0 && (
                    <AdvancedOptionsArea>
                        {advancedParams.map(([key, param]) => (
                            <ParameterWidget
                                key={key}
                                projectId={projectId}
                                pluginId={artefact.pluginId}
                                formParamId={key}
                                required={required.includes(key)}
                                taskParameter={{ paramId: key, param: properties[key] }}
                                onFileUploadClick={() => toggleFileUploader(key)}
                                formHooks={formHooks}
                                changeHandlers={changeHandlers}
                                initialValues={initialValues}
                            />
                        ))}
                    </AdvancedOptionsArea>
                )}
                <button type="button" onClick={() => console.log(getValues(), errors)}>
                    Debug: Console Form data
                </button>
                {artefact.taskType === "Dataset" && (
                    <>
                        <Spacing />
                        <DataPreview
                            title={"Preview"}
                            preview={datasetConfigPreview(projectId, artefact.pluginId, getValues())}
                            externalValidation={{
                                validate: triggerValidation,
                                errorMessage: "Parameter validation failed. Please fix the issues first.",
                            }}
                        />
                    </>
                )}
            </form>
        </>
    );
}
