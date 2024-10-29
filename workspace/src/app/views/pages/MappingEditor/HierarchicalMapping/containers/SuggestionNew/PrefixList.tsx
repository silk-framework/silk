import React, { useEffect, useState } from "react";
import { Button, FieldItem, Highlighter, MenuItem, Select } from "@eccenca/gui-elements";
import { IPrefix } from "./suggestion.typings";

interface IProps {
    disabled?: boolean;

    onChange(uri: string);

    prefixes: IPrefix[];

    selectedPrefix: string;
}

/** The selection of URI prefixes used for auto-generated properties */
export default function PrefixList({ prefixes, selectedPrefix, onChange, disabled }: IProps) {
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
            <Select<IPrefix>
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
            >
                <Button
                    rightIcon="toggler-caretdown"
                    text={
                        selectedItem === selectedPrefix ? selectedItem : "Select prefix for auto-generated properties"
                    }
                    disabled={disabled}
                />
            </Select>
        </FieldItem>
    );
}
