import React from 'react';
import {
    TableToolbar,
    TableToolbarAction,
    TableToolbarContent,
    TableToolbarMenu,
    TableToolbarSearch
} from 'carbon-components-react';

export default function SuggestionHeader({getBatchActionProps, onInputChange}) {
    return (
        <TableToolbar>
            <TableToolbarContent>
                <TableToolbarSearch
                    tabIndex={getBatchActionProps().shouldShowBatchActions ? -1 : 0}
                    onChange={onInputChange}
                />
                <TableToolbarMenu
                    tabIndex={getBatchActionProps().shouldShowBatchActions ? -1 : 0}>
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
