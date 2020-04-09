import React from "react";
import { IArtefactItemProperty, IPropertyAutocomplete } from "@ducks/global/typings";
import { INPUT_VALID_TYPES } from "../../../../../constants";
import { NumericInput, Switch } from "@wrappers/index";
import { QueryEditor } from "../../../QueryEditor/QueryEditor";
import InputGroup from "@wrappers/blueprint/input-group";
import { Autocomplete } from "../../../Autocomplete/Autocomplete";

interface IProps {
    type: string;
    inputAttributes: {
        id?: string;
        name: string;
        value?: string;
        intent?: any;
        onChange(e: any): void;
    }

    extraInfo?: {
        autoCompletion?: IPropertyAutocomplete;
        artefactId: string;
        parameterId: string;
        projectId: string | null;
    }
}

export function InputMapper(props: IProps) {
    const {type, inputAttributes, extraInfo } = props;

    if (extraInfo) {

        if (extraInfo.autoCompletion) {
            // @ts-ignore
            return <Autocomplete {...extraInfo} {...inputAttributes} />
        }

    }


    switch (type) {
        case INPUT_VALID_TYPES.BOOLEAN:
            return <Switch {...inputAttributes}/>;
        case INPUT_VALID_TYPES.INTEGER:
            return <NumericInput {...inputAttributes} buttonPosition={'none'}/>;
        case INPUT_VALID_TYPES.MULTILINE_STRING:
            return <QueryEditor {...inputAttributes} />;
        case INPUT_VALID_TYPES.STRING:
        default:
            return <InputGroup {...inputAttributes} />
    }
};
