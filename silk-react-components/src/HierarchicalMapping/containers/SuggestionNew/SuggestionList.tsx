import React, { useEffect, useState } from 'react';
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
import { Button, Pagination } from '@gui-elements/index';

interface IProps {
    headers: any;
    rows: any;
    onSwapAction: any
}
export default function SuggestionList({headers, rows, onSwapAction}: IProps) {
    const [selectedTypes, setSelectedTypes] = useState({});

    const [pageRows, setPageRows] = useState({});

    const [pagination, setPagination] = useState({
        page: 1,
        pageSize: 5
    });

    const [selectedRows, setSelectedRows] = useState({});

    useEffect(() => {
        Object.keys(pageRows).forEach(sourceKey => {
            handleSelectTarget(sourceKey, pageRows[sourceKey].type);
        });
        updateCurrentRows(pagination);

    }, [rows, pagination]);

    const isAllSelected = () => Object.keys(pageRows).length === Object.keys(selectedRows).length;

    const updateCurrentRows = (pagination) => {
        const { page, pageSize } = pagination;
        setPageRows({});
        const rowSourceKeys = Object.keys(rows);

        const startIndex = (page - 1) * pageSize;

        const showRows = rowSourceKeys.slice(startIndex, startIndex + pageSize);
        console.log('showRows', showRows, rowSourceKeys);
        showRows.forEach(rowKey => {
            setPageRows(prevState => ({
                ...prevState,
                [rowKey]: rows[rowKey]
            }))
        });
    };

    const handleSelectRow = (sourceKey: string) => {
        setSelectedRows(prevState => ({
            ...prevState,
            [sourceKey]: !selectedRows[sourceKey]
        }))
    };

    const toggleSelectAll = () => {
        if (isAllSelected()) {
            setSelectedRows({});
        } else {
            // @NOTE: current page rows
            Object.keys(rows).forEach(handleSelectRow)
        }
    };

    const handlePageChange = (pagination) => {
        setPagination(pagination);
        updateCurrentRows(pagination)
    };

    const handleSelectTarget = (sourceKey: string, selectType?: string) => {
        if (!selectType) {
            selectType = rows[sourceKey].type;
        }
        const {uri} = rows[sourceKey];
        setSelectedTypes(prevState => ({
            ...prevState,
            [uri]: selectType
        }))
    };

    return <>
        <Table>
            <TableHead>
                <TableRow>
                    <TableSelectAll
                        id={'select-all'}
                        name={'select-all'}
                        onSelect={toggleSelectAll}
                        checked={isAllSelected()}
                    />
                    {headers.map(header => (
                        <TableHeader>
                            {header.key === 'swapAction' ? <Button onClick={onSwapAction}>Swap</Button> : header.header}
                        </TableHeader>
                    ))}
                </TableRow>
            </TableHead>
            <TableBody>
                {
                    Object.keys(pageRows).map(sourceKey =>
                        <TableRow key={sourceKey}>
                            <TableSelectRow
                                name={sourceKey}
                                id={sourceKey}
                                onSelect={() => handleSelectRow(sourceKey)}
                                checked={selectedRows[sourceKey]}
                            />
                            <TableCell>
                                <select>
                                    <option value={sourceKey}>{sourceKey}</option>
                                </select>
                            </TableCell>
                            <TableCell>
                                <div/>
                            </TableCell>
                            <TableCell>
                                <select>
                                    {rows[sourceKey].map(target => <option value={target.uri} onClick={() => handleSelectTarget(sourceKey)}>{target.uri}</option>)}
                                </select>
                            </TableCell>
                            <TableCell>
                                <select value={selectedTypes[rows[sourceKey].uri]}>
                                    <option value="object" onClick={() => handleSelectTarget(sourceKey, 'object')}>object</option>
                                    <option value="value" onClick={() => handleSelectTarget(sourceKey, 'value')}>value</option>
                                </select>
                            </TableCell>
                        </TableRow>
                    )
                }
            </TableBody>
        </Table>
        <Pagination
            onChange={handlePageChange}
            totalItems={Object.keys(rows).length}
            pageSizes={[5, 10, 25, 50, 100]}
            page={pagination.page}
            pageSize={pagination.pageSize}
            backwardText={"Previous page"}
            forwardText={"Next page"}
            itemsPerPageText={"Items per page:"}
            itemRangeText={(min, max, total) => `${min}–${max} of ${total} items`}
            pageRangeText={(current, total) => `of ${total} pages`}
        />
    </>
}
