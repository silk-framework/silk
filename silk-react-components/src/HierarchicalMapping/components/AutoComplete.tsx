import React from 'react';
import {autocompleteAsync} from '../store';
import {
    AutoCompleteField,
    FieldItem, Highlighter, MenuItem, OverflowText,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine, Spacing
} from "@gui-elements/index";
import {createNewItemRendererFactory} from "@gui-elements/src/components/AutocompleteField/autoCompleteFieldUtils";
import {IRenderModifiers} from "@gui-elements/src/components/AutocompleteField/AutoCompleteField";

// Creates a search function for the auto-complete field
const onSearchFactory = (ruleId: string,
                         entity: string): (searchText: string) => Promise<any[]> => {
    return (searchText: string) => {
        return new Promise((resolve, reject) => {
            autocompleteAsync({
                entity,
                input: searchText,
                ruleId,
            }).subscribe(({options}) => {
                resolve(options)
            },
                err => reject(err))
        })
    }
}

// Parameters for the isValidNewOption function
export interface IIsValidNewOptionParams {
    label: string
}

// Parameters for a new options creator function
export interface INewOptionCreatorParams {
    label: string
    labelKey: string
    valueKey: string
}

interface IProps {
    // The property of the rule that is changed, e.g. source path, target property etc.
    entity: string
    // The ID of the rule
    ruleId: string
    // Class name of the container element
    className: string
    // The label and input field placeholder string of the auto-complete field
    placeholder: string
    // If items can be created
    creatable?: boolean
    // The current selected value
    value: string | IAutoCompleteItem
    // The function that is called when a new value gets selected
    onChange: (value: string) => any
    // Creates a new option
    newOptionCreator?: (params: INewOptionCreatorParams) => any
    // Checks if an input is a valid new options
    isValidNewOption?: (input: IIsValidNewOptionParams) => any
    // If the selected value can be cleared
    clearable?: boolean
    // If true the query will be reset to the value and not the optional label
    resetQueryToValue?: boolean
    // The string that should be displayed in the input field of the selected item. By default it is the optional label or value.
    itemDisplayLabel?: (item: IAutoCompleteItem) => string
}

// Auto-complete interface as it is returned by the auto-complete backend APIs
interface IAutoCompleteItem {
    value: string
    label?: string
    description?: string | null
}

// Checks if a label exists that is distinct enough from the value
const hasDistinctLabel = (autoCompleteItem: IAutoCompleteItem) => autoCompleteItem.label && autoCompleteItem.label.toLowerCase() !== autoCompleteItem.value.toLowerCase()

// Renders an option item in the suggest list
const itemRenderer = (autoCompleteItem: IAutoCompleteItem, query: string, modifiers: IRenderModifiers, handleClick: () => any): JSX.Element => {
    let label: string | undefined
    let value: string | undefined = undefined
    // Do not display value and label if they are basically the same
    if(hasDistinctLabel(autoCompleteItem)) {
        label = autoCompleteItem.label
        value = autoCompleteItem.value
    } else {
        label = autoCompleteItem.value
    }
    const highlighter = (value: string) => modifiers.highlightingEnabled ? <Highlighter label={value} searchValue={query} /> : value
    const item = value || autoCompleteItem.description ? (
        <OverviewItem style={modifiers.styleWidth}>
            <OverviewItemDescription>
                <OverviewItemLine><OverflowText ellipsis={"reverse"}>{highlighter(label)}</OverflowText></OverviewItemLine>
                {value &&
                  <OverviewItemLine small={true}><OverflowText ellipsis={"reverse"}>{highlighter(value)}</OverflowText></OverviewItemLine>}
                {autoCompleteItem.description &&
                  <OverviewItemLine small={true}><OverflowText>{highlighter(autoCompleteItem.description)}</OverflowText></OverviewItemLine>}
            </OverviewItemDescription>
        </OverviewItem> ) : (
            <OverflowText style={modifiers.styleWidth} ellipsis={"reverse"}>{highlighter(label)}</OverflowText> )
    return <MenuItem
        active={modifiers.active}
        disabled={modifiers.disabled}
        key={autoCompleteItem.value}
        onClick={handleClick}
        text={item}
    />
}

// Necessary because else popovers won't be shown (still unclear why, maybe interaction with MDL)
const portalContainer = () => document.body
// Creates a new, custom option element
const queryToNewOption: (label: string) => INewOptionCreatorParams = (label: string) => ({label: label, labelKey: "label", valueKey: "value"})
const itemLabel = (itemDisplayLabel?: (item: IAutoCompleteItem) => string) => (autoCompleteItem: IAutoCompleteItem) => {
    if(hasDistinctLabel(autoCompleteItem)) {
        return itemDisplayLabel ? itemDisplayLabel(autoCompleteItem) : autoCompleteItem.label
    } else {
        return autoCompleteItem.value
    }
}

/** Input field supporting auto-complete support. */
const AutoComplete = ({ entity, ruleId, className, placeholder, creatable, onChange, value,
                          clearable = true, resetQueryToValue = false, itemDisplayLabel, ...otherProps}: IProps) => {
    const reset = clearable ? {
        resetValue: "",
        resetButtonText: "Clear value",
        resettableValue: (v: IAutoCompleteItem) => !!v?.value
    } : undefined
    const isValidNewOption = otherProps.isValidNewOption ? otherProps.isValidNewOption : () => true
    const newOptionCreator = otherProps.newOptionCreator ? otherProps.newOptionCreator : ({label}) => ({value: label})
    const create = creatable ? {
        itemFromQuery: (query: string) => isValidNewOption({label: query}) ? newOptionCreator(queryToNewOption(query)) : undefined,
        itemRenderer: createNewItemRendererFactory((query: string) => `Create option '${query}'`, "item-add-artefact"),
        showNewItemOptionFirst: true
    } : undefined
    return <div className={className}>
        <Spacing size={"tiny"} />
        <FieldItem
            labelAttributes={{
                text: placeholder,
            }}
        >
            <AutoCompleteField<IAutoCompleteItem, string>
                inputProps={{
                    placeholder: placeholder
                }}
                popoverProps={{
                    portalContainer: portalContainer()
                }}
                reset={reset}
                onChange={onChange}
                initialValue={typeof value === "string" ? {value} : value}
                itemValueSelector={item => item.value}
                onSearch={onSearchFactory(ruleId, entity)}
                itemRenderer={itemRenderer}
                itemValueRenderer={itemLabel(itemDisplayLabel)}
                noResultText={"No result."}
                createNewItem={create}
                resetQueryToValue={resetQueryToValue ? (item) => item.value : undefined}
            />
        </FieldItem>
        <Spacing size={"tiny"} />
    </div>
};

export default AutoComplete;
