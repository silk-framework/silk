import React, { useContext } from "react";
import TypesList from "../TypesList";
import { SuggestionTypeValues } from "../../suggestion.typings";
import { Checkbox, TableCell, TableRow } from "@gui-elements/index";
import { SuggestionListContext } from "../../SuggestionContainer";
import { SourceCellData } from "./SourceCellData";
import { TargetCellData } from "./TargetCellData";
import TargetList from "../TargetList";
import TargetInfoBox from "./TargetInfoBox";

export default function STableRow({row, onRowSelect, selected, onModifyTarget}) {
    const context = useContext(SuggestionListContext);
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
            <Checkbox onChange={() => onRowSelect(row)} checked={!!selected}/>
        </TableCell>
        <TableCell>
            {
                context.isFromDataset
                    ? <SourceCellData label={source} search={context.search}/>
                    : <TargetCellData search={context.search} target={row} />
            }
        </TableCell>
        <TableCell>
            <div/>
        </TableCell>
        <TableCell>
            {
                context.isFromDataset
                    ? <>
                        <TargetList targets={candidates} onChange={handleModifyTarget}/>
                        <TargetInfoBox selectedTarget={selectedTarget} />
                    </>
                    : <SourceCellData label={selectedTarget.label || selectedTarget.uri} search={context.search}/>
            }
        </TableCell>
        <TableCell>
            <TypesList
                onChange={(type) => handleModifyTarget(selectedTarget.uri, type)}
                selected={selectedType}
            />
        </TableCell>
    </TableRow>
}
