import React, { useEffect, useState } from "react";
import FilterBar from "./FilterBar/FilterBar";
import Datalist from "../../components/datalist/Datalist";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "../../../state/ducks/dashboard";
import './style.scss';
import { ISearchResultsTask } from "../../../state/ducks/dashboard/typings";
import Loading from "../../components/loading/Loading";

export default function DashboardLayout() {
    const dispatch = useDispatch();

    const [deleteModalOptions, setDeleteModalOptions] = useState({});

    const data = useSelector(dashboardSel.resultsSelector);
    const sorters = useSelector(dashboardSel.sortersSelector);

    const pagination = useSelector(dashboardSel.paginationSelector);
    const appliedFilters = useSelector(dashboardSel.appliedFiltersSelector);
    const appliedFacets = useSelector(dashboardSel.appliedFacetsSelector);

    useEffect(() => {
        dispatch(dashboardOp.fetchListAsync());
    }, [appliedFilters, sorters.applied, pagination.current]);

    const handleSearch = (value: string) => {
        const filter = {
            field: 'textQuery',
            value
        };
        dispatch(dashboardOp.applyFilter(filter));
    };

    const handlePageChange = (n: number) => {
        dispatch(dashboardOp.changePage(n))
    };

    const handleSort = (value: string) => {
        dispatch(dashboardOp.applySorter(value));
    };

    const handleRemoveModalOpen = async ({id, projectId, label}: ISearchResultsTask) => {
        setDeleteModalOptions({
            render: () => <Loading/>
        });

        const data = await dashboardOp.getTaskMetadataAsync(id, projectId);
        const {dependentTasksDirect} = data.relations;

        if (dependentTasksDirect.length) {
            setDeleteModalOptions({
                confirmationRequired: true,
                render: () =>
                    <div>
                        <p>There are tasks depending on task {label || id}. </p>
                        <p>Are you sure you want to delete all tasks below?</p>
                        <ul>
                            {
                                dependentTasksDirect.map(rel => <li key={rel}>{rel}</li>)
                            }
                        </ul>
                    </div>
            });
        } else {
            setDeleteModalOptions({
                confirmationRequired: false,
                render: () => <p>
                    Are you sure you want to permanently remove this item?
                </p>
            });
        }
    };

    const handleItemRemove = ({id, projectId}: ISearchResultsTask) => {
        dispatch(dashboardOp.fetchRemoveTaskAsync(id, projectId));
    };

    const onFacetRemove = (facetId: string, keywordId: string) => {
        dispatch(dashboardOp.removeFacet({
            facetId,
            keywordId
        }));
    };

    return (
        <div className='main clearfix'>
            <div className='left-content'>
                <FilterBar/>
            </div>
            <div className={'right-content'}>
                <Datalist
                    data={data}
                    pagination={pagination}
                    sortersList={sorters.list}
                    appliedFacets={appliedFacets}
                    searchValue={appliedFilters.textQuery}
                    hooks={{
                        onRemoveModalOpened: handleRemoveModalOpen,
                        onItemRemove: handleItemRemove,
                        onPageChange: handlePageChange,
                        onSearch: handleSearch,
                        onSort: handleSort,
                        onFacetRemove: onFacetRemove
                    }}
                    deleteModalOptions={deleteModalOptions}
                />
            </div>
        </div>
    )
}
