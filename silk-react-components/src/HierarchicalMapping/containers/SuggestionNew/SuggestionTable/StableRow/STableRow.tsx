import React, { useContext } from "react";
import TypesList from "../TypesList";
import { IPageSuggestion, ITargetWithSelected, SuggestionTypeValues } from "../../suggestion.typings";
import { Checkbox, Highlighter, OverflowText, TableCell, TableRow } from "@gui-elements/index";
import { SuggestionListContext } from "../../SuggestionContainer";
import { SourceCellData } from "./SourceCellData";
import TargetList from "../TargetList";
import TargetInfoBox from "./TargetInfoBox";
import { ExampleInfoBox } from "./ExampleInfoBox";

export default function STableRow({row, onRowSelect, selected, onModifyTarget}) {
    const context = useContext(SuggestionListContext);
    const {uri, candidates} = row;

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

    const {search} = context;
    return <TableRow>
        <TableCell>
            <Checkbox onChange={() => onRowSelect(row)} checked={!!selected}/>
        </TableCell>
        <TableCell>
            {
                context.isFromDataset
                    ? <SourceCellData label={uri} search={search}/>
                    : <>
                        {row.label && <p><OverflowText><Highlighter label={row.label} searchValue={search}/></OverflowText></p>}
                        {row.uri && <p><OverflowText><Highlighter label={row.uri} searchValue={search}/></OverflowText></p>}
                        {
                            row.description &&
                            <p><OverflowText><Highlighter label={row.description} searchValue={search}/></OverflowText></p>
                        }
                        <TargetInfoBox selectedTarget={row} />
                    </>
            }
        </TableCell>
        <TableCell>
            <div/>
        </TableCell>
        <TableCell>
            <TargetList targets={candidates} onChange={handleModifyTarget}/>
            {
                context.isFromDataset
                    ? <TargetInfoBox selectedTarget={selectedTarget}/>
                    : <ExampleInfoBox source={selectedTarget.uri}/>

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
