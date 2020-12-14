import React, { useContext } from "react";
import { ContextMenu } from "@gui-elements/index";
import { ITargetWithSelected } from "../../suggestion.typings";
import { SuggestionListContext } from "../../SuggestionContainer";

interface IProps {
    source?: string | ITargetWithSelected[];
}

export function ExampleInfoBox({source}: IProps) {
    const context = useContext(SuggestionListContext);
    const {exampleValues, portalContainer} = context;

    let examples = [];
    if (typeof source === 'string') {
        examples = exampleValues[source as string];
    } else if (Array.isArray(source)) {
        const selected = source.find(t => t._selected);
        if (selected && exampleValues[selected.uri]) {
            examples.push(exampleValues[selected.uri]);
        }
    }

    return (
        examples?.length && <ContextMenu
            portalContainer={portalContainer}
            togglerElement={'item-info'}
        >
            <ul>
                {
                    examples.map(example =>
                        <li key={example}><p>{example}</p></li>
                    )
                }
            </ul>
        </ContextMenu>
    )
}
