import React, { useState } from "react";
import Pagination from "./Pagination";
import { HTMLTable, Icon, Tag } from "@blueprintjs/core";
import SearchInput from "../../layout/header/SearchInput";
import { IPaginationState } from "../../../state/typings";
import SortButton from "./SortButton";
import { IAppliedFacetState, ISorterListItemState } from "../../../state/ducks/dashboard/typings";
import DeleteModal, { IDeleteModalOptions } from "../modals/DeleteModal";


interface IProps {
    data: any[];
    pagination: IPaginationState;
    searchValue?: string;
    sortersList?: ISorterListItemState[];
    appliedFacets: IAppliedFacetState[];
    hooks: {
        onRemoveModalOpened?(item: any): void;
        onItemRemove?(item: any): void;
        onSearch(value: string): void;
        onSort?(value: string): void;
        onPageChange(value: number): void;
        onFacetRemove?(facetId: string, keyword: string): void;
    }
    deleteModalOptions: Partial<IDeleteModalOptions>;
}

export default function Datalist({data, pagination, appliedFacets, hooks, deleteModalOptions, searchValue, sortersList}: IProps) {
    const [searchInput, setSearchInput] = useState();
    const [selectedItem, setSelectedItem] = useState();
    const [showDeleteModal, setShowDeleteModal] = useState();

    const handleSearchChange = (e) => {
        setSearchInput(e.target.value);
    };

    const handleSearchBlur = () => {
        if (searchValue !== searchInput) {
            hooks.onSearch(searchInput);
        }
    };

    const toggleDeleteModal = (item?: any) => {
        setShowDeleteModal(!showDeleteModal);
        if (!showDeleteModal) {
            setSelectedItem(item);
        }
        if (hooks.onRemoveModalOpened) {
            hooks.onRemoveModalOpened(item);
        }
    };

    const handleConfirmRemove = () => {
        setShowDeleteModal(false);
        if (hooks.onItemRemove) {
            hooks.onItemRemove(selectedItem);
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
                    <SortButton sortersList={sortersList} onSort={hooks.onSort}/>
                </div>
            </div>
            <div style={{marginTop: '15px'}}>
                {
                    !data.length
                        ? <p>No resources found</p>
                        : <div>
                            {
                                hooks.onFacetRemove && appliedFacets.map(facet =>
                                    <div
                                        key={facet.facetId}
                                        style={{display:'inline-block'}}
                                    >
                                        {facet.facetId}
                                        {
                                            facet.keywordIds.map(keyword =>
                                                <Tag onRemove={() => hooks.onFacetRemove(facet.facetId, keyword)}>
                                                    {keyword}
                                                </Tag>
                                            )
                                        }
                                    </div>

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
                                                <a style={{'paddingRight': '10px'}}>
                                                    <Icon icon={'duplicate'}/>
                                                </a>
                                                <a onClick={() => toggleDeleteModal(item)}>
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
                                                onPageChange={hooks.onPageChange}
                                            />
                                        </td>
                                    </tr>
                                </tfoot>
                            </HTMLTable>
                        </div>
                }
            </div>
            <DeleteModal
                isOpen={showDeleteModal}
                onDiscard={toggleDeleteModal}
                onConfirm={handleConfirmRemove}
                {...deleteModalOptions}
            />
        </>
    )
}
