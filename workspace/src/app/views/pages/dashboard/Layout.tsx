import React, { useEffect } from "react";
import FilterBar from "../../components/FilterBar/FilterBar";
import Datalist from "../../components/Datalist/Datalist";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "../../../state/ducks/dashboard";

export default function DashboardLayout() {
    const dispatch = useDispatch();

    const data = useSelector(dashboardSel.resultsSelector);
    const searchQuery = useSelector(dashboardSel.appliedFiltersSelector).textQuery;
    const pagination = useSelector(dashboardSel.paginationSelector);

    useEffect(() => {
        dispatch(dashboardOp.fetchListAsync());
    }, []);

    const handleSearch = (value: string) => {
        dispatch(dashboardOp.setSearchQueryAsync(value));
    };

    const handlePageChange = (i: number) => {
        dispatch(dashboardOp.changePageAsync(i))
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
                    searchValue={searchQuery}
                    onPageChange={handlePageChange}
                    pagination={pagination}
                />
            </div>
        </div>
    )
}
