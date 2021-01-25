import React, { useContext, useEffect, useState } from "react";
import {
    MenuItem,
    Select,
    Button,
    Highlighter,
    OverflowText,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemActions,
} from '@gui-elements/index';
import { ITargetWithSelected } from "../suggestion.typings";
import { SuggestionListContext } from "../SuggestionContainer";

// Select<T> is a generic component to work with your data types.
// In TypeScript, you must first obtain a non-generic reference:
const TargetSelect = Select.ofType<ITargetWithSelected>();

interface IProps {
    targets: ITargetWithSelected[];

    onChange(uri: string);
}
export default function TargetList({targets, onChange}: IProps) {
    const context = useContext(SuggestionListContext);

    const [items, setItems] = useState<ITargetWithSelected[]>(targets);

    const [inputQuery, setInputQuery] = useState<string>('');
    const selected = targets.find(t => t._selected);

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

    const itemLabel = (target: ITargetWithSelected, search: string) => <OverviewItem>
        <OverviewItemDescription>
            {target.label && <OverviewItemLine><OverflowText><Highlighter label={target.label} searchValue={search} /></OverflowText></OverviewItemLine>}
            {target.uri && <OverviewItemLine><OverflowText><Highlighter label={target.uri} searchValue={search} /></OverflowText></OverviewItemLine>}
            {target.description && <OverviewItemLine><OverflowText><Highlighter label={target.description} searchValue={search} /></OverflowText></OverviewItemLine>}
        </OverviewItemDescription>
    </OverviewItem>;

    const itemRenderer = (target: ITargetWithSelected, {handleClick}) => {
        return <MenuItem
            text={<div style={{width: "40em", maxWidth: "90vw"}}>{itemLabel(target, inputQuery)}</div>}
            key={target.uri}
            onClick={handleClick}
            active={target.uri === selected.uri}
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
            fill={true}
            rightIcon="select-caret"
            text={itemLabel(selected, context.search)}
        />
    </TargetSelect>

}
