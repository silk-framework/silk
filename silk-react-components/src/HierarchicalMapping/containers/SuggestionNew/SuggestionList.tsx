import React, { useEffect, useState } from 'react';
import { Table } from 'carbon-components-react';
import { Button, Pagination } from '@gui-elements/index';
import { IPlainObject, ISuggestionTarget, ITransformedSuggestion, SuggestionTypeValues } from "./suggestion.typings";
import _ from 'lodash';
import paginate from "../../utils/paginate";
import STableBody from "./SuggestionTable/STableBody";
import STableHeader from "./SuggestionTable/STableHeader";
import { FILTER_ACTIONS } from "./constants";

interface IPagination {
    // store current page number
    page: number;
    // store page size
    pageSize: number;
}

// the object which pass parent for adding new suggestion
export interface IAddedSuggestion {
    // selected source
    source: string;

    // selected target uri
    targetUri: string;

    // target type
    type: SuggestionTypeValues;
}

interface ITargetWithSelected extends ISuggestionTarget {
    // indicate selected target
    _selected: boolean;
}

export interface IPageSuggestion extends ITransformedSuggestion {
    // modified target element
    target: ITargetWithSelected[]
}

interface IProps {
    // received native data from backend
    rows: ITransformedSuggestion[];

    // call parent action during column (source->target) swap
    onSwapAction();

    // call parent discard(cancel) action
    onAskDiscardChanges();

    // call parent add action
    onAdd(selected: IAddedSuggestion[]);
}

