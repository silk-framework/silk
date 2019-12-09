import React, { useEffect } from "react";
import FilterBar from "./FilterBar/FilterBar";
import Datalist from "../../components/Datalist/Datalist";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "../../../state/ducks/dashboard";
import './style.scss';

export default function DashboardLayout() {
    const dispatch = useDispatch();

    const data = useSelector(dashboardSel.resultsSelector);
    const sorters = useSelector(dashboardSel.sortersSelector);

    const pagination = useSelector(dashboardSel.paginationSelector);
    const appliedFilters = useSelector(dashboardSel.appliedFiltersSelector);

    const appliedModifiers = [];
    if (appliedFilters.itemType) {
        appliedModifiers.push(appliedFilters.itemType);
    }

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

    return (
        <div className='main clearfix'>
            <div className='left-content'>
                <FilterBar />
            </div>
            <div className={'right-content'}>
                <Datalist
                    data={data}
                    onSearch={handleSearch}
                    searchValue={appliedFilters.textQuery}
                    onPageChange={handlePageChange}
                    pagination={pagination}
                    sortersList={sorters.list}
                    onSort={handleSort}
                    appliedModifiers={appliedModifiers}
                />
            </div>
        </div>
    )
}
