import React from 'react';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    TableSelectAll,
    TableSelectRow,
} from 'carbon-components-react';
import { Button } from '@gui-elements/index';

export default function SuggestionList({ headers, rows, getSelectionProps, getRowProps, getHeaderProps, onSwapAction}) {
    return (
        <Table>
            <TableHead>
                <TableRow>
                    <TableSelectAll {...getSelectionProps()} />
                    {headers.map(header => (
                        <TableHeader {...getHeaderProps({header})}>
                            {header.key === 'swapAction' ? <Button onClick={onSwapAction}>Swap</Button> : header.header }
                        </TableHeader>
                    ))}
                </TableRow>
            </TableHead>
            <TableBody>
                {rows.map(row => (
                    <TableRow {...getRowProps({row})}>
                        <TableSelectRow {...getSelectionProps({row})} />
                        {row.cells.map(cell => (
                            <TableCell key={cell.id}>
                                {cell.value === null
                                    ? null
                                    : <div>
                                        <select><option value={cell.value}>{cell.value}</option></select>
                                    </div>
                                }
                            </TableCell>
                        ))}
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    )
}
