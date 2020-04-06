import React, { useCallback, useEffect, useState } from "react";
import { FormGroup } from "@blueprintjs/core";
import { IArtefactItem, IArtefactItemProperty } from "@ducks/global/typings";
import { Intent } from "@wrappers/blueprint/constants";
import { INPUT_VALID_TYPES } from "../../../../../constants";
import { InputMapper } from "./InputMapper";

export interface IProps {
    form: any;

    artefact: IArtefactItem;

    projectId: string;
}

export function TaskForm({form, projectId, artefact}: IProps) {
    const {properties, required, key: artefactId} = artefact;

    const [fieldValues, setFieldValues] = useState<any>({});
    const {register, errors, getValues, setValue} = form;

    useEffect(() => {
        const values = {};

        Object.keys(properties).map(key => {
            const property = properties[key];
            let value: any = property.value || '';

            if (property.type === INPUT_VALID_TYPES.BOOLEAN) {
                // cast to boolean from string
                value = property.value === 'true';
            }

            if (property.type === INPUT_VALID_TYPES.INTEGER) {
                value = +property.value;
            }

            // @Note: Register should work there for 3rd party libs such a Codemirror
            register({name: key}, {required: required.includes(key)});
            setValue(key, value);

            values[key] = value;
        });

        setFieldValues(values);

    }, [properties, register]);

    const handleChange = useCallback((key) => (e) => {
        const {triggerValidation} = form;
        const value = e.target ? e.target.value : e;

        setValue(key, value);
        setFieldValues({
            ...fieldValues,
            [key]: value
        });
        triggerValidation(key);

    }, [fieldValues]);

    return <form>
        {
            Object.keys(properties).map(key =>
                <FormGroup
                    key={key}
                    inline={false}
                    label={properties[key].title}
                    labelFor={key}
                    labelInfo={required.includes(key) ? "(required)" : ""}
                >
                    <InputMapper
                        inputAttributes={{
                            id: key,
                            name: key,
                            onChange: handleChange(key),
                            value: fieldValues[key],
                            intent: errors[key] ? Intent.DANGER : Intent.NONE
                        }}
                        property={properties[key]}
                        artefactId={artefactId}
                        projectId={projectId}
                        parameterId={key}
                    />
                    {
                        errors[key] && <span style={{color: 'red'}}>{properties[key].title} not specified</span>
                    }
                </FormGroup>
            )
        }
        <button type='button' onClick={() => console.log(getValues(), errors)}>Console Form data</button>
    </form>
}
