import React, { useContext, useEffect, useState } from "react";
import { MenuItem, FieldItem, Select, Button, Highlighter } from "@eccenca/gui-elements";
import { SuggestionListContext } from "./SuggestionContainer";
import { IPrefix } from "./suggestion.typings";

// Select<T> is a generic component to work with your data types.
// In TypeScript, you must first obtain a non-generic reference:
const PrefixSelect = Select.ofType<IPrefix>();

interface IProps {
    disabled?: boolean;

    onChange(uri: string);

    prefixes: IPrefix[];

    selectedPrefix: string;
}

/** The selection of URI prefixes used for auto-generated properties */
export default function PrefixList({ prefixes, selectedPrefix, onChange, disabled }: IProps) {
    const context = useContext(SuggestionListContext);

    const [items, setItems] = useState<IPrefix[]>([]);

    const [inputQuery, setInputQuery] = useState<string>("");

    const [selectedItem, setSelectedItem] = useState(selectedPrefix);

    useEffect(() => {
        setItems(prefixes);
    }, [prefixes]);

    const areTargetsEqual = (targetA: IPrefix, targetB: IPrefix) => {
        // Compare only the titles (ignoring case) just for simplicity.
        return targetA.uri?.toLowerCase() === targetB.uri?.toLowerCase();
    };

    const handleSelectTarget = (uri: string) => {
        setSelectedItem(uri);
        onChange(uri);
    };

    const itemLabel = (prefix: IPrefix, search: string) => (
        <>
            {prefix.uri && (
                <p>
                    <Highlighter label={`${prefix.key}: ${prefix.uri}`} searchValue={search} />
                </p>
            )}
        </>
    );

    const itemRenderer = (prefix: IPrefix, { handleClick }) => {
        return <MenuItem text={itemLabel(prefix, inputQuery)} key={prefix.uri} onClick={handleClick} />;
    };

    const handleQueryChange = (value) => {
        if (!value) {
            setItems(prefixes);
        } else {
            const filtered = prefixes.filter((item) => item.uri.includes(value) || item.key.includes(value));

            setItems(filtered);
        }
        setInputQuery(value);
    };

    const createPrefix = (value: string) => {
        return {
            uri: value,
            key: "MANUALLY_ADDED",
        };
    };

    const renderCreatePrefixOptionRenderer = (
        query: string,
        active: boolean,
        handleClick: React.MouseEventHandler<HTMLElement>
    ) => (
        <MenuItem
            icon="item-add-artefact"
            text={`Create "${query}"`}
            active={active}
            onClick={handleClick}
            shouldDismissPopover={false}
        />
    );

    return (
        <FieldItem labelProps={{ text: "Use known prefix" }}>
            <PrefixSelect
                filterable={true}
                onItemSelect={(t) => handleSelectTarget(t.uri)}
                items={items}
                itemRenderer={itemRenderer}
                itemsEqual={areTargetsEqual}
                onQueryChange={handleQueryChange}
                disabled={disabled}
                query={inputQuery}
                createNewItemFromQuery={createPrefix}
                createNewItemRenderer={renderCreatePrefixOptionRenderer}
                createNewItemPosition={"last"}
                contextOverlayProps={{
                    minimal: true,
                    portalContainer: context.portalContainer,
                }}
            >
                <Button
                    rightIcon="toggler-caret"
                    text={
                        selectedItem === selectedPrefix ? selectedItem : "Select prefix for auto-generated properties"
                    }
                    disabled={disabled}
                />
            </PrefixSelect>
        </FieldItem>
    );
}
