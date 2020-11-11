import React from 'react';
import {
    TableToolbar,
    TableToolbarAction,
    TableToolbarContent,
    TableToolbarMenu,
    TableToolbarSearch
} from 'carbon-components-react';

export default function SuggestionHeader({ onSearch }) {
    return (
        <TableToolbar>
            <TableToolbarContent>
                <TableToolbarSearch
                    tabIndex={0}
                    onChange={e => onSearch(e.target.value)}
                />
                <TableToolbarMenu tabIndex={1}>
                    <TableToolbarAction primaryFocus onClick={() => {}}>
                        Set prefix for autosuggestion
                    </TableToolbarAction>
                </TableToolbarMenu>
            </TableToolbarContent>
        </TableToolbar>

    )
}
