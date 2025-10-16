import React from "react";

import { CodeAutocompleteField } from "@eccenca/gui-elements";
import {
    CodeAutocompleteFieldPartialAutoCompleteResult,
    CodeAutocompleteFieldReplacementResult,
    CodeAutocompleteFieldValidationResult,
} from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import { sharedOp } from "@ducks/shared";
import { IPropertyAutocomplete } from "@ducks/common/typings";
import {
    dependentValueIsSet,
    DependsOnParameterValueAny,
} from "./modals/CreateArtefactModal/ArtefactForms/ParameterAutoCompletion";
import { DependsOnParameterValue } from "@ducks/shared/typings";
import { validateYaml } from "@ducks/workspace/requests";
import useErrorHandler from "../../hooks/useErrorHandler";

interface YamlEditorProps {
    /** ID of the project. */
    projectId?: string;
    /** ID of this plugin. */
    pluginId?: string;
    /** Unique ID/name of the parameter in the form. */
    formParamId?: string;
    /** Get parameter values this auto-completion might depend on. */
    dependentValue?: (paramId: string) => DependsOnParameterValueAny | undefined;
    /** The default value as defined in the parameter spec. */
    defaultValue?: (paramId: string) => string | null | undefined;
    id: string;
    initialValue?: string;
    /** Callback on value change
     */
    onChange: (currentValue: string) => any;
    /** The auto-completion config. */
    autoCompletion?: IPropertyAutocomplete;
}

export const YamlEditor: React.FC<YamlEditorProps> = ({
    projectId,
    pluginId,
    formParamId,
    dependentValue,
    initialValue,
    defaultValue,
    autoCompletion,
    id,
    onChange,
}) => {
    const { registerError } = useErrorHandler();

    const selectDependentValues = (autoCompletion: IPropertyAutocomplete): DependsOnParameterValue[] => {
        if (!formParamId || !defaultValue || !dependentValue) return [];
        const prefixIdx = formParamId.lastIndexOf(".");
        const parameterPrefix = prefixIdx >= 0 ? formParamId.substring(0, prefixIdx + 1) : "";
        return autoCompletion.autoCompletionDependsOnParameters.flatMap((paramId) => {
            const value = dependentValue!(paramId);
            if (dependentValueIsSet(value?.value, defaultValue!(parameterPrefix + paramId) != null)) {
                return [{ value: `${value!.value}`, isTemplate: value!.isTemplate }];
            } else {
                return [];
            }
        });
    };

    const fetchSuggestions = React.useCallback(
        async (
            inputString: string,
            cursorPosition: number,
        ): Promise<CodeAutocompleteFieldPartialAutoCompleteResult | undefined> => {
            if (inputString.startsWith(" ") || inputString.startsWith("\t")) {
                return undefined;
            }
            const firstColon = inputString.indexOf(":");
            const cursorBeforeColon = firstColon < 0 || cursorPosition <= firstColon;
            if (cursorBeforeColon && !!projectId && !!pluginId && !!autoCompletion) {
                const searchText = firstColon < 0 ? inputString : inputString.substring(0, firstColon);
                const result = await sharedOp.getAutocompleteResultsAsync({
                    pluginId: pluginId,
                    parameterId: id,
                    projectId: projectId,
                    dependsOnParameterValues: selectDependentValues(autoCompletion),
                    textQuery: searchText,
                    limit: 5,
                });
                if (result.data.length) {
                    const autoCompletionResults: CodeAutocompleteFieldReplacementResult = {
                        extractedQuery: searchText,
                        replacementInterval: {
                            from: 0,
                            length: searchText.length,
                        },
                        replacements: result.data,
                    };
                    return {
                        inputString,
                        cursorPosition,
                        replacementResults: [autoCompletionResults],
                    };
                } else {
                    return undefined;
                }
            } else {
                return undefined;
            }
        },
        [pluginId, id, projectId, autoCompletion?.autoCompletionDependsOnParameters.join("|")],
    );

    const checkYamlString = React.useCallback(
        async (yaml: string): Promise<CodeAutocompleteFieldValidationResult | undefined> => {
            try {
                const result = await validateYaml(yaml, false, true);
                return result.data;
            } catch (ex) {
                registerError("checkYamlString", "YAML validation request has failed.", ex);
            }
        },
        [],
    );

    return (
        <CodeAutocompleteField
            mode={"yaml"}
            initialValue={initialValue ?? ""}
            multiline={true}
            onChange={onChange}
            fetchSuggestions={fetchSuggestions}
            autoCompletionRequestDelay={200}
            checkInput={checkYamlString}
        />
    );
};
