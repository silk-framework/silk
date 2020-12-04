import { Button, SimpleDialog } from "@gui-elements/index";
import PrefixList from "./PrefixList";
import React, { useContext, useState } from "react";
import { SuggestionListContext } from "./SuggestionContainer";
import { IPrefix } from "./suggestion.typings";

interface IProps {
    isOpen: boolean;

    onAdd(e: any, prefix:string);

    onDismiss(e: any);

    prefixList: IPrefix[];

    selectedPrefix: string;
}

export function PrefixDialog({ isOpen, onAdd, onDismiss, prefixList, selectedPrefix }) {
    const context = useContext(SuggestionListContext);

    const [inputPrefix, setInputPrefix] = useState(selectedPrefix);

    const handleInputPrefix = (value: string) => setInputPrefix(value);

    const handleAdd = (e) => onAdd(e, inputPrefix);

    return <SimpleDialog
        isOpen={isOpen}
        portalContainer={context.portalContainer}
        actions={[
            <Button key="confirm" onClick={handleAdd}>
                Confirm
            </Button>,
            <Button key="cancel" onClick={onDismiss}>
                Cancel
            </Button>,
        ]}
    >
        <PrefixList
            prefixes={prefixList}
            selectedPrefix={inputPrefix}
            onChange={handleInputPrefix}
        />
    </SimpleDialog>
};
