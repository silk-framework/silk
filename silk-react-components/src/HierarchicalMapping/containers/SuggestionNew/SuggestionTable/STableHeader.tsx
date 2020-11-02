import { TableHead, TableHeader, TableRow, TableSelectAll } from "carbon-components-react";
import { Button, ContextMenu, Icon, MenuItem } from "@gui-elements/index";
import React, { useState } from "react";
import { FILTER_ACTIONS } from "../constants";
import { IPlainObject } from "../suggestion.typings";

export default function STableHeader({headers, isAllSelected, toggleSelectAll, onSwap, sortDirections, onSort, onApplyFilter, portalContainerRef}) {
    const [selectedFilters, setSelectedFilters] = useState<IPlainObject>({});

    const handleApplyFilter = (column: string, filter: string) => {
        const filters = {...selectedFilters};

        if (filters[column] === filter) {
            // unselect
            delete filters[column];
        } else {
            filters[column] = filter;
        }
        setSelectedFilters(filters);

        onApplyFilter(filter);
    };

    return <TableHead>
        <TableRow>
            <TableHeader>
                <TableSelectAll
                    id={'select-all'}
                    name={'select-all'}
                    onSelect={toggleSelectAll}
                    checked={isAllSelected}
                />
                <ContextMenu portalContainer={portalContainerRef?.current}>
                    <MenuItem
                        text={'Show only selected items'}
                        onClick={() => handleApplyFilter('checkbox', FILTER_ACTIONS.SHOW_SELECTED)}
                        active={selectedFilters['checkbox'] === FILTER_ACTIONS.SHOW_SELECTED}
                    />
                    <MenuItem
                        text={'Show only unselected items'}
                        onClick={() => handleApplyFilter('checkbox', FILTER_ACTIONS.SHOW_UNSELECTED)}
                        active={selectedFilters['checkbox'] === FILTER_ACTIONS.SHOW_UNSELECTED}
                    />
                </ContextMenu>
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
                                    header.key === 'target' && <ContextMenu portalContainer={portalContainerRef?.current}>
                                        <MenuItem
                                            text={'Show only matches'}
                                            onClick={() => handleApplyFilter('target', FILTER_ACTIONS.SHOW_MATCHES)}
                                            active={selectedFilters['target'] === FILTER_ACTIONS.SHOW_MATCHES}
                                        />
                                        <MenuItem
                                            text={'Show only auto-generated properties'}
                                            onClick={() => handleApplyFilter('target', FILTER_ACTIONS.SHOW_GENERATED)}
                                            active={selectedFilters['target'] === FILTER_ACTIONS.SHOW_GENERATED}
                                        />
                                    </ContextMenu>
                                }
                                {
                                    header.key === 'type' && <ContextMenu portalContainer={portalContainerRef?.current}>
                                        <MenuItem
                                            text={'Show only value mappings'}
                                            onClick={() => handleApplyFilter('type', FILTER_ACTIONS.SHOW_VALUE_MAPPINGS)}
                                            active={selectedFilters['type'] === FILTER_ACTIONS.SHOW_VALUE_MAPPINGS}
                                        />
                                        <MenuItem
                                            text={'Show only object mappings'}
                                            onClick={() => handleApplyFilter('type', FILTER_ACTIONS.SHOW_OBJECT_MAPPINGS)}
                                            active={selectedFilters['type'] === FILTER_ACTIONS.SHOW_OBJECT_MAPPINGS}
                                        />
                                    </ContextMenu>
                                }
                            </>
                    }
                </TableHeader>
            ))}
        </TableRow>
    </TableHead>
}
