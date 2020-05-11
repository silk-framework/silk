import React from "react";
import { INPUT_TYPES } from "../../../../../constants";
import { NumericInput, Switch, TextField, TextArea } from "@wrappers/index";
import { QueryEditor } from "../../../QueryEditor/QueryEditor";

interface IProps {
    type: string;
    inputAttributes: {
        id?: string;
        name: string;
        value?: string;
        intent?: any;
        onChange(e: any): void;
        style?: any;
    };
}

export function InputMapper(props: IProps) {
    const { type, inputAttributes } = props;

    switch (type) {
        case INPUT_TYPES.BOOLEAN:
            return <Switch {...inputAttributes} />;
        case INPUT_TYPES.INTEGER:
            return <NumericInput {...inputAttributes} buttonPosition={"none"} />;
        case INPUT_TYPES.TEXTAREA:
            return <TextArea {...inputAttributes} />;
        case INPUT_TYPES.MULTILINE_STRING:
            return <QueryEditor {...inputAttributes} />;
        case INPUT_TYPES.PASSWORD:
            return <TextField {...inputAttributes} type={"password"} />;
        case INPUT_TYPES.RESOURCE:
            // File uploader handled in TaskForm
            return null;
        case INPUT_TYPES.ENUMERATION:
        case INPUT_TYPES.OPTION_INT:
        case INPUT_TYPES.STRING:
        default:
            return <TextField {...inputAttributes} />;
    }
}
