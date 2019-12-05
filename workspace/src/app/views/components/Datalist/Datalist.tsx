import React, { useState } from "react";
import Pagination from "./Pagination";
import { HTMLTable, Icon } from "@blueprintjs/core";
import SearchInput from "../../layout/header/SearchInput";
import { IPaginationState } from "../../../state/typings";
import SortButton from "./SortButton";
import { ISorterListItemState } from "../../../state/ducks/dashboard/typings";
import { ISearchResultsTask } from "../../../state/ducks/dashboard/typings/IDashboardPreview";

interface IProps {
    data: any[];
    pagination: IPaginationState;
    searchValue?: string;
    sortersList?: ISorterListItemState[];

    onSearch(value: string): void

    onSort?(value: string): void

    onPageChange(value: number): void

    onClone(task: ISearchResultsTask): void
}

export default function Datalist({data, pagination, searchValue, sortersList, onSearch, onPageChange, onSort, onClone}: IProps) {
    const [searchInput, setSearchInput] = useState();

    const handleRemove = (id: string) => {

    };
    const handleSearchChange = (e) => {
        const {value} = e.target;
        setSearchInput(value);
    };

    const handleSearchBlur = () => {
        if (searchValue !== searchInput) {
            onSearch(searchInput);
        }
    };

    return (
        <>
            <div style={{width: '80%', float: 'left', paddingRight: '10px'}}>
                <SearchInput
                    onFilterChange={handleSearchChange}
                    onBlur={handleSearchBlur}
                />
            </div>
            <div style={{width: '15%', float: 'left'}}>
                <SortButton sortersList={sortersList} onSort={onSort}/>
            </div>
            {
                !data.length
                    ? <p>No resources found</p>
                    : <HTMLTable bordered={true} interactive={true} striped={true}>
                        <thead>
                        <tr>
                            <th colSpan={2}>Results</th>
                        </tr>
                        </thead>
                        <tbody>
                        {
                            data.map(item =>
                                <tr key={item.id}>
                                    <td>
                                        <p>{item.id}</p>
                                        <p>{item.description}</p>
                                    </td>
                                    <td>
                                        <a onClick={() => onClone(item)}>Clone</a>
                                         <a onClick={() => handleRemove(item.id)}>Remove</a>
                                    </td>
                                </tr>
                            )
                        }
                        </tbody>
                        <tfoot>
                        <tr>
                            <td colSpan={3}>
                                <Pagination
                                    pagination={pagination}
                                    onPageChange={onPageChange}
                                />
                            </td>
                        </tr>
                        </tfoot>
                    </HTMLTable>
            }
        </>
    )
}
