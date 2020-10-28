import { TableHead, TableHeader, TableRow, TableSelectAll } from "carbon-components-react";
import { Button, Icon, ContextMenu, MenuItem } from "@gui-elements/index";
import React from "react";
import { FILTER_ACTIONS } from "../constants";

export default function STableHeader({headers, isAllSelected, toggleSelectAll, onSwap, sortDirections, onSort, onApplyFilter}) {
    // @TODO FIND BETTER REPLACEMENT, useRef can be a option
    const portalContainer = document.querySelector('.mdl-layout__inner-container');

    return <TableHead>
        <TableRow>
            <TableHeader>
                <TableSelectAll
                    id={'select-all'}
                    name={'select-all'}
                    onSelect={toggleSelectAll}
                    checked={isAllSelected}
                />
                <ContextMenu portalContainer={portalContainer}>
                    <MenuItem
                        text={'Show only selected items'}
                        onClick={() => onApplyFilter(FILTER_ACTIONS.SHOW_SELECTED)}
                    />
                    <MenuItem
                        text={'Show only unselected items'}
                        onClick={() => onApplyFilter(FILTER_ACTIONS.SHOW_UNSELECTED)}
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
                                    header.key === 'target' && <ContextMenu portalContainer={portalContainer}>
                                        <MenuItem
                                            text={'Show only matches'}
                                            onClick={() => onApplyFilter(FILTER_ACTIONS.SHOW_MATCHES)}
                                        />
                                        <MenuItem
                                            text={'Show only auto-generated properties'}
                                            onClick={() => onApplyFilter(FILTER_ACTIONS.SHOW_GENERATED)}
                                        />
                                    </ContextMenu>
                                }
                                {
                                    header.key === 'type' && <ContextMenu portalContainer={portalContainer}>
                                        <MenuItem
                                            text={'Show only value mappings'}
                                            onClick={() => onApplyFilter(FILTER_ACTIONS.SHOW_VALUE_MAPPINGS)}
                                        />
                                        <MenuItem
                                            text={'Show only object mappings'}
                                            onClick={() => onApplyFilter(FILTER_ACTIONS.SHOW_OBJECT_MAPPINGS)}
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
