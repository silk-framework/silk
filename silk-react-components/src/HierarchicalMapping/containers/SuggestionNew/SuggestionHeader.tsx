import React from 'react';
import {
    TableToolbar,
    TableToolbarAction,
    TableToolbarContent,
    TableToolbarMenu,
    TableToolbarSearch
} from 'carbon-components-react';

export default function SuggestionHeader() {
    return (
        <TableToolbar>
            <TableToolbarContent>
                <TableToolbarSearch
                    tabIndex={0}
                    onChange={() => {}}
                />
                <TableToolbarMenu tabIndex={0}>
                    <TableToolbarAction primaryFocus onClick={() => {}}>
                        Set prefix for autosuggestion
                    </TableToolbarAction>
                    <TableToolbarAction onClick={() => {}}>
                        Recalculate matches
                    </TableToolbarAction>
                </TableToolbarMenu>
            </TableToolbarContent>
        </TableToolbar>

    )
}
