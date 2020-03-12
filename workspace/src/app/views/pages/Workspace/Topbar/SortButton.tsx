import React from "react";
import {IAppliedSorterState, ISorterListItemState} from "@ducks/workspace/typings";
import Popover from "@wrappers/blueprint/popover";
import {Position} from "@wrappers/blueprint/constants";

import {
    Icon,
    Menu,
    MenuItem,
} from '@wrappers/index';

interface IProps {
    sortersList: ISorterListItemState[],
    activeSort?: IAppliedSorterState;

    onSort(id: string): void
}

export default function SortButton({sortersList, activeSort, onSort}: IProps) {
    return (
        <div className={'sortButton'}>
            <Popover
                     position={Position.BOTTOM_RIGHT}
            >
                <Icon name='list-sort'/>
                <Menu>
                    {
                        sortersList.map(item =>
                            <MenuItem
                                key={item.id}
                                text={item.label}
                                icon={
                                    activeSort.sortBy === item.id
                                        ? activeSort.sortOrder === 'ASC' ? 'list-sortasc' : 'list-sortdesc'
                                        : null
                                }
                                onClick={() => onSort(item.id)}
                            />
                        )
                    }
                </Menu>
            </Popover>
        </div>
    )
}
