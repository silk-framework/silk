import React from "react";
import { IAppliedSorterState, ISorterListItemState, SortModifierType } from "@ducks/workspace/typings";

import { ContextMenu, MenuItem } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { GlobalTableContext } from "../../../GlobalContextsWrapper";

interface IProps {
    sortersList: ISorterListItemState[];
    activeSort: IAppliedSorterState;
}

const directions: { order: Exclude<SortModifierType, "">; icon: string; labelKey: string; fallback: string }[] = [
    { order: "ASC", icon: "list-sortasc", labelKey: "common.words.ascending", fallback: "ascending" },
    { order: "DESC", icon: "list-sortdesc", labelKey: "common.words.descending", fallback: "descending" },
];

export default function SortButton({ sortersList, activeSort }: IProps) {
    const [t] = useTranslation();
    const { updateGlobalTableSettings } = React.useContext(GlobalTableContext);

    const applySort = React.useCallback(
        (sortBy: string, sortOrder: SortModifierType) => {
            updateGlobalTableSettings({ sortBy, sortOrder });
        },
        [updateGlobalTableSettings],
    );

    return (
        <div className={"sortButton"} data-test-id={"sortButton"}>
            <ContextMenu togglerElement="list-sort" togglerText={t("common.words.sortOptions", "Sort options")}>
                {/* Each sorter is offered as two explicit, icon-labelled entries (ascending / descending)
                    so the effect of a click is unambiguous instead of toggling a hidden direction. */}
                {sortersList.flatMap((item) =>
                    directions.map((dir) => (
                        <MenuItem
                            key={`${item.id}-${dir.order}`}
                            data-test-id={`sort-option-${item.id || "default"}-${dir.order}`}
                            active={activeSort.sortBy === item.id && activeSort.sortOrder === dir.order}
                            text={`${item.label} (${t(dir.labelKey, dir.fallback)})`}
                            icon={[dir.icon]}
                            onClick={() => applySort(item.id, dir.order)}
                        />
                    )),
                )}
            </ContextMenu>
        </div>
    );
}
