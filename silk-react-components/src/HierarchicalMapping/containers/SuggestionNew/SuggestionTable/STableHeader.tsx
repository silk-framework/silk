import { Button, Checkbox, Icon, TableHead, TableHeader, TableRow } from "@gui-elements/index";
import React from "react";
import { COLUMN_FILTERS } from "../constants";
import { ISortDirection, ITableHeader } from "../suggestion.typings";
import ColumnFilter from "./ColumnFilter";

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
    onApplyFilter(columnName: string, filter: string);
}

export default function STableHeader({
     headers,
     isAllSelected,
     toggleSelectAll,
     onSwap,
     sortDirections,
     onSort,
     onApplyFilter
}: IProps) {
    return <TableHead>
        <TableRow>
            <TableHeader>
                <Checkbox
                    onChange={toggleSelectAll}
                    checked={isAllSelected}
                />
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
