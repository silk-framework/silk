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

interface IProps {
    headers: any[];

    rows: ITransformedSuggestion[];

    onSwapAction();

    onAskDiscardChanges();

    onAdd(selected: ITransformedSuggestion[]);
}

export default function SuggestionList({headers, rows, onSwapAction, onAskDiscardChanges, onAdd}: IProps) {
    const [pageRows, setPageRows] = useState<ITransformedSuggestion[]>([]);

    const [pagination, setPagination] = useState({
        page: 1,
        pageSize: 5
    });

    const [selectedRows, setSelectedRows] = useState<ITransformedSuggestion[]>([]);

    const [sortDirections, setSortDirections] = useState<{ [key: string]: 'asc' | 'desc' }>({});

    useEffect(() => {
        console.log('Suggestions', rows);
        updateCurrentRows(pagination);
    }, [rows]);

    const isAllSelected = () => pageRows.length === selectedRows.length;

    const updateCurrentRows = (pagination, updatedRows?: ITransformedSuggestion[]) => {
        const {page, pageSize} = pagination;
        const startIndex = (page - 1) * pageSize;

        const slicedRows = (updatedRows || rows).slice(startIndex, startIndex + pageSize);

        setPageRows(slicedRows);

        console.log('sliced rows', slicedRows);
    };

    const selectRow = ({source, target}: ITransformedSuggestion) => {
        const selectedRow = selectedRows.find(row => row.source === source);
        if (selectedRow) {
            setSelectedRows(selectedRows.filter(row => row.source !== source));
        } else {
            const defaultTarget = target[0];
            setSelectedRows([
                ...selectedRows,
                {
                    source,
                    target: [defaultTarget]
                }
            ]);
        }
    };

    const updateSelectedRows = (source: string, updateData: Partial<ISuggestionTarget>) => {
        const ind = selectedRows.findIndex(r => r.source === source);
        if (ind > -1) {
            const arr = [...selectedRows];
            arr[ind] = {
                ...selectedRows[ind],
                ...updateData
            };
            setSelectedRows(arr);
        }
    };

    const toggleSelectAll = () => {
        setSelectedRows([]);
        if (!isAllSelected()) {
            pageRows.forEach(selectRow);
        }
    };

    const handleSelectTarget = (sugIndex: number, selectedTarget: ISuggestionTarget) => {
        const {source} = pageRows[sugIndex];
        const isExists = selectedRows.find(r => r.source === source);

        const updateData = {
            uri: selectedTarget.uri,
        };

        if (isExists) {
            updateSelectedRows(source, updateData)
        } else {
            selectRow({
                source,
                target: [selectedTarget]
            })
        }
    };

    const handleSelectType = (sugIndex: number, selectedType: 'object' | 'value') => {
        // const {source, target} = pageRows[sugIndex];
        //
        // if (!selectedRows[source]) {
        //     const data = target.find(p => p.type === selectedType);
        //     selectRow({
        //         source,
        //         target: [{
        //             ...data,
        //             type: selectedType,
        //         }]
        //     })
        // }
    };

    const handlePageChange = (pagination) => {
        setPagination(pagination);
        updateCurrentRows(pagination)
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

        updateCurrentRows(pagination, sortedArray);
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
                    pageRows.map((row: ITransformedSuggestion, index: number) => {
                        const {source, target} = row;
                        const selected = selectedRows.find(r => r.source === source);
                        return <TableRow key={source}>
                            <TableSelectRow
                                name={source}
                                id={source}
                                onSelect={() => selectRow(row)}
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
                                    onChange={(selected) => handleSelectTarget(index, selected)}
                                />
                            </TableCell>
                            <TableCell>
                                <select>
                                    <option value="object" onClick={() => handleSelectType(index, 'object')}>object
                                    </option>
                                    <option value="value" onClick={() => handleSelectType(index, 'value')}>value
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
