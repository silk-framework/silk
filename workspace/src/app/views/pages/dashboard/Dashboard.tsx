import React, { useEffect } from "react";
import FilterBar from "./FilterBar/FilterBar";
import Datalist from "../../components/Datalist/Datalist";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "../../../state/ducks/dashboard";
import { ISearchResultsTask } from "../../../state/ducks/dashboard/typings/IDashboardPreview";

export default function DashboardLayout() {
    const dispatch = useDispatch();

    const data = useSelector(dashboardSel.resultsSelector);
    const sorters = useSelector(dashboardSel.sortersSelector);

    const pagination = useSelector(dashboardSel.paginationSelector);
    const appliedFilters = useSelector(dashboardSel.appliedFiltersSelector);

    useEffect(() => {
        dispatch(dashboardOp.fetchListAsync());
    }, [appliedFilters, sorters.applied, pagination.current]);

    const handleSearch = (value: string) => {
        dispatch(dashboardOp.applyFilter({
            field: 'textQuery',
            value
        }));
    };

    const handlePageChange = (i: number) => {
        dispatch(dashboardOp.changePage(i))
    };

    const handleSort = (value: string) => {
        dispatch(dashboardOp.applySorter(value));
    };

    const handleClone = (task: ISearchResultsTask) => {
        dispatch(dashboardOp.cloneTask(task));
    };

    return (
        <div style={{'marginTop': '50px', padding: '5px 10px'}}>
            <div style={{'float': 'left', 'width': '200px'}}>
                <FilterBar />
            </div>
            <div style={{'float': 'left'}}>
                <Datalist
                    data={data}
                    onSearch={handleSearch}
                    searchValue={appliedFilters.textQuery}
                    onPageChange={handlePageChange}
                    pagination={pagination}
                    sortersList={sorters.list}
                    onSort={handleSort}
                    onClone={handleClone}
                />
            </div>
        </div>
    )
}
