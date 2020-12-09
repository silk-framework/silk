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
                    data-test-id={'search_input'}
                    tabIndex={0}
                    onChange={e => onSearch(e.target.value)}
                />
            </TableToolbarContent>
        </TableToolbar>

    )
}
