import React, { createContext, useEffect, useRef, useState } from 'react';
import { Button, Pagination, Table } from '@gui-elements/index';
import {
    IAddedSuggestion,
    IPageSuggestion,
    IPlainObject,
    ISortDirection,
    ITableHeader,
    ITargetWithSelected,
    ITransformedSuggestion
} from "./suggestion.typings";
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

interface ISuggestionListContext {
    portalContainer: HTMLDivElement;
}

export const SuggestionListContext = React.createContext<ISuggestionListContext>({
    portalContainer: null
});

export default function SuggestionList({rows, onSwapAction, onAskDiscardChanges, onAdd}: IProps) {
    const portalContainerRef = useRef();

    const [headers, setHeaders] = useState<ITableHeader[]>(
        [
            {header: 'Source data', key: 'source'},
            {header: null, key: 'SWAP_BUTTON'},
            {header: 'Target data', key: 'target',},
            {header: 'Mapping type', key: 'type'}
        ]
    );

    // store all result, because without pagination
    const [allRows, setAllRows] = useState<IPageSuggestion[]>([]);

    // store rows for current page
    const [pageRows, setPageRows] = useState<IPageSuggestion[]>([]);

    // store all filtered rows by column
    const [filteredRows, setFilteredRows] = useState<IPageSuggestion[]>([]);

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
    const [sortDirections, setSortDirections] = useState<ISortDirection>({
        column: '',
        modifier: ''
    });

    // contain filtered columns filters
    const [columnFilters, setColumnFilters] = useState<string[]>([]);

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
                    console.log(sourceToTargetMap[source], uri);
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
        const filteredRows = filterRows(columnFilters, arr);
        setFilteredRows(filteredRows);

        const ordered = sortRows(filteredRows, sortDirections);
        setPageRows(
            paginate(ordered, pagination)
        );

        console.log('Suggestions', arr);
    }, [rows]);

    const updateRelations = (source, targets: ITargetWithSelected[]) => {
        const {uri, type} = targets.find(t => t._selected);

        setSourceToTargetMap(prevState => ({
            ...prevState,
            [source]: uri
        }));

        setTargetToTypeMap(prevState => ({
            ...prevState,
            [uri]: type
        }));
    };

    // @TODO: can be moved to another place as a utils
    const filterRows = (filters: string[], rows: IPageSuggestion[]): IPageSuggestion[] => {
        let pageItems = [...rows];

        filters.forEach(filter => {
            switch (filter) {
                case FILTER_ACTIONS.SHOW_SELECTED:
                case FILTER_ACTIONS.SHOW_UNSELECTED: {
                    pageItems = pageItems.filter(
                        row => filter === FILTER_ACTIONS.SHOW_SELECTED
                            ? selectedSources.includes(row.source)
                            : !selectedSources.includes(row.source)
                    );
                    break;
                }

                case FILTER_ACTIONS.SHOW_VALUE_MAPPINGS:
                case FILTER_ACTIONS.SHOW_OBJECT_MAPPINGS: {
                    const type = filter === FILTER_ACTIONS.SHOW_VALUE_MAPPINGS ? 'value' : 'object';
                    pageItems = pageItems.filter(
                        row => row.target.every(t => t.type === type)
                    );
                    break;
                }
            }
        });

        return pageItems;
    };

    // @TODO: can be moved to another place as a utils
    const sortRows = (rows: IPageSuggestion[], sortDirections: ISortDirection) => {
        const isAsc = sortDirections.modifier === 'asc';
        const direction = isAsc ? 'desc' : 'asc'

        return _.orderBy(rows, sortDirections.column, direction);
    };

    const toggleRowSelect = ({source, target}: IPageSuggestion) => {
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
            updateRelations(source, target);
        }
    };

    const toggleSelectAll = () => {
        if (isAllSelected()) {
            setSelectedSources([]);
        } else if (!selectedSources.length) {
            pageRows.forEach(toggleRowSelect);
        } else {
            setSelectedSources(
                pageRows.map(row => {
                    updateRelations(row.source, row.target);
                    return row.source;
                })
            );
        }
    };

    const handlePageChange = (pagination: IPagination) => {
        setPagination(pagination);
        setPageRows(
            paginate(filteredRows, pagination)
        );
    };

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
        const direction = isAsc ? 'desc' : 'asc';

        const sortDirection: ISortDirection = {
            column: headerKey,
            modifier: direction
        };

        setSortDirections(sortDirection);

        const sortedArray = sortRows(filteredRows, sortDirection);

        setPageRows(
            paginate(sortedArray, pagination)
        );
    };

    const handleFilterColumn = (action: string) => {
        let colFilters = [...columnFilters];

        if (colFilters.includes(action)) {
            colFilters = colFilters.filter(filter => filter !== action);
        } else {
            colFilters.push(action);
        }
        setColumnFilters(colFilters);

        const filteredRows = filterRows(colFilters, allRows);
        setPageRows(
            paginate(filteredRows, pagination)
        );
        setFilteredRows(filteredRows);
    };

    const handleSwap = () => {
        // reset preview rows
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

    const handleModifyTarget = (row: IPageSuggestion, targets: ITargetWithSelected[]) => {
        const _allRows = [...allRows];
        const ind = _allRows.findIndex(r => r.source === row.source);

        if (ind > -1) {
            updateRelations(row.source, targets);

            _allRows[ind].target = targets;

            setAllRows(_allRows);
        }
    }

    const isAllSelected = () => filteredRows.length && pageRows.length === selectedSources.length;

    return <div ref={portalContainerRef}>
        <SuggestionListContext.Provider value={{
            portalContainer: portalContainerRef.current
        }}>
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
            <Button disruptive={true} onClick={onAskDiscardChanges}>Cancel</Button>
        </SuggestionListContext.Provider>
    </div>
}
