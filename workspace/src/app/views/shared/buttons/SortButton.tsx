import React from "react";
import { IAppliedSorterState, ISorterListItemState } from "@ducks/workspace/typings";

import { ContextMenu, MenuItem } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { settingsConfig, useStoreGlobalTableSettings } from "../../../hooks/useStoreGlobalTableSettings";
import { useLocation } from "react-router";

interface IProps {
    sortersList: ISorterListItemState[];
    activeSort: IAppliedSorterState;

    onSort(id: string): void;
}

export default function SortButton({ sortersList, activeSort, onSort }: IProps) {
    const [t] = useTranslation();
    const location = useLocation();
    const pathname = location.pathname.split("/").slice(-1)[0] as settingsConfig["path"];
    const { updateGlobalTableSettings } = useStoreGlobalTableSettings();

    const handleMenuClick = React.useCallback(
        (itemId: string) => {
            onSort(itemId);
            updateGlobalTableSettings({
                sortBy: itemId,
            });
        },
        [pathname],
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
