import React from "react";
import { IAppliedSorterState, ISorterListItemState } from "@ducks/workspace/typings";

import { ContextMenu, MenuItem } from "@gui-elements/index";
import { useTranslation } from "react-i18next";

interface IProps {
    sortersList: ISorterListItemState[];
    activeSort: IAppliedSorterState;

    onSort(id: string): void;
}

export default function SortButton({ sortersList, activeSort, onSort }: IProps) {
    const [t] = useTranslation();

    return (
        <div className={"sortButton"} data-test-id={"sortButton"}>
            <ContextMenu togglerElement="list-sort" togglerText={t("common.words.sortOptions", "Sort options")}>
                {sortersList.map((item) => (
                    <MenuItem
                        active={activeSort.sortBy === item.id ? true : false}
                        key={item.id}
                        text={item.label}
                        icon={
                            activeSort.sortBy && activeSort.sortBy === item.id
                                ? activeSort.sortOrder === "ASC"
                                    ? "list-sortasc"
                                    : "list-sortdesc"
                                : undefined
                        }
                        onClick={() => onSort(item.id)}
                    />
                ))}
            </ContextMenu>
        </div>
    );
}
