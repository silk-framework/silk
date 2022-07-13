import React from "react";
import STableRow from "./StableRow/STableRow";
import { TableBody } from "@eccenca/gui-elements";
import {IPageSuggestion, ITargetWithSelected} from "../suggestion.typings";
import {Set} from "immutable"

interface IProps {
    // Suggestions from the current page of the table
    pageRows: IPageSuggestion[]
    // The selected sources
    selectedSources: Set<string>
    // Toggles the selection of a row
    toggleRowSelect: (pageSuggestion: IPageSuggestion) => void
    // Selects a specific target from the target drop down
    onModifyTarget: (row: IPageSuggestion, targets: ITargetWithSelected[]) => void
}

/** Shows the currently visible rows of a table, i.e. the current page. */
export default function STableBody({pageRows, selectedSources, toggleRowSelect, onModifyTarget}: IProps) {
    return <TableBody>
        {
            pageRows.map((row: IPageSuggestion) => {
                const {uri} = row;
                const selected = selectedSources.contains(uri);

                return <STableRow
                    key={uri}
                    row={row}
                    onRowSelect={toggleRowSelect}
                    selected={selected}
                    onModifyTarget={target => onModifyTarget(row, target)}
                />
            })
        }
    </TableBody>

}
