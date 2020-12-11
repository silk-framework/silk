import React, { useContext } from "react";
import TargetList from "./TargetList";
import TypesList from "./TypesList";
import { ITargetWithSelected, SuggestionTypeValues } from "../suggestion.typings";
import { Checkbox, ContextMenu, TableCell, TableRow, Highlighter } from "@gui-elements/index";
import { SuggestionListContext } from "../SuggestionContainer";

export default function STableRow({row, onRowSelect, selected, onModifyTarget}) {
    const context = useContext(SuggestionListContext);
    const {source, candidates, label, description} = row;

    const handleModifyTarget = (uri: string, type?: SuggestionTypeValues) => {
        const modified = candidates.map(target => {
            const isSelected = uri === target.uri;

            return {
                ...target,
                _selected: isSelected,
                type: isSelected ? type || target.type : target.type
            }
        });

        onModifyTarget(modified);
    };

    const selectedTarget = candidates.find(t => t._selected);
    const selectedType = selectedTarget ? selectedTarget.type : 'value';

    const renderExampleIcon = (source: string | ITargetWithSelected[], context) => {
        const {exampleValues, portalContainer, isFromDataset} = context;
        let examples = [];
        if (isFromDataset) {
            examples = exampleValues[source as string];
        } else if (Array.isArray(source)) {
            const selected = source.find(t => t._selected);
            if (selected && exampleValues[selected.uri])  {
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
    };


    return <TableRow>
        <TableCell>
            <Checkbox
                onChange={() => onRowSelect(row)}
                checked={!!selected}
            />
        </TableCell>
        <TableCell>
            <Highlighter label={label || source} searchValue={context.search} />
            {label ? <p><Highlighter label={source} searchValue={context.search} /></p> : null }
            {description && <p><Highlighter label={description} searchValue={context.search} /></p>}
            {context.isFromDataset && renderExampleIcon(source, context)}
        </TableCell>
        <TableCell>
            <div/>
        </TableCell>
        <TableCell>
            <TargetList
                targets={candidates}
                onChange={handleModifyTarget}
            />
            {!context.isFromDataset && renderExampleIcon(candidates, context)}
        </TableCell>
        <TableCell>
            <TypesList
                onChange={(type) => handleModifyTarget(selectedTarget.uri, type)}
                selected={selectedType}
            />
        </TableCell>
    </TableRow>
}
