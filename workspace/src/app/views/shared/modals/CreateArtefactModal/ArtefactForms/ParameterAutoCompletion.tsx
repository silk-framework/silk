import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import React from "react";
import { AutoCompleteField } from "gui-elements";
import { IPropertyAutocomplete } from "@ducks/common/typings";
import { sharedOp } from "@ducks/shared";
import { useTranslation } from "react-i18next";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { createNewItemRendererFactory } from "gui-elements/src/components/AutocompleteField/autoCompleteFieldUtils";
import { Intent } from "@blueprintjs/core";

interface ParameterAutoCompletionProps {
    /** ID of the parameter. */
    paramId: string;
    /** Unique ID/name of the parameter in the form. */
    formParamId: string;
    projectId: string;
    /** ID of this plugin. */
    pluginId: string;
    /** The auto-completion config. */
    autoCompletion: IPropertyAutocomplete;
    /** The initial value */
    initialValue?: IAutocompleteDefaultResponse;
    /** Get parameter values this auto-completion might depend on. */
    dependentValue: (paramId: string) => string | undefined;
    /** If a value is required. If true, a reset won't be possible. */
    required: boolean;
    onChange: (value: IAutocompleteDefaultResponse) => any;
    intent: Intent;
}

/** Component for parameter auto-completion. */
export const ParameterAutoCompletion = ({
    paramId,
    formParamId,
    projectId,
    intent,
    pluginId,
    autoCompletion,
    initialValue,
    dependentValue,
    required,
    onChange,
}: ParameterAutoCompletionProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const selectDependentValues = (autoCompletion: IPropertyAutocomplete): string[] => {
        return autoCompletion.autoCompletionDependsOnParameters.flatMap((paramId) => {
            const value = dependentValue(paramId);
            if (value) {
                return [value];
            } else {
                return [];
            }
        });
    };

    const handleAutoCompleteInput = async (input: string, autoCompletion: IPropertyAutocomplete) => {
        // The auto-completion is only showing the first 100 values TODO: Make auto-completion list scrollable?
        const limit = 100;
        try {
            if (autoCompletion.customAutoCompletionRequest) {
                return autoCompletion.customAutoCompletionRequest(input, limit);
            } else {
                const autoCompleteResponse = await sharedOp.getAutocompleteResultsAsync({
                    pluginId: pluginId,
                    parameterId: paramId,
                    projectId,
                    dependsOnParameterValues: selectDependentValues(autoCompletion),
                    textQuery: input,
                    limit: limit,
                });
                return autoCompleteResponse.data;
            }
        } catch (e) {
            if (e.isHttpError && e.httpStatus !== 400) {
                // For now hide 400 errors from user, since they are not helpful.
                registerError(
                    "ParameterAutoCompletion.handleAutoCompleteInput",
                    "Could not fetch auto-completion suggestions.",
                    e
                );
            } else {
                console.warn(e);
            }
            return [];
        }
    };

    const itemValue = (value: IAutocompleteDefaultResponse | string) =>
        typeof value === "string" ? value : value.value;

    return (
        <AutoCompleteField<IAutocompleteDefaultResponse | string, IAutocompleteDefaultResponse>
            onSearch={(input: string) => handleAutoCompleteInput(input, autoCompletion)}
            onChange={onChange}
            initialValue={initialValue}
            disabled={
                selectDependentValues(autoCompletion).length < autoCompletion.autoCompletionDependsOnParameters.length
            }
            inputProps={{
                name: formParamId,
                id: formParamId,
                intent: intent,
            }}
            reset={
                !required
                    ? {
                          resetValue: { value: "" },
                          resettableValue: (v) => !!itemValue(v),
                          resetButtonText: t("common.action.resetSelection", "Reset selection"),
                      }
                    : undefined
            }
            itemRenderer={autoCompletion.customItemRenderer ?? displayAutoCompleteLabel}
            itemValueRenderer={autoCompleteLabel}
            itemValueSelector={(item) => (typeof item === "string" ? { value: item } : item)}
            itemValueString={(item) => itemValue(item)}
            createNewItem={
                autoCompletion.allowOnlyAutoCompletedValues
                    ? undefined
                    : {
                          itemFromQuery: (query) => ({ value: query }),
                          itemRenderer: createNewItemRendererFactory(
                              (query) => t("ParameterWidget.AutoComplete.createNewItem", { query }),
                              "item-add-artefact"
                          ),
                      }
            }
            noResultText={t("common.messages.noResults")}
        />
    );
};

// Label of auto-completion results
const autoCompleteLabel = (item: IAutocompleteDefaultResponse) => {
    const label = item.label || item.value;
    return label;
};

const displayAutoCompleteLabel = (item: IAutocompleteDefaultResponse) => {
    const label = autoCompleteLabel(item);
    if (label === "") {
        return "\u00A0";
    } else {
        return label;
    }
};
