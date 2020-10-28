import React from "react";
import { TableCell, TableRow, TableSelectRow } from "carbon-components-react";
import TargetList from "./TargetList";
import TypesList from "./TypesList";
import { SuggestionTypeValues } from "../suggestion.typings";

export default function STableRow({row, onRowSelect, selected, onModifyTarget}) {
    const {source, target} = row;

    const handleModifyTarget = (uri: string, type?: SuggestionTypeValues) => {
        const modified = target.map(target => {
            const isSelected = uri === target.uri;

            return {
                ...target,
                _selected: isSelected,
                type: isSelected ? type || target.type : target.type
            }
        });

        onModifyTarget(modified);
    };

    const selectedTarget = target.find(t => t._selected);
    const selectedType = selectedTarget ? selectedTarget.type : 'value';

    return <TableRow>
        <TableSelectRow
            name={source}
            id={source}
            onSelect={() => onRowSelect(row)}
            checked={!!selected}
            ariaLabel={'select row'}
        />
        <TableCell>
            <select>
                <option value={source}>{source}</option>
            </select>
        </TableCell>
        <TableCell>
            <div/>
        </TableCell>
        <TableCell>
            <TargetList
                targets={target}
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
