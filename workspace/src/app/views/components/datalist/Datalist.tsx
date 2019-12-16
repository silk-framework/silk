import React, { useState } from "react";
import Pagination from "./Pagination";
import { HTMLTable, Icon, Popover, Position, Tag } from "@blueprintjs/core";
import SearchInput from "../../layout/header/SearchInput";
import { IPaginationState } from "../../../state/typings";
import SortButton from "./SortButton";
import { IAppliedFacetState, IFacetState, ISorterListItemState } from "../../../state/ducks/dashboard/typings";
import DeleteModal, { IDeleteModalOptions } from "../modals/DeleteModal";
import { Menu } from "@blueprintjs/core/lib/esnext";
import './style.scss';

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
    facets: IFacetState[];
}

export default function Datalist({data, pagination, appliedFacets, facets, hooks, deleteModalOptions, searchValue, sortersList}: IProps) {
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

    const getSearchHighlight = (label: string) => {
        if (searchValue) {
            const regExp = RegExp(searchValue, 'g');
            return label.toString().replace(regExp, `<mark>${searchValue}</mark>`)
        }
        return label;
    };

    const getRowMenu = (item: any) => {
        const {itemLinks} = item;
        const menuItems = itemLinks.map(link =>
            <Menu.Item text={link.label} href={link.path} target={'_blank'}/>
        );
        menuItems.push(
            <Menu.Item icon={'trash'} onClick={() => toggleDeleteModal(item)} text={'Delete'}/>
        );

        return (
            <Menu>
                {menuItems}
            </Menu>
        )
    };

    const facetsList = [];
    appliedFacets.map(appliedFacet => {
        const facet = facets.find(o => o.id === appliedFacet.facetId);
        if (facet) {
            facetsList.push({
                label: facet.label,
                id: facet.id,
                keywords: facet.values.filter(key => appliedFacet.keywordIds.includes(key.id))
            });

        }
    });
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
                                hooks.onFacetRemove && facetsList.map(facet =>
                                    <div
                                        key={facet.id}
                                        className='tags-group'
                                    >
                                        <div className='tag-label'>{facet.label}:</div>
                                        {
                                            facet.keywords.map(keyword =>
                                                <Tag
                                                    className='tag'
                                                    onRemove={() => hooks.onFacetRemove(facet.id, keyword.id)}
                                                >
                                                    {keyword.label}
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
                                                <p dangerouslySetInnerHTML={{
                                                    __html: getSearchHighlight(item.label || item.id)
                                                }}
                                                />
                                                <p>{item.description}</p>
                                            </td>
                                            <td>
                                                <Popover content={getRowMenu(item)} position={Position.BOTTOM_LEFT}>
                                                    <Icon icon="more"/>
                                                </Popover>
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
