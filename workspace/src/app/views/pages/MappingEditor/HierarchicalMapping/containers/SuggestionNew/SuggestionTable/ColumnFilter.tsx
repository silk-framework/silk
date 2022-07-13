import {ContextMenu, MenuItem} from "@eccenca/gui-elements";
import React, {useContext} from "react";
import {IColumnFilters} from "../suggestion.typings";
import {SuggestionListContext} from "../SuggestionContainer";
import {TestableComponent} from "@eccenca/gui-elements/src/components/interfaces";

interface IProps extends TestableComponent {
    // column filters array
    filters: IColumnFilters[];

    // callback
    onApplyFilter(filter: string);

    // selected filter name
    selectedFilter: string;

    // True if the suggestion is in from-dataset view, else it is in from-vocabulary view
    fromDataset: boolean
}


export default function ColumnFilter({ filters, onApplyFilter, selectedFilter, fromDataset, ...restProps }: IProps) {
    const context = useContext(SuggestionListContext);

    const handleApplyFilter = (filter: string) => {
        onApplyFilter(filter);
    }
    const selectableFilters = filters.filter(f => f.selectable === "always" ||
        (f.selectable === "sourceViewOnly" && fromDataset) ||
        (f.selectable === "vocabularyViewOnly" && !fromDataset))
    return selectableFilters.length > 0 ? <ContextMenu
        contextOverlayProps={{
            portalContainer: context.portalContainer,
        }}
        togglerElement={selectedFilter ? "operation-filteredit" : "operation-filter"}
        {...restProps}
    >
        {
                selectableFilters.map(filter => <MenuItem
                    key={filter.action}
                    text={filter.label}
                    onClick={() => handleApplyFilter(filter.action)}
                    active={selectedFilter === filter.action}/>
                )
        }
    </ContextMenu>
        : null
}