export default function SuggestionList({rows, onSwapAction, onAskDiscardChanges, onAdd}: IProps) {
    const [headers, setHeaders] = useState(
        [
            {header: 'Source data', key: 'source'},
            {header: null, key: 'SWAP_BUTTON'},
            {header: 'Target data', key: 'target',},
            {header: 'Mapping type', key: 'type'}
        ]
    );

    // store all result, because  of pagination  implemented in client side
    const [allRows, setAllRows] = useState<IPageSuggestion[]>([]);

    const [pageRows, setPageRows] = useState<IPageSuggestion[]>([]);

    // pagination info
    const [pagination, setPagination] = useState<IPagination>({
        page: 1,
        pageSize: 5
    });

    // stored selected source labels or uris
    const [selectedSources, setSelectedSources] = useState<string[]>([]);

    // store hashmap for source->target, invert values on header swap action
    const [sourceToTargetMap, setSourceToTargetMap] = useState<IPlainObject>({});

    // store hashmap for target->type, replace target with source on header swap action
    const [targetToTypeMap, setTargetToTypeMap] = useState<any>({});

    // keep sort directions for columns
    const [sortDirections, setSortDirections] = useState<{
        column: string;
        modifier: 'asc' | 'desc' | ''
    }>({
        column: '',
        modifier: ''
    });

    useEffect(() => {
        const arr = [];

        rows.forEach((row) => {
            const {source, target} = row;

            // add _selected field for each target
            const modifiedTarget = target.map(targetItem => ({
                ...targetItem,
                _selected: false
            }));

            // store modified source,target
            const modifiedRow: IPageSuggestion = {
                source,
                target: modifiedTarget
            };

            // keep changes for selected items only after swap action
            if (selectedSources.includes(source)) {
                modifiedRow.target = modifiedRow.target.map(targetItem => {
                    const {uri, type, confidence} = targetItem;
                    return {
                        uri,
                        confidence,
                        type: targetToTypeMap[uri] || type,
                        _selected: sourceToTargetMap[source] === uri
                    }
                })
            }
            // in case nothing selected, then select first item
            const someSelected = modifiedRow.target.some(t => t._selected);
            if (!someSelected) {
                modifiedRow.target[0]._selected = true;
            }

            arr.push(modifiedRow);
        });

        setAllRows(arr);

        setPageRows(
            paginate(arr, pagination)
        );

        console.log('Suggestions', rows);
    }, [rows]);

    const toggleRowSelect = ({source}: IPageSuggestion) => {
        const selectedRow = selectedSources.find(selected => selected === source);
        if (selectedRow) {
            setSelectedSources(
                selectedSources.filter(selected => selected !== source)
            );
        } else {
            setSelectedSources(prevState => ([
                ...prevState,
                source,
            ]));
        }
    };

    const toggleSelectAll = () => {
        if (isAllSelected()) {
            setSelectedSources([]);
        } else if (!selectedSources.length) {
            pageRows.forEach(toggleRowSelect);
        } else {
            setSelectedSources([]);
            pageRows.forEach(toggleRowSelect);
        }
    };

    const handlePageChange = (pagination: IPagination) => {
        setPagination(pagination);
    };

    const handleCancel = () => onAskDiscardChanges();

    const handleAdd = () => {
        const addedRows = selectedSources.map(source => {
            const found = allRows.find(row => row.source === source);
            if (found) {
                const target = found.target.find(t => t._selected);
                return {
                    source,
                    targetUri: target.uri,
                    type: target.type
                }
            }
        });

        onAdd(addedRows);
    }

    const handleSort = (headerKey: string) => {
        const isAsc = sortDirections.modifier === 'asc';

        const direction = isAsc ? 'desc' : 'asc'
        setSortDirections({
            column: headerKey,
            modifier: direction
        });

        const sortedArray = _.orderBy(allRows, headerKey, direction);

        setPageRows(
            paginate(sortedArray, pagination)
        );
    };

    const handleFilterColumn = (action: keyof typeof FILTER_ACTIONS) => {
        switch (action) {
            case FILTER_ACTIONS.SHOW_SELECTED:
            case FILTER_ACTIONS.SHOW_UNSELECTED: {
                const arr = allRows.filter(
                    row => action === FILTER_ACTIONS.SHOW_SELECTED
                        ? selectedSources.includes(row.source)
                        : !selectedSources.includes(row.source)
                );
                setPageRows(
                    paginate(arr, pagination)
                );
                break;
            }

            case FILTER_ACTIONS.SHOW_VALUE_MAPPINGS:
            case FILTER_ACTIONS.SHOW_OBJECT_MAPPINGS: {
                const type = action === FILTER_ACTIONS.SHOW_VALUE_MAPPINGS ? 'value' : 'object';
                const arr = allRows.filter(row => row.target.every(t => t.type === type));
                setPageRows(paginate(arr, pagination));
                break;
            }
        }
    };

    const handleSwap = () => {
        setPageRows([]);

        const targetsAsSelected = selectedSources.map(source => {
            const found = allRows.find(row => row.source === source);
            if (found) {
                const {uri} = found.target.find(t => t._selected);
                return uri;
            }
        });

        setSelectedSources(targetsAsSelected);

        const sourceToType = {};
        const targetToSource = _.invert(sourceToTargetMap);

        _.forEach(targetToTypeMap, (value, key) => {
            const source = targetToSource[key];
            sourceToType[source] = value;
        });

        setSourceToTargetMap(targetToSource);
        setTargetToTypeMap(sourceToType);

        // swap header columns
        const temp = headers[0];
        headers[0] = headers[2];
        headers[2] = temp;

        setHeaders(headers);

        onSwapAction();
    };

    const handleModifyTarget = (row: IPageSuggestion, targets) => {
        const _allRows = [...allRows];
        const ind = _allRows.findIndex(r => r.source === row.source);

        if (ind > -1) {
            const {uri, type} = targets.find(t => t._selected);

            setSourceToTargetMap(prevState => ({
                ...prevState,
                [row.source]: uri
            }));

            setTargetToTypeMap(prevState => ({
                ...prevState,
                [uri]: type
            }));

            _allRows[ind].target = targets;

            setAllRows(_allRows);
        }
    }

    const isAllSelected = () => allRows.length && pageRows.length === selectedSources.length;

    return <>
        <Table>
            <STableHeader
                headers={headers}
                isAllSelected={isAllSelected()}
                toggleSelectAll={toggleSelectAll}
                onSwap={handleSwap}
                onSort={handleSort}
                onApplyFilter={handleFilterColumn}
                sortDirections={sortDirections}
            />
            <STableBody
                pageRows={pageRows}
                selectedSources={selectedSources}
                toggleRowSelect={toggleRowSelect}
                onModifyTarget={handleModifyTarget}
            />
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
        <Button affirmative={true} onClick={handleAdd}>Add ({selectedSources.length})</Button>
        <Button disruptive={true} onClick={handleCancel}>Cancel</Button>
    </>
}
