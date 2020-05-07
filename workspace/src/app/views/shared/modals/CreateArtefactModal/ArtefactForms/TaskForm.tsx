import React, { useCallback, useEffect, useState } from "react";
import { IDetailedArtefactItem } from "@ducks/common/typings";
import { Intent } from "@wrappers/blueprint/constants";
import { INPUT_TYPES } from "../../../../../constants";
import { InputMapper } from "./InputMapper";
import { Button, FieldItem } from "@wrappers/index";
import { FileUploadModal } from "../../FileUploadModal/FileUploadModal";
import { Autocomplete } from "../../../Autocomplete/Autocomplete";
import { sharedOp } from "@ducks/shared";
import { AppToaster } from "../../../../../services/toaster";

export interface IProps {
    form: any;

    artefact: IDetailedArtefactItem;

    projectId: string;
}

export function TaskForm({ form, projectId, artefact }: IProps) {
    const { properties, required, pluginId } = artefact;

    const [selectedFileField, setSelectedFileField] = useState<string>("");
    const [fieldValues, setFieldValues] = useState<any>({});

    const { register, errors, getValues, setValue, unregister } = form;

    useEffect(() => {
        const values = {};
        register({ name: "label" }, { required: true });
        register({ name: "description" });

        Object.keys(properties).forEach((key) => {
            const property = properties[key];
            let value: any = property.value || "";

            if (property.type === INPUT_TYPES.BOOLEAN) {
                // cast to boolean from string
                value = property.value === "true";
            }

            if (property.type === INPUT_TYPES.INTEGER) {
                value = +property.value;
            }

            register({ name: key }, { required: required.includes(key) });
            setValue(key, value);

            values[key] = value;
        });

        setFieldValues(values);

        // Unsubscribe
        return () => {
            unregister("label");
            unregister("description");
            Object.keys(properties).map((key) => unregister(key));
        };
    }, [properties, register]);

    const handleChange = useCallback(
        (key) => (e) => {
            const { triggerValidation } = form;
            const value = e.target ? e.target.value : e;

            setValue(key, value);
            setFieldValues({
                ...fieldValues,
                [key]: value,
            });
            triggerValidation(key);
        },
        []
    );

    const toggleFileUploader = (fieldName: string = "") => {
        setSelectedFileField(fieldName);
    };

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

    const isFileInput = (type: string) => type === INPUT_TYPES.RESOURCE;
    const isAutocomplete = (property) => !!property.autoCompletion;

    const MAXLENGTH_TOOLTIP = 40;

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
                    <InputMapper
                        type={"string"}
                        inputAttributes={{
                            id: "label",
                            name: "label",
                            onChange: handleChange("label"),
                            intent: errors.label ? Intent.DANGER : Intent.NONE,
                        }}
                    />
                </FieldItem>
                <FieldItem
                    key="description"
                    labelAttributes={{
                        text: "Description",
                        htmlFor: "description",
                    }}
                >
                    <InputMapper
                        type={"textarea"}
                        inputAttributes={{
                            id: "description",
                            name: "description",
                            onChange: handleChange("description"),
                        }}
                    />
                </FieldItem>

                {Object.keys(properties).map((key) => (
                    <FieldItem
                        key={key}
                        labelAttributes={{
                            text: properties[key].title,
                            info: required.includes(key) ? "required" : "",
                            htmlFor: key,
                            tooltip:
                                properties[key].description && properties[key].description.length <= MAXLENGTH_TOOLTIP
                                    ? properties[key].description
                                    : "",
                        }}
                        helperText={
                            properties[key].description && properties[key].description.length > MAXLENGTH_TOOLTIP
                                ? properties[key].description
                                : ""
                        }
                        messageText={errors[key] ? properties[key].title + " not specified" : ""}
                        hasStateDanger={errors[key]}
                    >
                        {isFileInput(properties[key].parameterType) ? (
                            <Button onClick={() => toggleFileUploader(key)}>Upload new {properties[key].title}</Button>
                        ) : isAutocomplete(properties[key]) ? (
                            <Autocomplete
                                autoCompletion={properties[key].autoCompletion}
                                onInputChange={handleAutoCompleteInput(key)}
                                onChange={handleChange(key)}
                                value={fieldValues[key]}
                            />
                        ) : (
                            <InputMapper
                                inputAttributes={{
                                    id: key,
                                    name: properties[key].title || key,
                                    onChange: handleChange(key),
                                    value: fieldValues[key],
                                    intent: errors[key] ? Intent.DANGER : Intent.NONE,
                                }}
                                type={properties[key].parameterType}
                            />
                        )}
                    </FieldItem>
                ))}
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
