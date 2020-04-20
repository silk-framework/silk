import React, { useCallback, useEffect, useState } from "react";
import { FormGroup } from "@blueprintjs/core";
import { IDetailedArtefactItem } from "@ducks/common/typings";
import { Intent } from "@wrappers/blueprint/constants";
import { INPUT_VALID_TYPES } from "../../../../../constants";
import { InputMapper } from "./InputMapper";

export interface IProps {
    form: any;

    artefact: IDetailedArtefactItem;

    projectId: string;
}

export function TaskForm({form, projectId, artefact}: IProps) {
    const {properties, required, key: artefactId} = artefact;

    const [fieldValues, setFieldValues] = useState<any>({});
    const {register, errors, getValues, setValue, unregister} = form;

    useEffect(() => {
        const values = {};
        register({name: 'label'}, {required: true});
        register({name: 'description'});

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

            register({name: key}, {required: required.includes(key)});
            setValue(key, value);

            values[key] = value;
        });

        setFieldValues(values);

        // Unsubscribe
        return () =>  {
            unregister('label');
            unregister('description');
            Object.keys(properties).map(key => unregister(key));
        };

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
        <FormGroup
            key='label'
            inline={false}
            label='Label'
            labelFor={'label'}
            labelInfo="(required)"
        >
            <InputMapper
                type={'string'}
                inputAttributes={{
                    id: 'label',
                    name: 'label',
                    onChange: handleChange('label'),
                    intent: errors.label ? Intent.DANGER : Intent.NONE
                }}
            />
        </FormGroup>
        <FormGroup
            key='description'
            inline={false}
            label='Description'
            labelFor={'description'}
        >
            <InputMapper
                type={'textarea'}
                inputAttributes={{
                    id: 'description',
                    name: 'description',
                    onChange: handleChange('description'),
                    style: {
                        width: '100%'
                    }
                }}
            />
        </FormGroup>

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
                        type={properties[key].type}
                        extraInfo={{
                            autoCompletion: properties[key].autoCompletion,
                            artefactId,
                            projectId,
                            parameterId: key
                        }}
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
