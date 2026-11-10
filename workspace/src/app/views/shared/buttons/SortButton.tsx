import React from "react";
import { IAppliedSorterState, ISorterListItemState } from "@ducks/workspace/typings";

import { ContextMenu, MenuItem } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { GlobalTableContext } from "../../../GlobalContextsWrapper";

interface IProps {
    sortersList: ISorterListItemState[];
    activeSort: IAppliedSorterState;
}

export default function SortButton({ sortersList, activeSort }: IProps) {
    const [t] = useTranslation();
    const { updateGlobalTableSettings } = React.useContext(GlobalTableContext);
    const descriptionId = React.useId();
    const currentItem = sortersList.find((item) => item.id === activeSort.sortBy);
    const currentDirection =
        activeSort.sortOrder === "DESC" ? t("common.sorter.descending") : t("common.sorter.ascending");
    const togglerText = currentItem
        ? t("common.sorter.optionsWithState", { field: currentItem.label, direction: currentDirection })
        : t("common.words.sortOptions");

    return (
        <div className={"sortButton"} data-test-id={"sortButton"}>
            <ContextMenu togglerElement="list-sort" togglerText={togglerText}>
                {sortersList.map((item, index) => {
                    const isCurrent = activeSort.sortBy === item.id;
                    const nextOrder = isCurrent
                        ? activeSort.sortOrder === "ASC"
                            ? "DESC"
                            : "ASC"
                        : activeSort.sortOrder || "ASC";
                    const nextDirection =
                        nextOrder === "ASC" ? t("common.sorter.ascending") : t("common.sorter.descending");
                    const description = isCurrent
                        ? t("common.sorter.currentAndAction", { direction: currentDirection, nextDirection })
                        : t("common.sorter.action", { field: item.label, direction: nextDirection });
                    const itemDescriptionId = `${descriptionId}-${index}`;
                    return (
                        <MenuItem
                            data-test-id={`sort-option-${item.id || "default"}-${activeSort.sortOrder}`}
                            active={isCurrent}
                            aria-describedby={itemDescriptionId}
                            key={item.id}
                            text={
                                <>
                                    {item.label}
                                    <span id={itemDescriptionId} hidden>
                                        {description}
                                    </span>
                                </>
                            }
                            icon={
                                activeSort.sortBy && activeSort.sortBy === item.id
                                    ? activeSort.sortOrder === "ASC"
                                        ? "list-sortasc"
                                        : "list-sortdesc"
                                    : undefined
                            }
                            onClick={() => updateGlobalTableSettings({ sortBy: item.id, sortOrder: nextOrder })}
                        />
                    );
                })}
            </ContextMenu>
        </div>
    );
}
