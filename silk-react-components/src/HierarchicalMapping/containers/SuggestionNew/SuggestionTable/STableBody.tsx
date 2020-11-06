import React from "react";
import STableRow from "./STableRow";
import { TableBody } from "@gui-elements/index";
import { IPageSuggestion } from "../suggestion.typings";

export default function STableBody({pageRows, selectedSources, toggleRowSelect, onModifyTarget}) {
    return <TableBody>
        {
            pageRows.map((row: IPageSuggestion) => {
                const {source} = row;
                const selected = selectedSources.find(selected => selected === source);

                return <STableRow
                    key={source}
                    row={row}
                    onRowSelect={toggleRowSelect}
                    selected={selected}
                    onModifyTarget={target => onModifyTarget(row, target)}
                />
            })
        }
    </TableBody>

}
