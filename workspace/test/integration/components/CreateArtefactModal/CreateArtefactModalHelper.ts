import { IArtefactItemProperty, IPropertyAutocomplete } from "../../../../src/app/store/ducks/common/typings";
import { INPUT_TYPES } from "../../../../src/app/constants";
import { IRequestAutocompletePayload } from "../../../../src/app/store/ducks/shared/typings";
import { mockAxiosResponse } from "../../TestHelper";
import { workspaceApi } from "../../../../src/app/utils/getApiEndpoint";
import { HttpResponse } from "jest-mock-axios/dist/lib/mock-axios-types";
import { AxiosError } from "axios";

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
        value: "",
        visibleInDialog: true,
        autoCompletion: autoCompletion ? autocompleteDescription(autoCompletion) : undefined,
        ...props,
    };
};

export const autocompleteDescription = (
    autoCompletion: Partial<IPropertyAutocomplete>
): IPropertyAutocomplete | undefined => {
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

export const mockAutoCompleteResponse = (
    requestPayload: Partial<IRequestAutocompletePayload>,
    response?: HttpResponse | AxiosError
): void => {
    mockAxiosResponse(
        {
            url: workspaceApi("pluginParameterAutoCompletion"),
            method: "POST",
            partialPayload: requestPayload,
        },
        response
    );
};
