import React, {useContext} from "react";
import {
    Button,
    ContextMenu,
    IconButton,
    MenuItem,
    Spacing,
    TableHead,
    TableHeader,
    TableRow,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import {COLUMN_FILTERS} from "../constants";
import {ISortDirection, ITableHeader} from "../suggestion.typings";
import {SuggestionListContext} from "../SuggestionContainer";
import ColumnFilter from "./ColumnFilter";

interface IProps {
    // table headers
    headers: ITableHeader[];

    // flag for select-all checkbox
    isAllSelected: boolean;

    // callback for select-all checkbox
    toggleSelectAll(scope: 'all' | 'page', action: 'select' | 'unselect');

    // callback for swap button
    onSwap();

    // column sorting information
    sortDirections: ISortDirection;

    // callback for column sorting
    onSort(headerKey: string);

    // callback for column filtering
    onApplyFilter(columnName: string, filter: string);

    // applied column filters
    appliedFilters: {
        [key: string]: string
    };

    // percentage of selected suggestion rules
    ratioSelection: number;
}

const checkAllFilterOptions = [
    // FIXME: Select all page items is not additive
    // {
    //     text: 'Select items on current page',
    //     value: 'page_select',
    // },
    {
        text: 'Select all items',
        value: 'all_select',
    },
    // {
    //     text: 'Unselect all items on current page',
    //     value: 'page_unselect',
    // },
    {
        text: 'Unselect all items',
        value: 'all_unselect',
    }
];

export default function STableHeader({
     headers,
     toggleSelectAll,
     onSwap,
     sortDirections,
     onSort,
     onApplyFilter,
     appliedFilters,
     ratioSelection
 }: IProps) {
    const context = useContext(SuggestionListContext);
    const {portalContainer} = context;

    const handleSort = (value: string) => {
        const [scope, action] = value.split('_');
        toggleSelectAll(scope as 'all' | 'page', action as 'select' | 'unselect');
    }

    // Optional swap button
    const swapButton = (header: ITableHeader) => {
        return (context.vocabulariesAvailable || !context.isFromDataset) &&
            <Button
                onClick={onSwap}
                data-test-id={header.key}
                tooltip="Swap view of source data and target vocabulary"
                tooltipProps={{ portalContainer: portalContainer }}
            >
                Swap
            </Button>
    }

    return <TableHead>
        <TableRow>
            <TableHeader>
                <Toolbar>
                    <ToolbarSection>
                        <ContextMenu
                            contextOverlayProps={{
                                portalContainer
                            }}
                            togglerElement={ratioSelection === 0 ? "state-unchecked" : ratioSelection === 1 ? "state-checked" : "state-partlychecked"}
                        >
                            {
                                checkAllFilterOptions.map(o => (
                                    <MenuItem
                                        key={o.value}
                                        text={o.text}
                                        onClick={() => handleSort(o.value)}
                                    />
                                ))
                            }
                        </ContextMenu>
                        <ColumnFilter
                            data-test-id={"suggest-table-selection-filter-btn"}
                            selectedFilter={appliedFilters.checkbox}
                            filters={COLUMN_FILTERS.checkbox}
                            onApplyFilter={(filter) => onApplyFilter('checkbox', filter)}
                            fromDataset={context.isFromDataset}
                        />
                    </ToolbarSection>
                </Toolbar>
            </TableHeader>

            {headers.map(header => (
                <TableHeader key={header.key}>
                    {
                        header.key === 'SWAP_BUTTON'
                            ? swapButton(header)
                            : <Toolbar noWrap={true}>
                                <ToolbarSection canShrink={true}>
                                    {header.header}
                                </ToolbarSection>
                                <ToolbarSection>
                                    <Spacing size="tiny" vertical={true} />
                                    <IconButton
                                        data-test-id={header.sortDataTestId}
                                        name={
                                            sortDirections.column !== header.key
                                                ? 'list-sort'
                                                : sortDirections.modifier === 'asc' ? 'list-sortasc' : 'list-sortdesc'
                                        }
                                        text={"Sort column: " +
                                            (sortDirections.column !== header.key
                                                ? 'ascending'
                                                : (sortDirections.modifier === 'asc' ? 'descending' : 'remove'))
                                        }
                                        tooltipProps={{ portalContainer: portalContainer }}
                                        onClick={() => onSort(header.key)}
                                    />
                                    {
                                        COLUMN_FILTERS[header.key] && <ColumnFilter
                                            data-test-id={header.filterDataTestId}
                                            selectedFilter={appliedFilters[header.key]}
                                            filters={COLUMN_FILTERS[header.key]}
                                            onApplyFilter={(filter) => onApplyFilter(header.key, filter)}
                                            fromDataset={context.isFromDataset}
                                        />
                                    }
                                </ToolbarSection>
                            </Toolbar>
                    }
                </TableHeader>
            ))}
        </TableRow>
    </TableHead>
}
