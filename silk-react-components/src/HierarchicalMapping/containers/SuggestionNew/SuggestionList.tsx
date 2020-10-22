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
import { ISuggestionTarget, ITransformedSuggestion, SuggestionTypeValues } from "./suggestion.typings";
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
    const [allRows, setAllRows] = useState<IPageSuggestion[]>([]);

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

        setAllRows(arr);

        setPageRows(
            paginate(arr, pagination)
        );

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
            setSelectedRows(prevState => ([
                ...prevState,
                {
                    source,
                    targetUri: selectedTarget.uri,
                    type: selectedTarget.type
                }
            ]));
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
        const _pageRows = [...pageRows];

        _pageRows[i].target = [...updatedTargets];

        setPageRows(_pageRows);
    };

    const handlePageChange = (pagination) => {
        setPagination(pagination);
        setPageRows(
            paginate(allRows, pagination)
        );
    };

    const handleSwap = () => {
        setPageRows([]);

        setSelectedRows([]);

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

        const sortedArray = _.orderBy(allRows, headerKey, direction);
        setPageRows(
            paginate(sortedArray, pagination)
        );
    };

    const handleFilterDialog = () => {
    };

    const getType = (sourceIndex): SuggestionTypeValues => {
        const row = pageRows[sourceIndex];
        if (row) {
            const selectedTarget = row.target.find(t => t._selected);
            return selectedTarget.type;
        }
    };

    const handleTypeChange = (value: SuggestionTypeValues) => {

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
                                <select onChange={e => handleTypeChange(e.target.value as SuggestionTypeValues)}>
                                    <option selected={getType(index) === 'object'} value="object">object
                                    </option>
                                    <option selected={getType(index) === 'value'} value="value">value
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
            totalItems={allRows.length}
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
