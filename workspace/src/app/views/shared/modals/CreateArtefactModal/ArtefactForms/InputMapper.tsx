import React from "react";
import { IPropertyAutocomplete } from "@ducks/common/typings";
import { INPUT_VALID_TYPES } from "../../../../../constants";
import { NumericInput, Switch, TextField, TextArea } from "@wrappers/index";
import { QueryEditor } from "../../../QueryEditor/QueryEditor";
import { Autocomplete } from "../../../Autocomplete/Autocomplete";
import { FileUploader } from "../../../FileUploader/FileUploader";

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

    extraInfo?: {
        autoCompletion?: IPropertyAutocomplete;
        artefactId: string;
        parameterId: string;
        projectId: string | null;
    };
}

export function InputMapper(props: IProps) {
    const { type, inputAttributes, extraInfo } = props;

    if (extraInfo) {
        if (extraInfo.autoCompletion) {
            // @ts-ignore
            return <Autocomplete {...extraInfo} {...inputAttributes} />;
        }
    }

    const handleFileAdded = () => {};

    switch (type) {
        case INPUT_VALID_TYPES.BOOLEAN:
            return <Switch {...inputAttributes} />;
        case INPUT_VALID_TYPES.INTEGER:
            return <NumericInput {...inputAttributes} buttonPosition={"none"} />;
        case INPUT_VALID_TYPES.TEXTAREA:
            return <TextArea {...inputAttributes} />;
        case INPUT_VALID_TYPES.MULTILINE_STRING:
            return <QueryEditor {...inputAttributes} />;
        case INPUT_VALID_TYPES.PASSWORD:
            return <TextField {...inputAttributes} type={"password"} />;
        case INPUT_VALID_TYPES.RESOURCE:
            return <FileUploader simpleInput={true} />;
        case INPUT_VALID_TYPES.ENUMERATION:
        case INPUT_VALID_TYPES.OPTION_INT:
        case INPUT_VALID_TYPES.STRING:
        default:
            return <TextField {...inputAttributes} />;
    }
}
