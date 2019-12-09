import React, { useState } from "react";
import Pagination from "./Pagination";
import { HTMLTable, Icon, Tag } from "@blueprintjs/core";
import SearchInput from "../../layout/header/SearchInput";
import { IPaginationState } from "../../../state/typings";
import SortButton from "./SortButton";
import { ISorterListItemState } from "../../../state/ducks/dashboard/typings";
import Dialog from "../wrappers/dialog/Dialog";

interface IProps {
    data: any[];
    pagination: IPaginationState;
    searchValue?: string;
    sortersList?: ISorterListItemState[];
    appliedModifiers: string[];

    onSearch(value: string): void

    onSort?(value: string): void

    onPageChange(value: number): void

}

export default function Datalist({data, pagination, appliedModifiers, searchValue, sortersList, onSearch, onPageChange, onSort}: IProps) {
    const [searchInput, setSearchInput] = useState();
    const [showRemoveDialog, setSowRemoveDialog] = useState();
    const [selectedRow, setSelectedRow] = useState();

    const handleRemove = (id: string) => {
        setSowRemoveDialog(true);
        setSelectedRow(id);
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

    const handleModalClose = (isConfirm: boolean) => {
        if (isConfirm) {
            console.log('fetch remove');
        } else {
            setSowRemoveDialog(false);
        }
    };

    return (
        <>
            <div className="clearfix">
                <div style={{width: '80%', float: 'left'}}>
                    <SearchInput
                        onFilterChange={handleSearchChange}
                        onBlur={handleSearchBlur}
                    />
                </div>
                <div style={{width: '15%', float: 'left', marginLeft: '15px'}}>
                    <SortButton sortersList={sortersList} onSort={onSort}/>
                </div>
            </div>
            <div style={{marginTop: '15px'}}>
                {
                    !data.length
                        ? <p>No resources found</p>
                        : <div>
                            {
                                appliedModifiers.map(modifier =>
                                    <Tag
                                        key={modifier}
                                        onRemove={() => {}}
                                    >
                                        {modifier}
                                    </Tag>
                                )
                            }
                            <HTMLTable bordered={true} interactive={true} striped={true}>
                                <tbody>
                                {
                                    data.map(item =>
                                        <tr key={item.id}>
                                            <td>
                                                <p>{item.label || item.id}</p>
                                                <p>{item.description}</p>
                                            </td>
                                            <td>
                                                <a onClick={() => {
                                                }} style={{'paddingRight': '10px'}}>
                                                    <Icon icon={'duplicate'}/>
                                                </a>
                                                <a onClick={() => handleRemove(item.id)}>
                                                    <Icon icon={'trash'}/>
                                                </a>
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
                        </div>
                }
            </div>
            <Dialog
                isOpen={showRemoveDialog}
                onClose={handleModalClose}
            />
        </>
    )
}
