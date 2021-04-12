import {ContextMenu, MenuItem} from "@gui-elements/index";
import React, {useContext} from "react";
import {IColumnFilters} from "../suggestion.typings";
import {SuggestionListContext} from "../SuggestionContainer";
import {TestableComponent} from "@gui-elements/src/components/interfaces";

interface IProps extends TestableComponent {
    // column filters array
    filters: IColumnFilters[];

    // callback
    onApplyFilter(filter: string);

    // selected filter name
    selectedFilter: string;
}


export default function ColumnFilter({ filters, onApplyFilter, selectedFilter, ...restProps }: IProps) {
    const context = useContext(SuggestionListContext);

    const handleApplyFilter = (filter: string) => {
        onApplyFilter(filter);
    }
    return <ContextMenu
        portalContainer={context.portalContainer}
        togglerElement={selectedFilter ? "operation-filteredit" : "operation-filter"}
        {...restProps}
    >
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
