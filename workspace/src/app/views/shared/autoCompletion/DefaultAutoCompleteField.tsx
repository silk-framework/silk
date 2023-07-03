import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import React from "react";
import { useTranslation } from "react-i18next";
import { SuggestField, suggestFieldUtils } from "@eccenca/gui-elements";
import { autoCompleteItemRenderer } from "../../pages/MappingEditor/HierarchicalMapping/components/AutoComplete";

interface Props {
    /** Auto-complete search function. The parameter will be rendered as auto-complete field. */
    onSearch: (textQuery: string) => Promise<IAutocompleteDefaultResponse[]>;
    /** Callback when a value has been selected. */
    onChange: (value: string) => any;
    /** Optional initial value. */
    initialValue?: IAutocompleteDefaultResponse;
    /** The prefix that is put in front of the value when creating a new value. */
    newItemPrefix?: string;
    /** Optional ID, e.g. for testing. */
    id?: string;
}

/** Default auto complete field that works with auto complete responses of the form {"value": "the value", "label": "Optional label"}. */
export const DefaultAutoCompleteField = ({ onChange, onSearch, initialValue, newItemPrefix, id }: Props) => {
    const [t] = useTranslation();

    const customItemPrefixFn = newItemPrefix
        ? (query: string) => `${newItemPrefix}${query}`
        : (query: string) => t("ParameterWidget.AutoComplete.createNewItem", { query });

    return (
        <SuggestField<IAutocompleteDefaultResponse, string>
            inputProps={
                id
                    ? {
                          id: id,
                      }
                    : undefined
            }
            onSearch={onSearch}
            itemRenderer={autoCompleteItemRenderer}
            noResultText={t("common.messages.noResults")}
            initialValue={initialValue}
            onChange={onChange}
            itemValueSelector={autoCompleteValue}
            itemValueRenderer={autoCompleteStringRepresentation}
            itemValueString={autoCompleteValue}
            createNewItem={{
                itemFromQuery: (value: string) => ({ value: value }),
                itemRenderer: suggestFieldUtils.createNewItemRendererFactory(customItemPrefixFn),
            }}
            reset={{
                resetValue: "",
                resetButtonText: t("common.action.resetSelection"),
                resettableValue: () => true,
            }}
        />
    );
};

const autoCompleteValue = (item: IAutocompleteDefaultResponse): string => {
    return item.value;
};

const autoCompleteStringRepresentation = (item: IAutocompleteDefaultResponse): string => {
    return item.label && item.label !== item.value ? `${item.label} (${item.value})` : item.value;
};
