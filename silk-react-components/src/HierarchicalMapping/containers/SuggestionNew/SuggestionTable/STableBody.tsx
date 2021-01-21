import React from "react";
import STableRow from "./StableRow/STableRow";
import { TableBody } from "@gui-elements/index";
import { IPageSuggestion } from "../suggestion.typings";

export default function STableBody({pageRows, selectedSources, toggleRowSelect, onModifyTarget}) {
    return <TableBody>
        {
            pageRows.map((row: IPageSuggestion) => {
                const {uri} = row;
                const selected = selectedSources.find(selected => selected === uri);

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
