import React from "react";
import {IAppliedSorterState, ISorterListItemState} from "@ducks/workspace/typings";

import {
    ContextMenu,
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
            <ContextMenu togglerElement='list-sort' togglerText="Sort options">
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
            </ContextMenu>
        </div>
    )
}
