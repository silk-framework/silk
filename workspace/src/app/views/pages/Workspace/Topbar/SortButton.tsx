import React from "react";
import {IAppliedSorterState, ISorterListItemState} from "@ducks/workspace/typings";
import Popover from "@wrappers/popover";
import Menu from "@wrappers/menu";
import Button from "@wrappers/button";
import MenuItem from "@wrappers/menu-item";
import {IconNames, Position} from "@wrappers/constants";

interface IProps {
    sortersList: ISorterListItemState[],
    activeSort?: IAppliedSorterState;

    onSort(id: string): void
}

export default function SortButton({sortersList, activeSort, onSort}: IProps) {
    return (
        <div className={'sortButton'}>
            <Popover
                     content={
                         <Menu>
                             {
                                 sortersList.map(item =>
                                     <MenuItem
                                         key={item.id}
                                         text={item.label}
                                         icon={
                                             activeSort.sortBy === item.id
                                                 ? activeSort.sortOrder === 'ASC' ? IconNames.ARROW_UP : IconNames.ARROW_DOWN
                                                 : null
                                         }
                                         onClick={() => onSort(item.id)}
                                     />
                                 )
                             }
                         </Menu>
                     }
                     position={Position.BOTTOM_RIGHT}
            >
                <Button icon={IconNames.SORT_ASC}/>
            </Popover>
        </div>
    )
}
