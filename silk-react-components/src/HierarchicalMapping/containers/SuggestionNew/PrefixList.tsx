import React, { useContext, useEffect, useState } from "react";
import { MenuItem, Select, Button, Highlighter } from '@gui-elements/index';
import { SuggestionListContext } from "./SuggestionContainer";
import { IPrefix } from "./suggestion.typings";

// Select<T> is a generic component to work with your data types.
// In TypeScript, you must first obtain a non-generic reference:
const PrefixSelect = Select.ofType<IPrefix>();

export default function PrefixList({prefixes, selectedPrefix, onChange}) {
    const context = useContext(SuggestionListContext);

    const [items, setItems] = useState<IPrefix[]>([]);

    const [inputQuery, setInputQuery] = useState<string>('');

    useEffect(() => {
        setItems(prefixes);
    }, [prefixes]);

    const areTargetsEqual = (targetA: IPrefix, targetB: IPrefix) => {
        // Compare only the titles (ignoring case) just for simplicity.
        return targetA.uri?.toLowerCase() === targetB.uri?.toLowerCase();
    }

    const handleSelectTarget = (uri: string) => {
        onChange(uri);
    };

    const itemLabel = (prefix: IPrefix, search: string) => <>
        {prefix.uri && <p><Highlighter label={prefix.uri} searchValue={search} /></p>}
    </>;

    const itemRenderer = (prefix: IPrefix, {handleClick}) => {
        if (prefix === selectedPrefix) {
            return null;
        }
        return <MenuItem
            text={itemLabel(prefix, inputQuery)}
            key={prefix.uri}
            onClick={handleClick}
        />
    }

    const handleQueryChange = (value) => {
        if (!value) {
            setItems(prefixes);
        } else {
            const filtered = prefixes.filter(item =>
                item.uri.includes(value) ||
                item.key.includes(value)
            );

            setItems(filtered);
        }
        setInputQuery(value);
    }

    const createPrefix = (value: string) => {
        return {
            uri: value,
            key: 'MANUALLY_ADDED'
        }
    };

    const renderCreatePrefixOptionRenderer = (
        query: string,
        active: boolean,
        handleClick: React.MouseEventHandler<HTMLElement>,
    ) => (
        <MenuItem
            icon="add"
            text={`Create "${query}"`}
            active={active}
            onClick={handleClick}
            shouldDismissPopover={false}
        />
    );
    return <PrefixSelect
        filterable={true}
        onItemSelect={t => handleSelectTarget(t.uri)}
        items={items}
        itemRenderer={itemRenderer}
        itemsEqual={areTargetsEqual}
        onQueryChange={handleQueryChange}
        query={inputQuery}
        createNewItemFromQuery={createPrefix}
        createNewItemRenderer={renderCreatePrefixOptionRenderer}
        createNewItemPosition={"last"}
        popoverProps={{
            minimal: true,
            portalContainer: context.portalContainer
        }}
    >
        <Button
            rightIcon="select-caret"
            text={selectedPrefix || 'Select the Auto generation prefix'}
        />
    </PrefixSelect>

}
