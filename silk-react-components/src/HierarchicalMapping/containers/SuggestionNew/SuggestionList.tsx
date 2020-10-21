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
import { Button, Icon, Pagination } from '@gui-elements/index';
import { ISuggestionTarget, ITransformedSuggestion } from "./suggestion.typings";
import TargetList from "./TargetList";
import _ from 'lodash';

export interface ISelectedSuggestion {
    source: string;

    targetUri: string;

    type: string;
}

interface ITargetWithSelected extends ISuggestionTarget {
    _selected: boolean;
}

interface IPageSuggestion extends ITransformedSuggestion {
    target: ITargetWithSelected[]
}

interface IProps {
    headers: any[];

    rows: ITransformedSuggestion[];

    onSwapAction();

    onAskDiscardChanges();

    onAdd(selected: ISelectedSuggestion[]);
}

export default function SuggestionList({headers, rows, onSwapAction, onAskDiscardChanges, onAdd}: IProps) {
    const [pageRows, setPageRows] = useState<IPageSuggestion[]>([]);

    const [pagination, setPagination] = useState({
        page: 1,
        pageSize: 5
    });

    const [selectedRows, setSelectedRows] = useState<ISelectedSuggestion[]>([]);

    const [sortDirections, setSortDirections] = useState<{ [key: string]: 'asc' | 'desc' }>({});

    useEffect(() => {
        const arr = [];

        rows.forEach(({source, target}) => {
            arr.push({
                source,
                target: target.map((t, index) => ({
                    ...t,
                    _selected: index === 0
                }))
            });
        });

        const slicedRows = paginate(arr, pagination);

        setPageRows(slicedRows);

        console.log('Suggestions', rows);
        console.log('sliced rows', arr);
    }, [rows]);

    const isAllSelected = () => pageRows.length && pageRows.length === selectedRows.length;

    const paginate = (arr, pagination) => {
        const {page, pageSize} = pagination;
        const startIndex = (page - 1) * pageSize;

        return arr.slice(startIndex, startIndex + pageSize);
    }

    const toggleRowSelect = ({source, target}: IPageSuggestion) => {
        const selectedRow = selectedRows.find(row => row.source === source);
        if (selectedRow) {
            setSelectedRows(
                selectedRows.filter(row => row.source !== source)
            );
        } else {
            const selectedTarget = target.find(t => t._selected);
            setSelectedRows([
                ...selectedRows,
                {
                    source,
                    targetUri: selectedTarget.uri,
                    type: selectedTarget.type
                }
            ]);
        }
    };

    const toggleSelectAll = () => {
        if (isAllSelected()) {
            setSelectedRows([]);
        } else if (!selectedRows.length) {
            pageRows.forEach(toggleRowSelect);
        } else {
            setSelectedRows([]);
            pageRows.forEach(toggleRowSelect);
        }
    };

    const handleSelectTarget = (i: number, updatedTargets: ITargetWithSelected[]) => {
        const _pageRows = [...pageRows]

        _pageRows[i].target = updatedTargets;

        setPageRows(_pageRows);
    };

    const handlePageChange = (pagination) => {
        setPagination(pagination);
        setPageRows(
            paginate(rows, pagination)
        );
    };

    const handleSwap = () => {
        setPageRows([]);
        onSwapAction();
    };

    const handleCancel = () => onAskDiscardChanges();

    const handleAdd = () => onAdd(selectedRows);

    const handleSort = (headerKey: string) => {
        const isAsc = sortDirections[headerKey] === 'asc';

        const direction = isAsc ? 'desc' : 'asc'
        setSortDirections({
            [headerKey]: direction
        });

        const sortedArray = _.orderBy(rows, headerKey, direction);
        setPageRows(
            paginate(sortedArray, pagination)
        );
    };

    const handleFilterDialog = () => {
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
                            {
                                header.key === 'swapAction'
                                    ? <Button onClick={handleSwap}>Swap</Button>
                                    : <>
                                        {header.header}
                                        <Icon
                                            small
                                            name={
                                                !sortDirections[header.key]
                                                    ? 'list-sort'
                                                    : sortDirections[header.key] === 'asc' ? 'list-sortasc' : 'list-sortdesc'
                                            }
                                            onClick={() => handleSort(header.key)}
                                        />
                                        <Icon small name={'item-moremenu'} onClick={handleFilterDialog}/>
                                    </>
                            }
                        </TableHeader>
                    ))}
                </TableRow>
            </TableHead>
            <TableBody>
                {
                    pageRows.map((row: IPageSuggestion, index: number) => {
                        const {source, target} = row;
                        const selected = selectedRows.find(r => r.source === source);
                        return <TableRow key={source}>
                            <TableSelectRow
                                name={source}
                                id={source}
                                onSelect={() => toggleRowSelect(row)}
                                checked={!!selected}
                                ariaLabel={'select row'}
                            />
                            <TableCell>
                                <select>
                                    <option value={source}>{source}</option>
                                </select>
                            </TableCell>
                            <TableCell>
                                <div/>
                            </TableCell>
                            <TableCell>
                                <TargetList
                                    targets={target}
                                    onChange={(updatedTargets) => handleSelectTarget(index, updatedTargets)}
                                />
                            </TableCell>
                            <TableCell>
                                <select>
                                    <option value="object" onClick={() => {}}>object
                                    </option>
                                    <option value="value" onClick={() => {}}>value
                                    </option>
                                </select>
                            </TableCell>
                        </TableRow>
                    })
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
        <Button affirmative={true} onClick={handleAdd}>Add ({selectedRows.length})</Button>
        <Button disruptive={true} onClick={handleCancel}>Cancel</Button>
    </>
}
