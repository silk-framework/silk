import React from 'react';
import {autocompleteAsync} from '../store';
import {AutoCompleteField, FieldItem} from "@gui-elements/index";

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
}

// Auto-complete interface as it is returned by the auto-complete backend APIs
interface IAutoCompleteItem {
    value: string
    label?: string
    description?: string | null
}

const portalContainer = () => document.body
const queryToNewOption: (label: string) => INewOptionCreatorParams = (label: string) => ({label: label, labelKey: "label", valueKey: "value"})

const AutoComplete = ({ entity, ruleId, className, placeholder, creatable, onChange, value, clearable = true, ...otherProps}: IProps) => {
    const reset = clearable ? {
        resetValue: "",
        resetButtonText: "Clear value",
        resettableValue: (v: IAutoCompleteItem) => !!v?.value
    } : undefined
    const isValidNewOption = otherProps.isValidNewOption ? otherProps.isValidNewOption : () => true
    const newOptionCreator = otherProps.newOptionCreator ? otherProps.newOptionCreator : ({label}) => ({value: label})
    const create = creatable ? {
        itemFromQuery: (query: string) => isValidNewOption({label: query}) ? newOptionCreator(queryToNewOption(query)) : undefined,
        itemRenderer: (
            query: string,
            active: boolean,
            handleClick: React.MouseEventHandler<HTMLElement>
        ) => {
            return <div>{query}</div>
        }
    } : undefined
    return <div className={className}>
        <FieldItem
            labelAttributes={{
                text: placeholder,
            }}
        >
            <AutoCompleteField<IAutoCompleteItem, string>
                inputProps={{
                    placeholder: "Value path"
                }}
                popoverProps={{
                    portalContainer: portalContainer()
                }}
                reset={reset}
                onChange={onChange}
                initialValue={typeof value === "string" ? {value} : value}
                itemValueSelector={item => item.value}
                onSearch={onSearchFactory(ruleId, entity)}
                itemRenderer={item => item.value}
                itemValueRenderer={item => item.value}
                noResultText={"No result."}
                createNewItem={create}
            />
        </FieldItem>
    </div>
};

export default AutoComplete;
