import React from "react";
import {IAppliedSorterState, ISorterListItemState, SortModifierType} from "@ducks/workspace/typings";

import { ContextMenu, MenuItem } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { useStoreGlobalTableSettings } from "../../../hooks/useStoreGlobalTableSettings";
import {GlobalTableContext} from "../../../GlobalContextsWrapper";

interface IProps {
    sortersList: ISorterListItemState[];
    activeSort: IAppliedSorterState;
}

export default function SortButton({ sortersList, activeSort }: IProps) {
    const [t] = useTranslation();
    const { updateGlobalTableSettings } = React.useContext(GlobalTableContext)

    const handleMenuClick = React.useCallback(
        (itemId: string) => {
            const {sortBy, sortOrder} = activeSort
            let newSortOrder: SortModifierType = activeSort.sortOrder || "ASC";
            if (itemId === sortBy) {
                newSortOrder = activeSort.sortOrder === "ASC" ? "DESC" : "ASC";
            }
            updateGlobalTableSettings({
                sortBy: itemId,
                sortOrder: newSortOrder,
            })
        },
        [updateGlobalTableSettings, activeSort],
    );

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
                        onClick={() => handleMenuClick(item.id)}
                    />
                ))}
            </ContextMenu>
        </div>
    );
}
