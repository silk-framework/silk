import React from "react";
import { autocompleteAsync } from "../store";
import {
    SuggestField,
    suggestFieldUtils,
    FieldItem,
    Highlighter,
    highlighterUtils,
    Icon,
    MenuItem,
    OverflowText,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    SuggestFieldItemRendererModifierProps,
    OverviewItemDepiction,
    OverviewItemActions,
} from "@eccenca/gui-elements";
import { TaskContext } from "../../../../shared/projectTaskTabView/projectTaskTabView.typing";
import { ValidIconName } from "@eccenca/gui-elements/src/components/Icon/canonicalIconNames";

// Creates a search function for the auto-complete field
const onSearchFactory = (
    ruleId?: string,
    entity?: string,
    taskContext?: TaskContext
): ((searchText: string) => Promise<any[]>) => {
    return (searchText: string) => {
        return new Promise((resolve, reject) => {
            autocompleteAsync({
                entity,
                input: searchText,
                ruleId,
                taskContext,
            }).subscribe(
                ({ options }) => {
                    resolve(options);
                },
                (err) => reject(err)
            );
        });
    };
};

// Creates a search function from a list of options
const onSearchOptionFactory = (options: string[]) => {
    return (query: string) => {
        const searchWords = highlighterUtils.extractSearchWords(query, true);
        return options
            .filter((o) => highlighterUtils.matchesAllWords(o.toLowerCase(), searchWords))
            .map((o) => ({ value: o }));
    };
};

// Parameters for the isValidNewOption function
export interface IIsValidNewOptionParams {
    label: string;
}

// Parameters for a new options creator function
export interface INewOptionCreatorParams {
    label: string;
    labelKey: string;
    valueKey: string;
}

interface IProps {
    // The property of the rule that is changed, e.g. source path, target property etc.
    entity: string;
    // The ID of the rule
    ruleId?: string;
    // Fixed array of options
    options?: string[];
    // Class name of the container element
    className: string;
    // The label and input field placeholder string of the auto-complete field
    placeholder: string;
    // If items can be created
    creatable?: boolean;
    // The current selected value
    value?: string | IAutoCompleteItem;
    // The function that is called when a new value gets selected
    onChange: (value: string) => any;
    // Creates a new option
    newOptionCreator?: (params: INewOptionCreatorParams) => any;
    // New option label creator from the input query
    newOptionText?: (query: string) => string;
    // Checks if an input is a valid new options
    isValidNewOption?: (input: IIsValidNewOptionParams) => any;
    // If the selected value can be cleared
    clearable?: boolean;
    // If true the query will be reset to the value and not the optional label
    resetQueryToValue?: boolean;
    // The string that should be displayed in the input field of the selected item. By default it is the optional label or value.
    itemDisplayLabel?: (item: IAutoCompleteItem) => string;
    // The text that is displayed when no item was found and no item can be created
    noResultsText?: string;
    // When a label exist, also show the value in the 2. row below the label of the suggested item. Default: true
    showValueWhenLabelExists?: boolean;
    /** Context of the transform task. */
    taskContext?: TaskContext;
}

// Auto-complete interface as it is returned by the auto-complete backend APIs
interface IAutoCompleteItem {
    value: string;
    label?: string;
    description?: string | null;
}

// Checks if a label exists that is distinct enough from the value
const hasDistinctLabel = (autoCompleteItem: IAutoCompleteItem) =>
    autoCompleteItem.label && autoCompleteItem.label.toLowerCase() !== autoCompleteItem.value.toLowerCase();

interface OptionalRenderFunctions<T> {
    optionalLabelFn?: (obj: T) => string | undefined;
    optionalValueFn?: (obj: T) => string;
    optionalDescriptionFn?: (obj: T) => string | undefined;
    optionalIconFn?: (obj: T) => ValidIconName | JSX.Element | undefined;
    optionalItemActionsFn?: (obj: T) => JSX.Element | undefined;
}

