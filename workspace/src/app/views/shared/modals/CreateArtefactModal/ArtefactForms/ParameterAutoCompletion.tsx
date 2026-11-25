import { DependsOnParameterValue, IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import React, { useEffect } from "react";
import {
    Highlighter,
    OverflowText,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spinner,
    SuggestField,
    SuggestFieldProps,
    SuggestFieldItemRendererModifierProps,
    suggestFieldUtils,
} from "@eccenca/gui-elements";
import { IPropertyAutocomplete } from "@ducks/common/typings";
import { sharedOp } from "@ducks/shared";
import { useTranslation } from "react-i18next";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { IntentBlueprint as Intent } from "@eccenca/gui-elements/src/common/Intent";
import { parseErrorCauseMsg } from "../../../ApplicationNotifications/NotificationsMenu";
import { CLASSPREFIX as eccguiprefix } from "@eccenca/gui-elements/src/configuration/constants";
import { RegisterForExternalChangesFn } from "./InputMapper";
import { CreateArtefactModalContext } from "../CreateArtefactModalContext";
import { CodeAutocompleteFieldPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";

export interface ParameterAutoCompletionProps {
    /** ID of the parameter. */
    paramId: string;
    /** Unique ID/name of the parameter in the form. */
    formParamId: string;
    projectId: string;
    /** ID of the task the parameter belongs to, if the task exists already. */
    taskId?: string;
    /** ID of this plugin. */
    pluginId: string;
    /** The auto-completion config. */
    autoCompletion: IPropertyAutocomplete;
    /** The initial value */
    initialValue?: IAutocompleteDefaultResponse;
    /** Get parameter values this auto-completion might depend on. */
    dependentValue: (paramId: string) => DependsOnParameterValueAny | undefined;
    /** The default value as defined in the parameter spec. */
    defaultValue: (paramId: string) => string | null | undefined;
    /** If a value is required. If true, a reset won't be possible. */
    required: boolean;
    onChange: (value: IAutocompleteDefaultResponse) => any;
    intent?: Intent;
    /** Show errors in the auto-completion list instead of the global error notification widget. */
    showErrorsInline?: boolean;
    /** When set to true the auto-complete input field will be in read-only mode and cannot be edited. */
    readOnly?: boolean;
    /** Creates a backdrop when the popover is shown that captures outside clicks in order to close the popover.
     * This is needed if other components on the same page are swallowing events, e.g. the react-flow canvas.
     * hasBackDrop should then be set to true in these cases otherwise the popover won't close when clicking those other components.
     **/
    hasBackDrop?: boolean;
    /** Register for getting external updates for values. */
    registerForExternalChanges?: RegisterForExternalChangesFn;
    /**
     * Props to spread to the underlying input field. This is BlueprintJs specific.
     */
    inputProps?: Omit<
        SuggestFieldProps<StringOrReifiedValue, IAutocompleteDefaultResponse>["inputProps"],
        "onChange" | "name" | "id" | "intent" | "readonly"
    >;
    /**
     * Fetches partial auto-completion results for the transforms task input paths, i.e. any part of a path could be auto-completed
     * without replacing the complete path.
     */
    partialAutoCompletion?: (
        inputType: "source" | "target",
    ) => (
        inputString: string,
        cursorPosition: number,
    ) => Promise<CodeAutocompleteFieldPartialAutoCompleteResult | undefined>;
}

type StringOrReifiedValue = IAutocompleteDefaultResponse | string;

const AUTOCOMPLETION_LIMIT = 100;

/** Component for parameter auto-completion. */
export const ParameterAutoCompletion = ({
    paramId,
    formParamId,
    projectId,
    taskId,
    intent,
    pluginId,
    autoCompletion,
    initialValue,
    dependentValue,
    defaultValue,
    required,
    onChange,
    showErrorsInline = false,
    readOnly,
    hasBackDrop = false,
    registerForExternalChanges,
    inputProps,
}: ParameterAutoCompletionProps) => {
    const [t] = useTranslation();
    const { registerError: globalErrorHandler } = useErrorHandler();
    const modalContext = React.useContext(CreateArtefactModalContext);
    const [externalValue, setExternalValue] = React.useState<
        { value: string; label?: string; version: number } | undefined
    >(undefined);
    const initialOrExternalValue = externalValue ? externalValue : initialValue;
    const [highlightInput, setHighlightInput] = React.useState(false);
    const pagination = React.useRef<{ query: string; limit: number; hasMore: boolean }>();
    const registerError = modalContext.registerModalError ? modalContext.registerModalError : globalErrorHandler;

    let onChangeUsed = onChange;
    if (highlightInput) {
        onChangeUsed = (value: any) => {
            onChange(value);
            setHighlightInput(false);
        };
    }

    useEffect(() => {
        if (registerForExternalChanges) {
            let version = 0;
            const handleUpdates = (externalValue: { value: string; label?: string }) => {
                version += 1;
                setExternalValue({ ...externalValue, version });
                setHighlightInput(true);
                onChange(externalValue);
            };
            registerForExternalChanges(formParamId, handleUpdates);
        }
    }, [registerForExternalChanges]);

    const selectDependentValues = (autoCompletion: IPropertyAutocomplete): DependsOnParameterValue[] => {
        const prefixIdx = formParamId.lastIndexOf(".");
        const parameterPrefix = prefixIdx >= 0 ? formParamId.substring(0, prefixIdx + 1) : "";
        return autoCompletion.autoCompletionDependsOnParameters.flatMap((paramId) => {
            const value = dependentValue(paramId);
            if (dependentValueIsSet(value?.value, defaultValue(parameterPrefix + paramId) != null)) {
                return [{ value: `${value!.value}`, isTemplate: value!.isTemplate }];
            } else {
                return [];
            }
        });
    };

    const errorTitle = t("ParameterWidget.AutoComplete.fetchErrorTitle");

    const handleAutoCompleteInput = async (
        input: string,
        isCurrent: () => boolean,
        limit = AUTOCOMPLETION_LIMIT,
    ): Promise<IAutocompleteDefaultResponse[]> => {
        try {
            if (autoCompletion.customAutoCompletionRequest) {
                return await autoCompletion.customAutoCompletionRequest(input, limit);
            } else {
                const autoCompleteResponse = await sharedOp.getAutocompleteResultsAsync({
                    pluginId: pluginId,
                    parameterId: paramId,
                    projectId,
                    taskId,
                    dependsOnParameterValues: selectDependentValues(autoCompletion),
                    textQuery: input,
                    limit: limit,
                });
                return autoCompleteResponse.data;
            }
        } catch (e) {
            if (!isCurrent()) return [];
            // For now hide 400 errors from user, since they are not helpful.
            if (!e.isHttpError || (e.isHttpError && e.httpStatus !== 400)) {
                if (showErrorsInline) {
                    // This should be handled in the auto-completion component
                    const details = parseErrorCauseMsg(e) ?? "";
                    throw new Error(details);
                } else {
                    registerError("ParameterAutoCompletion.handleAutoCompleteInput", errorTitle, e);
                }
            } else {
                console.warn(e);
            }
            return [];
        }
    };

    const itemValue = (value: StringOrReifiedValue) => (typeof value === "string" ? value : value.value);

    const handleSearch = async (input: string, isCurrent: () => boolean) => {
        const results = await handleAutoCompleteInput(input, isCurrent);
        if (isCurrent()) {
            pagination.current = {
                query: input,
                limit: AUTOCOMPLETION_LIMIT,
                hasMore: results.length >= AUTOCOMPLETION_LIMIT,
            };
        }
        return results;
    };

    const loadMoreResults = async (isCurrent: () => boolean) => {
        const page = pagination.current;
        if (page?.hasMore) {
            const { query, limit } = page;
            const newLimit = limit + AUTOCOMPLETION_LIMIT;
            const results = await handleAutoCompleteInput(query, isCurrent, newLimit);
            if (!isCurrent()) return;
            pagination.current = { query, limit: newLimit, hasMore: results.length >= newLimit };
            return results.slice(limit);
        }
    };

    const dependencies = selectDependentValues(autoCompletion);

    return (
        <SuggestField<StringOrReifiedValue, IAutocompleteDefaultResponse>
            key={externalValue?.version != null ? `${formParamId}_${externalValue.version}` : undefined}
            onSearch={handleSearch}
            searchContextKey={JSON.stringify([projectId, pluginId, paramId, dependencies])}
            onChange={onChangeUsed}
            initialValue={initialOrExternalValue}
            disabled={dependencies.length < autoCompletion.autoCompletionDependsOnParameters.length}
            inputProps={{
                name: formParamId,
                id: formParamId,
                intent: highlightInput ? "success" : intent,
                readOnly: !!readOnly,
                ...inputProps,
            }}
            loadMoreResults={loadMoreResults}
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
                          itemRenderer: suggestFieldUtils.createNewItemRendererFactory(
                              (query) => t("ParameterWidget.AutoComplete.createNewItem", { query }),
                              "item-add-artefact",
                          ),
                      }
            }
            noResultText={t("common.messages.noResults")}
            requestErrorPrefix={errorTitle}
            hasBackDrop={hasBackDrop}
        />
    );
};

