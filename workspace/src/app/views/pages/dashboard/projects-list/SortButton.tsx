import React from "react";
import { ISorterListItemState } from "@ducks/dashboard/typings";
import Popover from "../../../../wrappers/popover";
import Menu from "../../../../wrappers/menu";
import Button from "../../../../wrappers/button";
import MenuItem from "../../../../wrappers/menu-item";
import { Position } from "@wrappers/constants";

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
