import React from "react";
import { INPUT_TYPES } from "../../../../../constants";
import { NumericInput, Switch, TextField, TextArea } from "@wrappers/index";
import { QueryEditor } from "../../../QueryEditor/QueryEditor";
import { ITaskParameter } from "@ducks/common/typings";
import { Intent } from "@blueprintjs/core";

interface IInputMapper {
    parameter: ITaskParameter;
    onChange: (e: any) => void;
    // Blueprint intent
    intent: Intent;
}

export function InputMapper({ parameter, onChange, intent }: IInputMapper) {
    const { paramId, param } = parameter;
    const inputAttributes = {
        id: paramId,
        name: param.title || paramId,
        onChange: onChange,
        value: param.value,
        intent: intent,
    };

    if (param.type === "object") {
        return <TextField {...inputAttributes} />;
    } else {
        switch (param.parameterType) {
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
}
