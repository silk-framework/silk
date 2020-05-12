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

    const visibleParams = Object.entries(properties).filter(([key, param]) => param.visibleInDialog);

    useEffect(() => {
        const values = {};
        register({ name: "label" }, { required: true });
        register({ name: "description" });

        visibleParams.forEach(([key, property]) => {
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
            visibleParams.forEach(([key, param]) => unregister(key));
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

    const ParameterWidget = ({ paramId, param }) => {
        return (
            <FieldItem
                labelAttributes={{
                    text: param.title,
                    info: required.includes(paramId) ? "required" : "",
                    htmlFor: paramId,
                    tooltip:
                        param.description && param.description.length <= MAXLENGTH_TOOLTIP ? param.description : "",
                }}
                helperText={param.description && param.description.length > MAXLENGTH_TOOLTIP ? param.description : ""}
                messageText={errors[paramId] ? param.title + " not specified" : ""}
                hasStateDanger={errors[paramId]}
            >
                {isFileInput(param.parameterType) ? (
                    <Button onClick={() => toggleFileUploader(paramId)}>Upload new {param.title}</Button>
                ) : isAutocomplete(param) ? (
                    <Autocomplete
                        autoCompletion={param.autoCompletion}
                        onInputChange={handleAutoCompleteInput(paramId)}
                        onChange={handleChange(paramId)}
                        value={fieldValues[paramId]}
                    />
                ) : (
                    <InputMapper
                        inputAttributes={{
                            id: paramId,
                            name: param.title || paramId,
                            onChange: handleChange(paramId),
                            value: fieldValues[paramId],
                            intent: errors[paramId] ? Intent.DANGER : Intent.NONE,
                        }}
                        type={param.parameterType}
                    />
                )}
            </FieldItem>
        );
    };

    const normalParams = visibleParams.filter(([k, param]) => !param.advanced);
    const advancedParams = visibleParams.filter(([k, param]) => param.advanced);

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

                {normalParams.map(([key, param]) => (
                    <ParameterWidget key={key} paramId={key} param={param} />
                ))}
                <div className={"advanced"}>
                    {advancedParams.map(([key, param]) => (
                        <ParameterWidget key={key} paramId={key} param={param} />
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
