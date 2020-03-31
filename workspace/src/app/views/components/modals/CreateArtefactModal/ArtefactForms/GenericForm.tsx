import React, { useEffect } from "react";
import InputGroup from "@wrappers/blueprint/input-group";
import { FormGroup } from "@blueprintjs/core";
import { IArtefactItemProperty } from "@ducks/global/typings";
import { Switch, NumericInput, QueryEditor } from "@wrappers/index";
import { Intent } from "@wrappers/blueprint/constants";

export interface IProps {
    form: any;

    properties: IArtefactItemProperty;

    required: string[];
}

const InputMapper = ({type, ...props}) => {
    switch (type) {
        case "boolean":
            return <Switch {...props}/>;
        case "int":
            return <NumericInput {...props} buttonPosition={'none'}/>;
        case "multiline string":
            return <QueryEditor {...props} />;
        case "string":
        default:
            return <InputGroup {...props} />
    }
};

export function GenericForm({properties, form, required}: IProps) {
    const {register, errors, getValues, setValue} = form;

    useEffect(() => {
        Object.keys(properties).map(key => {
            const property = properties[key];
            let value: any = property.value;

            if (property.type === 'boolean') {
                value = property.value === 'true';
            }

            setValue(key, value)
        });
    }, [properties, register]);

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
                    {
                        properties[key].type === 'multiline string'
                            ? <QueryEditor
                                name={key}
                                onChange={(value: string) => setValue(key, value)}
                            />
                            : <InputMapper
                                id={key}
                                name={key}
                                type={properties[key].type}
                                inputRef={register({
                                    required: required.includes(key)
                                })}
                                intent={errors[key] ? Intent.DANGER : Intent.NONE}
                            />
                    }
                    {
                        errors[key] && <span style={{color: 'red'}}>{properties[key].title} not specified</span>
                    }
                </FormGroup>
            )
        }
        <button type='button' onClick={() => console.log(getValues(), errors)}>Console Form data</button>
    </form>
}
