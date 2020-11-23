import React, { useContext } from "react";
import TargetList from "./TargetList";
import TypesList from "./TypesList";
import { SuggestionTypeValues } from "../suggestion.typings";
import { Checkbox, ContextMenu, TableCell, TableRow, } from "@gui-elements/index";
import { SuggestionListContext } from "../SuggestionContainer";
import Highlighter from "../../../elements/Highlighter";

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
    const {exampleValues, portalContainer} = context;

    const examples = exampleValues[source];

    return <TableRow>
        <TableCell>
            <Checkbox
                onChange={() => onRowSelect(row)}
                checked={!!selected}
            />
        </TableCell>
        <TableCell>
            <Highlighter label={label || source} searchValue={context.search} />
            {!label && source && <p><Highlighter label={source} searchValue={context.search} /></p>}
            {description && <p><Highlighter label={description} searchValue={context.search} /></p>}
            {
                examples && <ContextMenu
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
            }
        </TableCell>
        <TableCell>
            <div/>
        </TableCell>
        <TableCell>
            <TargetList
                targets={candidates}
                onChange={handleModifyTarget}
            />
        </TableCell>
        <TableCell>
            <TypesList
                onChange={(type) => handleModifyTarget(selectedTarget.uri, type)}
                selected={selectedType}
            />
        </TableCell>
    </TableRow>
}
