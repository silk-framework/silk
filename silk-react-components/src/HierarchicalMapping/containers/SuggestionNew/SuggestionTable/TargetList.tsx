import React, { useContext } from "react";
import { Button, MenuItem, Select } from '@gui-elements/index';
import { ITargetWithSelected } from "../suggestion.typings";
import { SuggestionListContext } from "../SuggestionContainer";

// Select<T> is a generic component to work with your data types.
// In TypeScript, you must first obtain a non-generic reference:
const TargetSelect = Select.ofType<ITargetWithSelected>();

export default function TargetList({targets, onChange}) {
    const context = useContext(SuggestionListContext);

    const selected = targets.find(t => t._selected);

    const areTargetsEqual = (targetA: ITargetWithSelected, targetB: ITargetWithSelected) => {
        // Compare only the titles (ignoring case) just for simplicity.
        return targetA.uri?.toLowerCase() === targetB.uri?.toLowerCase();
    }

    const handleSelectTarget = (uri) => {
        onChange(uri);
    };

    const itemLabel = (target: ITargetWithSelected) => <>
        {target.label && <p><b>{target.label}</b></p>}
        {target.uri && <p>{target.uri}</p>}
        {target.description && <p>{target.description}</p>}
    </>;

    const itemRenderer = (target: ITargetWithSelected, {handleClick}) => {
        if (target._selected) {
            return null;
        }
        return <MenuItem
            text={itemLabel(target)}
            key={target.uri}
            onClick={handleClick}
        />
    }

    return <TargetSelect
        filterable={false}
        onItemSelect={t => handleSelectTarget(t.uri)}
        items={targets}
        itemRenderer={itemRenderer}
        itemsEqual={areTargetsEqual}
        popoverProps={{
            minimal: true,
            portalContainer: context.portalContainer
        }}
    >
        <Button
            rightIcon="select-caret"
            text={itemLabel(selected)}
        />
    </TargetSelect>

}
