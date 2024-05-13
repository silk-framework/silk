import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import React, { useEffect } from "react";
import {
    SuggestField,
    suggestFieldUtils,
    Highlighter,
    OverflowText,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spinner,
} from "@eccenca/gui-elements";
import { IPropertyAutocomplete } from "@ducks/common/typings";
import { sharedOp } from "@ducks/shared";
import { useTranslation } from "react-i18next";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { Intent } from "@blueprintjs/core";
import { parseErrorCauseMsg } from "../../../ApplicationNotifications/NotificationsMenu";
import { IRenderModifiers } from "@eccenca/gui-elements/src/components/AutocompleteField/interfaces";
import { CLASSPREFIX as eccguiprefix } from "@eccenca/gui-elements/src/configuration/constants";
import { RegisterForExternalChangesFn } from "./InputMapper";
import { InputGroupProps as BlueprintInputGroupProps } from "@blueprintjs/core/lib/esm/components/forms/inputGroup";
import { HTMLInputProps as BlueprintHTMLInputProps } from "@blueprintjs/core/lib/esm/common/props";
import { CreateArtefactModalContext } from "../CreateArtefactModalContext";

export interface ParameterAutoCompletionProps {
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
     * Props to spread to the underlying input field. This is BlueprintJs specific. To control this input, use
     * `onChange` instead of `inputProps.onChange`. The properties name, id, intent and readonly should not be overwritten, because they
     * are maintained by this component.
     */
    inputProps?: BlueprintInputGroupProps & BlueprintHTMLInputProps;
}

type StringOrReifiedValue = IAutocompleteDefaultResponse | string;

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
    showErrorsInline = false,
    readOnly,
    hasBackDrop = false,
    registerForExternalChanges,
    inputProps,
}: ParameterAutoCompletionProps) => {
    const [t] = useTranslation();
    const { registerError: globalErrorHandler } = useErrorHandler();
    const modalContext = React.useContext(CreateArtefactModalContext);
    const [externalValue, setExternalValue] = React.useState<{ value: string; label?: string } | undefined>(undefined);
    const initialOrExternalValue = externalValue ? externalValue : initialValue;
    const [highlightInput, setHighlightInput] = React.useState(false);
    const [show, setShow] = React.useState(true);
    const [limit, setLimit] = React.useState<number>(100);
    const [searchQuery, setSearchQuery] = React.useState<string>("");
    const [fetchMore, setFetchMore] = React.useState<boolean>(true);
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
            const handleUpdates = (externalValue: { value: string; label?: string }) => {
                setExternalValue(externalValue);
                setHighlightInput(true);
                onChange(externalValue);
            };
            registerForExternalChanges(formParamId, handleUpdates);
        }
    }, [registerForExternalChanges]);

    // Re-init element when value is set from outside
    useEffect(() => {
        if (externalValue) {
            setShow(false);
            setTimeout(() => setShow(true), 0);
        }
    }, [externalValue]);

    const selectDependentValues = (autoCompletion: IPropertyAutocomplete): string[] => {
        return autoCompletion.autoCompletionDependsOnParameters.flatMap((paramId) => {
            const value = dependentValue(paramId);
            if (dependentValueIsSet(value)) {
                return [`${value}`];
            } else {
                return [];
            }
        });
    };

    const errorTitle = t("ParameterWidget.AutoComplete.fetchErrorTitle");

    const handleAutoCompleteInput = async (
        input: string,
        autoCompletion: IPropertyAutocomplete,
        limit: number
    ): Promise<IAutocompleteDefaultResponse[]> => {
        // The auto-completion is only showing the first 100 values FIXME: Make auto-completion list scrollable?
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

    if (!show) {
        return <Spinner position={"inline"} />;
    }

    const handleSearch = React.useCallback(
        (input: string) => {
            setSearchQuery(input);
            setFetchMore(true)
            setLimit(100)
            return handleAutoCompleteInput(input, autoCompletion, 100);
        },
        [limit]
    );

    const loadMoreResults = async () => {
        const newLimit = limit + 100;
        const results = (await handleAutoCompleteInput(searchQuery, autoCompletion, newLimit)) ?? [];
        setLimit(newLimit);

        //make one more requests if the response is less than the limit
        if (results.length < newLimit) {
            //hide loadMore
            setFetchMore(false);
        } else {
            //fetch one more ahead to see if there is still more.
            const nextLimit = newLimit + 1;
            const nextResults = (await handleAutoCompleteInput(searchQuery, autoCompletion, nextLimit)) ?? [];
            if (nextResults.length < nextLimit) {
                setFetchMore(false);
            }
        }

        return results.slice(limit + 1);
    };

    return (
        <SuggestField<StringOrReifiedValue, IAutocompleteDefaultResponse>
            onSearch={handleSearch}
            onChange={onChangeUsed}
            initialValue={initialOrExternalValue}
            disabled={
                selectDependentValues(autoCompletion).length < autoCompletion.autoCompletionDependsOnParameters.length
            }
            inputProps={{
                name: formParamId,
                id: formParamId,
                intent: highlightInput ? "success" : intent,
                readOnly: !!readOnly,
                ...inputProps,
            }}
            loadMoreResults={fetchMore ? loadMoreResults : undefined}
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
                              "item-add-artefact"
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
    modifiers: IRenderModifiers,
    handleSelectClick: () => any
): JSX.Element | string => {
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
export const dependentValueIsSet = (value: any): boolean => value != null && `${value}` !== "";
