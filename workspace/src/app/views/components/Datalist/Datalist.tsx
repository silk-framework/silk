import React, { useState } from "react";
import Pagination from "./Pagination";
import { HTMLTable } from "@blueprintjs/core";
import SearchInput from "../../layout/header/SearchInput";
import { PaginationDto } from "../../../state/dto";

interface IProps {
    data: any[];
    pagination: PaginationDto;
    searchValue?: string;

    onSearch(value: string): void
    onPageChange(value: number): void
}

export default function Datalist({data, pagination, searchValue, onSearch, onPageChange}: IProps) {
    const [searchInput, setSearchInput] = useState();
    const handleClone = (id: string) => {

    };
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
            <SearchInput
                onFilterChange={handleSearchChange}
                onBlur={handleSearchBlur}
            />
            {
                !data.length
                    ? <p>No resources found</p>
                    : <HTMLTable bordered={true} interactive={true} striped={true}>
                        <thead>
                        <tr>
                            <th colSpan={3}>Results</th>
                        </tr>
                        </thead>
                        <tbody>
                        {
                            data.map(item =>
                                <tr key={item.id}>
                                    <td>
                                        <p>{item.label || item.id}</p>
                                        <p>{item.description}</p>
                                    </td>
                                    <td>
                                        <a onClick={() => handleClone(item.id)}>Clone</a>
                                    </td>
                                    <td>
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
