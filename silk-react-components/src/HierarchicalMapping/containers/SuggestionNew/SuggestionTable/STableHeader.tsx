import { Button, Checkbox, ContextMenu, Icon, MenuItem, TableHead, TableHeader, TableRow } from "@gui-elements/index";
import React, { useContext, useState } from "react";
import { COLUMN_FILTERS } from "../constants";
import { ISortDirection, ITableHeader } from "../suggestion.typings";
import ColumnFilter from "./ColumnFilter";
import { SuggestionListContext } from "../SuggestionContainer";

interface IProps {
    // table headers
    headers: ITableHeader[];

    // flag for select-all checkbox
    isAllSelected: boolean;

    // callback for select-all checkbox
    toggleSelectAll(scope: 'all' | 'page', action: 'select' | 'unselect');

    // callback for swap button
    onSwap();

    // column sorting information
    sortDirections: ISortDirection;

    // callback for column sorting
    onSort(headerKey: string);

    // callback for column filtering
    onApplyFilter(columnName: string, filter: string);
}

const sortItems = [{
    text: 'Select all page items',
    value: 'page_select',
},{
    text: 'Select all items',
    value: 'all_select',
},{
    text: 'Unselect all page items',
    value: 'page_unselect',
},{
    text: 'Unselect all items',
    value: 'all_unselect',
}];
export default function STableHeader({
     headers,
     toggleSelectAll,
     onSwap,
     sortDirections,
     onSort,
     onApplyFilter
 }: IProps) {
    const [selectSorting, setSelectedSorting] = useState('');

    const context = useContext(SuggestionListContext);
    const {portalContainer} = context;

    const handleSort = (value: string) => {
        setSelectedSorting(value);
        const [scope, action] = value.split('_');
        toggleSelectAll(scope as 'all' | 'page', action as 'select' | 'unselect');
    }

    return <TableHead>
        <TableRow>
            <TableHeader>
                <ContextMenu
                    portalContainer={portalContainer}
                    togglerElement={'item-info'}
                >
                    {
                        sortItems.map(o => (
                            <MenuItem
                                key={o.value}
                                active={o.value === selectSorting}
                                text={o.text}
                                onClick={() => handleSort(o.value)}
                            />
                        ))
                    }
                </ContextMenu>
                <ColumnFilter
                    filters={COLUMN_FILTERS.checkbox}
                    onApplyFilter={(filter) => onApplyFilter('checkbox', filter)}
                />
            </TableHeader>

            {headers.map(header => (
                <TableHeader key={header.key}>
                    {
                        header.key === 'SWAP_BUTTON'
                            ? <Button onClick={onSwap} data-test-id={header.key}>Swap</Button>
                            : <>
                                {header.header}
                                <Icon
                                    small
                                    name={
                                        sortDirections.column !== header.key
                                            ? 'list-sort'
                                            : sortDirections.modifier === 'asc' ? 'list-sortasc' : 'list-sortdesc'
                                    }
                                    onClick={() => onSort(header.key)}
                                />
                                {
                                    COLUMN_FILTERS[header.key] && <ColumnFilter
                                        filters={COLUMN_FILTERS[header.key]}
                                        onApplyFilter={(filter) => onApplyFilter(header.key, filter)}
                                    />
                                }
                            </>
                    }
                </TableHeader>
            ))}
        </TableRow>
    </TableHead>
}
