import React from "react";
import { IArtefactItemProperty } from "@ducks/global/typings";
import { INPUT_VALID_TYPES } from "../../../../../constants";
import { NumericInput, Switch } from "@wrappers/index";
import { QueryEditor } from "../../../QueryEditor/QueryEditor";
import InputGroup from "@wrappers/blueprint/input-group";
import { Autocomplete } from "../../../Autocomplete/Autocomplete";

interface IProps {
    artefactId: string;
    parameterId: string;
    projectId?: string | null;
    property: IArtefactItemProperty;

    inputAttributes: {
        id?: string;
        name: string;
        value?: string;
        intent?: any;
        onChange(e: any): void;
    }
}

export function InputMapper(props: IProps) {

    const {property, inputAttributes, artefactId, parameterId, projectId} = props;

    if (property.autoCompletion) {
        return <Autocomplete
            options={property.autoCompletion}
            pluginId={artefactId}
            parameterId={parameterId}
            projectId={projectId}
            {...inputAttributes}
        />
    }


    switch (property.type) {
        case INPUT_VALID_TYPES.BOOLEAN:
            return <Switch {...props.inputAttributes}/>;
        case INPUT_VALID_TYPES.INTEGER:
            return <NumericInput {...props.inputAttributes} buttonPosition={'none'}/>;
        case INPUT_VALID_TYPES.MULTILINE_STRING:
            return <QueryEditor {...props.inputAttributes} />;
        case INPUT_VALID_TYPES.STRING:
        default:
            return <InputGroup {...props.inputAttributes} />
    }
};
