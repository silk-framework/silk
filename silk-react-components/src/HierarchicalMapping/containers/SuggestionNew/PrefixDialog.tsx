import {Button, SimpleDialog, FieldItem, TextField, Checkbox, Label} from "@gui-elements/index";
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

/** Let's the user choose the URI prefix that is used for auto-generated target property URIs. */
export function PrefixDialog({ isOpen, onAdd, onDismiss, prefixList, selectedPrefix }: IProps) {
    const context = useContext(SuggestionListContext);

    const [inputPrefix, setInputPrefix] = useState(selectedPrefix);

    const [withoutPrefix, setWithoutPrefix] = useState(false);

    const handleInputPrefix = (value: string) => setInputPrefix(value);

    const handleAdd = (e) => onAdd(e, withoutPrefix ? '' : inputPrefix);

    const handleNotIncludePrefix = () => {
        const toggledValue = !withoutPrefix;
        setWithoutPrefix(toggledValue);
    }

    return <SimpleDialog
        isOpen={isOpen}
        title="Set prefix for auto-generated properties"
        hasBorder={true}
        portalContainer={context.portalContainer}
        actions={[
            <Button data-test-id="suggest-mapping-prefix-ok-btn" key="confirm" onClick={handleAdd} affirmative={true}>
                Add
            </Button>,
            <Button data-test-id="suggest-mapping-prefix-cancel-btn" key="cancel" onClick={onDismiss}>
                Cancel
            </Button>,
        ]}
    >
        <FieldItem>
            <Checkbox
                onChange={handleNotIncludePrefix}
                checked={withoutPrefix}
                labelElement={
                    <Label
                        text={"Keep source properties unchanged"}
                        info={"do not prefix them"}
                        tooltip={"If checked, the source properties are transferred unchanged to the target properties."}
                        tooltipProperties={{portalContainer: context.portalContainer}}
                        isLayoutForElement={"span"}
                        style={{ display: "inline-block"}}
                    />
                }
            />
        </FieldItem>
        <PrefixList
            prefixes={prefixList}
            selectedPrefix={withoutPrefix ? '' : inputPrefix}
            onChange={handleInputPrefix}
            disabled={withoutPrefix}
        />
        <FieldItem labelAttributes={{text: 'Selected URI prefix for auto-generated properties', htmlFor:"prefix"}}>
            <TextField
                name={'prefix'}
                id="prefix"
                onChange={(e) => handleInputPrefix(e.target.value)}
                value={withoutPrefix ? '' : inputPrefix}
                disabled={withoutPrefix}
            />
        </FieldItem>
    </SimpleDialog>
};
