import { IArtefactItemProperty, IPropertyAutocomplete } from "../../../../src/app/store/ducks/common/typings";
import { INPUT_TYPES } from "../../../../src/app/constants";

export const atomicParamDescription = (
    props: Partial<IArtefactItemProperty>,
    autoCompletion?: Partial<IPropertyAutocomplete>
): IArtefactItemProperty => {
    return {
        advanced: false,
        description: "No description specified",
        parameterType: INPUT_TYPES.STRING,
        title: "No title specified",
        type: "string",
        value: null,
        visibleInDialog: true,
        autoCompletion: autocompleteDescription(autoCompletion),
        ...props,
    };
};

export const autocompleteDescription = (autoCompletion: Partial<IPropertyAutocomplete>): IPropertyAutocomplete => {
    if (autoCompletion) {
        return {
            allowOnlyAutoCompletedValues: true,
            autoCompleteValueWithLabels: false,
            autoCompletionDependsOnParameters: [],
            ...autoCompletion,
        };
    } else {
        return undefined;
    }
};

export const objectParamDescription = (
    pluginId: string,
    properties: Record<string, IArtefactItemProperty>,
    required: string[],
    props: Partial<IArtefactItemProperty>
): IArtefactItemProperty => {
    const initialObject = atomicParamDescription(props);
    return {
        ...initialObject,
        type: "object",
        pluginId,
        properties,
        required,
    };
};
