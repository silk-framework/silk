import React, { useContext, useEffect, useState } from "react";
import { MenuItem, Select, Button, Highlighter } from '@gui-elements/index';
import { ITargetWithSelected } from "../suggestion.typings";
import { SuggestionListContext } from "../SuggestionContainer";

// Select<T> is a generic component to work with your data types.
// In TypeScript, you must first obtain a non-generic reference:
const TargetSelect = Select.ofType<ITargetWithSelected>();

export default function TargetList({targets, onChange}) {
    const context = useContext(SuggestionListContext);

    const [items, setItems] = useState<ITargetWithSelected[]>(targets);

    const [inputQuery, setInputQuery] = useState<string>('');

    useEffect(() => {
        setItems(items);
    }, [targets]);

    const areTargetsEqual = (targetA: ITargetWithSelected, targetB: ITargetWithSelected) => {
        // Compare only the titles (ignoring case) just for simplicity.
        return targetA.uri?.toLowerCase() === targetB.uri?.toLowerCase();
    }

    const handleSelectTarget = (uri) => {
        onChange(uri);
    };

    const itemLabel = (target: ITargetWithSelected, search: string) => <>
        {target.label && <p><Highlighter label={target.label} searchValue={search} /></p>}
        {target.uri && <p><Highlighter label={target.uri} searchValue={search} /></p>}
        {target.description && <p><Highlighter label={target.description} searchValue={search} /></p>}
    </>;

    const itemRenderer = (target: ITargetWithSelected, {handleClick}) => {
        if (target._selected) {
            return null;
        }
        return <MenuItem
            text={itemLabel(target, inputQuery)}
            key={target.uri}
            onClick={handleClick}
        />
    }

    const handleQueryChange = (value) => {
        if (!value) {
            setItems(targets);
        } else {
            const filtered = targets.filter(o =>
                o.uri?.includes(value) ||
                o.label?.includes(value) ||
                o.description?.includes(value)
            );
            setItems(filtered);
        }
        setInputQuery(value);
    }

    const selected = targets.find(t => t._selected);
    return <TargetSelect
        filterable={targets.length > 1}
        onItemSelect={t => handleSelectTarget(t.uri)}
        items={items}
        itemRenderer={itemRenderer}
        itemsEqual={areTargetsEqual}
        popoverProps={{
            minimal: true,
            portalContainer: context.portalContainer
        }}
        onQueryChange={handleQueryChange}
        query={inputQuery}
    >
        <Button
            rightIcon="select-caret"
            text={itemLabel(selected, context.search)}
        />
    </TargetSelect>

}
