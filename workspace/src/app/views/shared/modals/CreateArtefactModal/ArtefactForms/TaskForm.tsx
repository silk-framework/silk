import React, { useCallback, useEffect, useState } from "react";
import { IArtefactItemProperty, IDetailedArtefactItem } from "@ducks/common/typings";
import { Intent } from "@wrappers/blueprint/constants";
import { INPUT_TYPES } from "../../../../../constants";
import { FieldItem, TextArea, TextField } from "@wrappers/index";
import { FileUploadModal } from "../../FileUploadModal/FileUploadModal";
import { ParameterWidget } from "./ParameterWidget";

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

    useEffect(() => {
        const values = {};
        register({ name: "label" }, { required: true });
        register({ name: "description" });

        visibleParams.forEach(([key, property]) => {
            let value = defaultValueAsJs(property);

            register({ name: key }, { required: required.includes(key) });
            setValue(key, value);

            values[key] = value;
        });

        // Unsubscribe
        return () => {
            unregister("label");
            unregister("description");
            visibleParams.forEach(([key, param]) => unregister(key));
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
                        onChange={handleChange(key)}
                    />
                ))}
                <div className={"advanced"}>
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
                            onChange={handleChange(key)}
                        />
                    ))}
                </div>
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
