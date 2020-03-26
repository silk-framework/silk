import React, { useCallback, useState } from "react";
import InputGroup from "@wrappers/blueprint/input-group";
import { FormGroup } from "@blueprintjs/core";
import { IArtefactItemProperty } from "@ducks/global/typings";
import { useForm } from "react-hook-form";

type IFormData = IArtefactItemProperty;

export interface IProps {
    properties: IArtefactItemProperty;
    onChange(formData: IFormData): void;
}

export function GenericForm({onChange, properties}: IProps) {
    const [formData, setFormData] = useState<IArtefactItemProperty>(properties);
    // const { register, handleSubmit, errors } = useForm();

    const handleInputChange = (e: any) => {
        console.log(e.target.name, formData);
        const updated = {
            ...formData,
            [e.target.name]: {
                ...formData[e.target.name],
                value: e.target.value
            }
        };
        setFormData(updated);
        // onChange(updated);
    };

    return <>
        {
            Object.keys(formData).map(key =>
                <FormGroup
                    key={key}
                    inline={false}
                    label={formData[key].title}
                    labelFor={key}
                    labelInfo={formData[key].advanced ? "" : "(required)"}
                >
                    <InputGroup
                        id={key}
                        name={key}
                        onChange={handleInputChange}
                        value={formData[key].value || ''}
                    />
                </FormGroup>
            )
        }
    </>
}
