import { ContextMenu, MenuItem } from "@gui-elements/index";
import React, { useState } from "react";
import { IColumnFilters } from "../suggestion.typings";

interface IProps {
    // column filters array
    filters: IColumnFilters[];

    // portal ref
    portalContainerRef: any;

    // callback
    onApplyFilter(filter: string);
}


export default function ColumnFilter({ filters, portalContainerRef, onApplyFilter }: IProps) {
    const [selectedFilter, setSelectedFilter] = useState<string>('');

    const handleApplyFilter = (filter: string) => {
        setSelectedFilter(filter === selectedFilter ? '' : filter);
        onApplyFilter(filter);
    }

    return <ContextMenu portalContainer={portalContainerRef?.current}>
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
