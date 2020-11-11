import React from "react";
import TargetList from "./TargetList";
import TypesList from "./TypesList";
import { SuggestionTypeValues } from "../suggestion.typings";
import { Checkbox, TableCell, TableRow, } from "@gui-elements/index";

export default function STableRow({row, onRowSelect, selected, onModifyTarget}) {
    const {source, candidates} = row;

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

    return <TableRow>
        <TableCell>
            <Checkbox
                onChange={() => onRowSelect(row)}
                checked={!!selected}
            />
        </TableCell>
        <TableCell>
            {source}
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
