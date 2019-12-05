import { Button, Menu, MenuItem, Popover, Position } from "@blueprintjs/core";
import React from "react";
import { ISorterListItemState } from "../../../state/ducks/dashboard/typings";

interface IProps {
    sortersList: ISorterListItemState[],
    onSort(id: string): void
}

export default function SortButton({ sortersList, onSort }: IProps) {
    return (
        <Popover content={
            <Menu>
                {
                    sortersList.map(item =>
                        <MenuItem key={item.id} text={item.label} onClick={() => onSort(item.id)}/>
                    )
                }
            </Menu>
        } position={Position.BOTTOM_RIGHT}>
            <Button icon="sort-asc"/>
        </Popover>
    )
}
