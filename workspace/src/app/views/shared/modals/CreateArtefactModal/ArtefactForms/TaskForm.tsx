import React, { useCallback, useEffect, useMemo, useState } from "react";
import { IArtefactItemProperty, IDetailedArtefactItem } from "@ducks/common/typings";
import { Intent } from "@wrappers/blueprint/constants";
import { INPUT_TYPES } from "../../../../../constants";
import { FieldItem, TextArea, TextField } from "@wrappers/index";
import { FileUploadModal } from "../../FileUploadModal/FileUploadModal";
import { AdvancedOptionsArea } from "../../../AdvancedOptionsArea/AdvancedOptionsArea";
import { errorMessage, ParameterWidget } from "./ParameterWidget";

export interface IProps {
    form: any;

    artefact: IDetailedArtefactItem;

    projectId: string;
}

/** Converts the default value to a JS value */
export const defaultValueAsJs = function (property: IArtefactItemProperty): any {
    let value: any = property.value || "";

    if (property.type === INPUT_TYPES.BOOLEAN) {
        // cast to boolean from string
        value = property.value === "true";
    }

    if (property.type === INPUT_TYPES.INTEGER) {
        value = +property.value;
    }
    return value;
};

/** The task creation/update form. */
export function TaskForm({ form, projectId, artefact }: IProps) {
    const { properties, required } = artefact;
    const [selectedFileField, setSelectedFileField] = useState<string>("");
    const { register, errors, getValues, setValue, unregister } = form;
    const visibleParams = Object.entries(properties).filter(([key, param]) => param.visibleInDialog);
    const [formValueKeys, setFormValueKeys] = useState<string[]>([]);

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
                    setValue(key, value);
                }
            });
        };
        register({ name: "label" }, { required: true });
        register({ name: "description" });
        registerParameters("", visibleParams);
        setFormValueKeys(returnKeys);

        // Unsubscribe
        return () => {
            unregister("label");
            unregister("description");
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

    const toggleFileUploader = (fieldName: string = "") => {
        setSelectedFileField(fieldName);
    };

    const normalParams = visibleParams.filter(([k, param]) => !param.advanced);
    const advancedParams = visibleParams.filter(([k, param]) => param.advanced);
    const formHooks = { errors };

    return (
        <>
            <form>
                <FieldItem
                    key="label"
                    labelAttributes={{
                        text: "Label",
                        info: "required",
                        htmlFor: "label",
                    }}
                    messageText={errorMessage("Label", errors.label)}
                >
                    <TextField
                        id={"label"}
                        name={"label"}
                        onChange={handleChange("label")}
                        intent={errors.label ? Intent.DANGER : Intent.NONE}
                    />
                </FieldItem>
                <FieldItem
                    key="description"
                    labelAttributes={{
                        text: "Description",
                        htmlFor: "description",
                    }}
                >
                    <TextArea id={"description"} name={"description"} onChange={handleChange("description")} />
                </FieldItem>

                {normalParams.map(([key, param]) => (
                    <ParameterWidget
                        key={key}
                        projectId={projectId}
                        pluginId={artefact.pluginId}
                        paramId={key}
                        required={required.includes(key)}
                        propertyDetails={properties[key]}
                        onFileUploadClick={() => toggleFileUploader(key)}
                        formHooks={formHooks}
                        changeHandlers={changeHandlers}
                    />
                ))}
                {advancedParams.length > 0 && (
                    <AdvancedOptionsArea>
                        {advancedParams.map(([key, param]) => (
                            <ParameterWidget
                                key={key}
                                projectId={projectId}
                                pluginId={artefact.pluginId}
                                paramId={key}
                                required={required.includes(key)}
                                propertyDetails={properties[key]}
                                onFileUploadClick={() => toggleFileUploader(key)}
                                formHooks={formHooks}
                                changeHandlers={changeHandlers}
                            />
                        ))}
                    </AdvancedOptionsArea>
                )}
                <button type="button" onClick={() => console.log(getValues(), errors)}>
                    Debug: Console Form data
                </button>
            </form>

            <FileUploadModal
                isOpen={!!selectedFileField}
                onDiscard={() => toggleFileUploader("")}
                onUploaded={handleChange(selectedFileField)}
            />
        </>
    );
}