export function autoCompleteItemRendererFactory<T = {}>(
    showValueWhenLabelExists: boolean,
    optionalFunctions: OptionalRenderFunctions<T> = {}
) {
    return (
        autoCompleteItem: IAutoCompleteItem & T,
        query: string,
        modifiers: SuggestFieldItemRendererModifierProps,
        handleClick: () => any
    ): JSX.Element => {
        const { optionalLabelFn, optionalValueFn, optionalDescriptionFn, optionalIconFn, optionalItemActionsFn } =
            optionalFunctions;
        let label: string | undefined;
        let value: string | undefined = undefined;
        // Do not display value and label if they are basically the same
        if (hasDistinctLabel(autoCompleteItem)) {
            label = autoCompleteItem.label;
            value = autoCompleteItem.value;
        } else {
            label = autoCompleteItem.value;
        }
        if (optionalLabelFn) {
            label = optionalLabelFn(autoCompleteItem);
        }
        if (optionalValueFn && label !== optionalValueFn(autoCompleteItem)) {
            value = optionalValueFn(autoCompleteItem);
        }
        const highlighter = (value: string | undefined) =>
            modifiers.highlightingEnabled ? <Highlighter label={value} searchValue={query} /> : value;
        const description: string | undefined | null = optionalDescriptionFn
            ? optionalDescriptionFn(autoCompleteItem)
            : autoCompleteItem.description;
        const icon = optionalIconFn?.(autoCompleteItem);
        const itemActions = optionalItemActionsFn?.(autoCompleteItem);
        const item =
            value || description ? (
                <OverviewItem style={modifiers.styleWidth}>
                    {icon ? (
                        <OverviewItemDepiction>
                            {typeof icon === "string" ? <Icon name={icon} /> : icon}
                        </OverviewItemDepiction>
                    ) : null}
                    <OverviewItemDescription>
                        <OverviewItemLine>
                            <OverflowText ellipsis={"reverse"}>{highlighter(label)}</OverflowText>
                        </OverviewItemLine>
                        {value && showValueWhenLabelExists && (
                            <OverviewItemLine small={true}>
                                <OverflowText ellipsis={"reverse"}>{highlighter(value)}</OverflowText>
                            </OverviewItemLine>
                        )}
                        {description && (
                            <OverviewItemLine small={true}>
                                <OverflowText>{highlighter(description)}</OverflowText>
                            </OverviewItemLine>
                        )}
                    </OverviewItemDescription>
                    {itemActions ? <OverviewItemActions>{itemActions}</OverviewItemActions> : null}
                </OverviewItem>
            ) : (
                <OverflowText style={modifiers.styleWidth} ellipsis={"reverse"}>
                    {highlighter(label)}
                </OverflowText>
            );
        return (
            <MenuItem
                active={modifiers.active}
                disabled={modifiers.disabled}
                key={value ?? label}
                onClick={handleClick}
                text={item}
            />
        );
    };
}

// Renders an option item in the suggest list
export const autoCompleteItemRenderer = autoCompleteItemRendererFactory(true);
// Does not render the value when a label exists
export const autoCompleteItemRendererWithoutValue = autoCompleteItemRendererFactory(false);

// Necessary because else popovers won't be shown (still unclear why, maybe interaction with MDL)
const portalContainer = () => document.body;
// Creates a new, custom option element
const queryToNewOption: (label: string) => INewOptionCreatorParams = (label: string) => ({
    label: label,
    labelKey: "label",
    valueKey: "value",
});
const itemLabel = (itemDisplayLabel?: (item: IAutoCompleteItem) => string) => (autoCompleteItem: IAutoCompleteItem) => {
    if (hasDistinctLabel(autoCompleteItem)) {
        return itemDisplayLabel ? itemDisplayLabel(autoCompleteItem) : (autoCompleteItem.label as string);
    } else {
        return autoCompleteItem.value;
    }
};

/** Input field supporting auto-complete support. */
const AutoComplete = ({
    entity,
    ruleId,
    className,
    placeholder,
    creatable,
    onChange,
    value,
    clearable = true,
    resetQueryToValue = false,
    itemDisplayLabel,
    newOptionText,
    options,
    noResultsText,
    showValueWhenLabelExists = true,
    taskContext,
    ...otherProps
}: IProps) => {
    const reset = clearable
        ? {
              resetValue: "",
              resetButtonText: "Clear value",
              resettableValue: (v: IAutoCompleteItem) => !!v?.value,
          }
        : undefined;
    const isValidNewOption = otherProps.isValidNewOption ? otherProps.isValidNewOption : () => true;
    const newOptionCreator = otherProps.newOptionCreator
        ? otherProps.newOptionCreator
        : ({ label }) => ({ value: label });
    const newItemRenderer = (
        query: string,
        modifiers: SuggestFieldItemRendererModifierProps,
        handleClick: React.MouseEventHandler<HTMLElement>
    ) => {
        if (isValidNewOption({ label: query })) {
            const newItemRenderer = suggestFieldUtils.createNewItemRendererFactory(
                (query: string) => (newOptionText ? newOptionText(query) : `Create option '${query}'`),
                "item-add-artefact"
            );
            return newItemRenderer(query, modifiers, handleClick);
        }
    };
    const create = creatable
        ? {
              itemFromQuery: (query: string) => newOptionCreator(queryToNewOption(query)),
              itemRenderer: newItemRenderer,
              showNewItemOptionFirst: true,
          }
        : undefined;
    return (
        <div className={className}>
            <Spacing size={"tiny"} />
            <FieldItem
                labelProps={{
                    text: placeholder,
                }}
            >
                <SuggestField<IAutoCompleteItem, string>
                    inputProps={{
                        placeholder: placeholder,
                    }}
                    contextOverlayProps={{
                        portalContainer: portalContainer(),
                    }}
                    reset={reset}
                    onChange={onChange}
                    initialValue={typeof value === "string" ? { value } : value}
                    itemValueSelector={(item) => item.value}
                    itemValueString={(item) => item.value}
                    onSearch={options ? onSearchOptionFactory(options) : onSearchFactory(ruleId, entity, taskContext)}
                    itemRenderer={
                        showValueWhenLabelExists ? autoCompleteItemRenderer : autoCompleteItemRendererWithoutValue
                    }
                    itemValueRenderer={itemLabel(itemDisplayLabel)}
                    noResultText={noResultsText ? noResultsText : "No result."}
                    createNewItem={create}
                    resetQueryToValue={resetQueryToValue ? (item) => item.value : undefined}
                />
            </FieldItem>
            <Spacing size={"tiny"} />
        </div>
    );
};

export default AutoComplete;
