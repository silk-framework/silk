import { TableHead, TableHeader, TableRow, TableSelectAll } from "carbon-components-react";
import { Button, Icon } from "@gui-elements/index";
import React from "react";
import { FILTER_ACTIONS } from "../constants";
import { IColumnFilters, ISortDirection, ITableHeader } from "../suggestion.typings";
import ColumnFilter from "./ColumnFilter";

const columnFilters: { [key: string]: IColumnFilters[] } = {
    checkbox: [{
        label: 'Show only selected items',
        action: FILTER_ACTIONS.SHOW_SELECTED
    }, {
        label: 'Show only unselected items',
        action: FILTER_ACTIONS.SHOW_UNSELECTED
    }],
    target: [{
        label: 'Show only matches',
        action: FILTER_ACTIONS.SHOW_MATCHES
    }, {
        label: 'Show only auto-generated properties',
        action: FILTER_ACTIONS.SHOW_GENERATED
    }],
    type: [{
        label: 'Show only value mappings',
        action: FILTER_ACTIONS.SHOW_VALUE_MAPPINGS
    }, {
        label: 'Show only object mappings',
        action: FILTER_ACTIONS.SHOW_OBJECT_MAPPINGS
    }]
};

interface IProps {
    // table headers
    headers: ITableHeader[];

    // flag for select-all checkbox
    isAllSelected: boolean;

    // callback for select-all checkbox
    toggleSelectAll();

    // callback for swap button
    onSwap();

    // column sorting information
    sortDirections: ISortDirection;

    // callback for column sorting
    onSort(headerKey: string);

    // callback for column filtering
    onApplyFilter(filter: string);

    // React.ref for filter dialog
    portalContainerRef: any;

}

export default function STableHeader({headers, isAllSelected, toggleSelectAll, onSwap, sortDirections, onSort, onApplyFilter, portalContainerRef}: IProps) {
    return <TableHead>
        <TableRow>
            <TableHeader>
                <TableSelectAll
                    id={'select-all'}
                    name={'select-all'}
                    onSelect={toggleSelectAll}
                    checked={isAllSelected}
                />
                <ColumnFilter
                    filters={columnFilters.checkbox}
                    portalContainerRef={portalContainerRef}
                    onApplyFilter={onApplyFilter}
                />

            </TableHeader>

            {headers.map(header => (
                <TableHeader key={header.key}>
                    {
                        header.key === 'SWAP_BUTTON'
                            ? <Button onClick={onSwap}>Swap</Button>
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
                                    columnFilters[header.key] && <ColumnFilter
                                        filters={columnFilters[header.key]}
                                        portalContainerRef={portalContainerRef}
                                        onApplyFilter={onApplyFilter}
                                    />
                                }
                            </>
                    }
                </TableHeader>
            ))}
        </TableRow>
    </TableHead>
}
