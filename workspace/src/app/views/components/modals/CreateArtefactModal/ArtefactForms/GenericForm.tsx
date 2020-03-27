import React, { useCallback, useEffect, useState } from "react";
import InputGroup from "@wrappers/blueprint/input-group";
import { FormGroup } from "@blueprintjs/core";
import { IArtefactItemProperty } from "@ducks/global/typings";
import { useForm } from "react-hook-form";

export interface IProps {
    properties: IArtefactItemProperty;

    onChange(field: string, value: any): void;
}

const transformToFormData = (properties: IArtefactItemProperty) => {
    return Object
        .keys(properties)
        .reduce((acc, key) => ({
            ...acc,
            [key]: properties[key].value || ''
        }), {})
};

export function GenericForm({onChange, properties}: IProps) {

    const [formData, setFormData] = useState(transformToFormData(properties));
    const {register, handleSubmit, errors, getValues, setValue} = useForm({
        mode: 'onChange',
    });
    const handleInputChange = ({target}: any) => {
        const {name, value} = target;
        setFormData({
            ...formData,
            [name]: value
        });
        onChange(name, value);
    };

    return <form>
        {
            Object.keys(properties).map(key =>
                <FormGroup
                    key={key}
                    inline={false}
                    label={properties[key].title}
                    labelFor={key}
                    labelInfo={properties[key].advanced ? "" : "(required)"}
                >
                    <InputGroup
                        id={key}
                        name={key}
                        onChange={handleInputChange}
                        value={formData[key]}
                    />
                </FormGroup>
            )
        }
    </form>
}
