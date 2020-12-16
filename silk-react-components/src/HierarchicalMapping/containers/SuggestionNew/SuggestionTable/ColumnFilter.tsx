import { ContextMenu, MenuItem } from "@gui-elements/index";
import React, { useContext, useState } from "react";
import { IColumnFilters } from "../suggestion.typings";
import { SuggestionListContext } from "../SuggestionContainer";

interface IProps {
    // column filters array
    filters: IColumnFilters[];

    // callback
    onApplyFilter(filter: string);

    selectedFilter: string;
}


export default function ColumnFilter({ filters, onApplyFilter, selectedFilter }: IProps) {
    const context = useContext(SuggestionListContext);

    const handleApplyFilter = (filter: string) => {
        onApplyFilter(filter);
    }
    return <ContextMenu portalContainer={context.portalContainer}>
        {
            filters.map(filter => <MenuItem
                key={filter.action}
                text={filter.label}
                onClick={() => handleApplyFilter(filter.action)}
                active={selectedFilter === filter.action}/>
            )
        }
    </ContextMenu>
}
