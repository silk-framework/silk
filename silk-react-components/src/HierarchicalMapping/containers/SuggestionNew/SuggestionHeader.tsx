import React from 'react';

// Todo: use our gui elements wrappers instead of native carbon elements

import {
    TableToolbar,
    TableToolbarAction,
    TableToolbarContent,
    TableToolbarMenu,
    TableToolbarSearch
} from 'carbon-components-react/lib/components/DataTable';

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
