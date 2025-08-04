import React from "react";
import { IAppliedSorterState, ISorterListItemState } from "@ducks/workspace/typings";

import { ContextMenu, MenuItem } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { useStoreGlobalTableSettings } from "../../../hooks/useStoreGlobalTableSettings";

interface IProps {
    sortersList: ISorterListItemState[];
    activeSort: IAppliedSorterState;

    onSort(id: string): void;
}

export default function SortButton({ sortersList, activeSort, onSort }: IProps) {
    const [t] = useTranslation();
    const storeSettingsPath = React.useMemo(
        () => (sortersList.find((l) => l.id === "runningTime") ? "activities" : "workbench"),
        [sortersList],
    );
    const { updateGlobalTableSettings, globalTableSettings } = useStoreGlobalTableSettings({
        sorters: sortersList,
        activeSortBy: activeSort.sortBy,
        onSort,
        path: storeSettingsPath,
    });

    const handleMenuClick = React.useCallback(
        (itemId: string) => {
            onSort(itemId);
            updateGlobalTableSettings({
                [storeSettingsPath]: { ...globalTableSettings[storeSettingsPath], sortBy: itemId },
            });
        },
        [globalTableSettings, storeSettingsPath],
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