// Label of auto-completion results
const autoCompleteLabel = (item: StringOrReifiedValue) => {
    const label = typeof item === "string" ? item : item.label || item.value;
    return label;
};

const displayAutoCompleteLabel = (item: StringOrReifiedValue) => {
    const label = autoCompleteLabel(item);
    if (label === "") {
        return "\u00A0";
    } else {
        return label;
    }
};

/** An item renderer for the auto-completion component that will render the label (if available) and value.
 * If the label and value are the same except for case then only the value is displayed. */
export const labelAndOrValueItemRenderer = (
    autoCompleteResponse: IAutocompleteDefaultResponse,
    query: string,
    modifiers: SuggestFieldItemRendererModifierProps,
    handleSelectClick: () => any,
): React.JSX.Element | string => {
    const labelValueKindOfSame =
        (autoCompleteResponse.label ?? "").toLowerCase() === autoCompleteResponse.value.toLowerCase();
    const showLabel = autoCompleteResponse.label && !labelValueKindOfSame;
    return (
        <OverviewItem
            key={autoCompleteResponse.value}
            onClick={handleSelectClick}
            hasSpacing={true}
            className={modifiers.active ? `${eccguiprefix}-overviewitem__item--active` : ""}
        >
            <OverviewItemDescription style={{ maxWidth: "50vw" }}>
                <OverviewItemLine>
                    <OverflowText inline={true} style={{ width: "100vw" }}>
                        <Highlighter
                            label={showLabel ? autoCompleteResponse.label : autoCompleteResponse.value}
                            searchValue={query}
                        />
                    </OverflowText>
                </OverviewItemLine>
                {showLabel ? (
                    <OverviewItemLine small={true}>
                        <OverflowText inline={true} style={{ width: "100vw" }}>
                            <Highlighter label={autoCompleteResponse.value} searchValue={query} />
                        </OverflowText>
                    </OverviewItemLine>
                ) : null}
            </OverviewItemDescription>
        </OverviewItem>
    );
};

/** At the moment a dependent value must be non-empty, else it is not considered to be set. */
export const dependentValueIsSet = (value: any, hasDefaultValue: boolean): boolean =>
    value != null && (value !== "" || hasDefaultValue); //TODO CMEM-5379 && `${value}` !== "";

export interface DependsOnParameterValueAny {
    value: any;
    isTemplate: boolean;
}
