import React, { useState } from "react";
import InputGroup from "@wrappers/blueprint/input-group";
import { FormGroup } from "@blueprintjs/core";
import TextArea from "@wrappers/blueprint/textarea";
import { IArtefactItemProperty } from "@ducks/global/typings";

type IFormData = IArtefactItemProperty;

export interface IProps {
    properties: IArtefactItemProperty;
    onChange(formData: IFormData): void;
}

export function GenericForm({onChange, properties}: IProps) {
    const [formData, setFormData] = useState<IArtefactItemProperty>(properties);

    const handleInputChange = (key: string, value: string) => {
        const updated = {
            ...formData,
            [key]: {
                ...formData[key],
                value
            }
        };
        setFormData(updated);
        onChange(updated);
    };

    return <>
        {
            Object.keys(formData).map(key =>
                <FormGroup
                    inline={false}
                    label={formData[key].title}
                    labelFor={key}
                    labelInfo={formData[key].advanced ? "" : "(required)"}
                >
                    <InputGroup
                        id={key}
                        placeholder={formData[key].title}
                        onChange={e => handleInputChange(key, e.target.value)}
                        value={formData[key].value}
                    />
                </FormGroup>
            )
        }
    </>
}
