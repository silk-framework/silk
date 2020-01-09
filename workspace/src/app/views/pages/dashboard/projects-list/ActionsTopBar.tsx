import React, { useState } from "react";
import SortButton from "./SortButton";
import SearchInput from "../../../components/search-input/SearchInput";
import { dashboardOp, dashboardSel } from "@ducks/dashboard";
import { useDispatch, useSelector } from "react-redux";

export default function ActionsTopBar() {
    const dispatch = useDispatch();
    const sorters = useSelector(dashboardSel.sortersSelector);

    const [searchInput, setSearchInput] = useState();

    const handleSearchChange = (e) => {
        setSearchInput(e.target.value);
    };

    const handleSort = (sortBy: string) => {
        dispatch(dashboardOp.applySorterOp(sortBy));
    };

    const handleSearchBlur = () => {
        dispatch(dashboardOp.applyFiltersOp({
            textQuery: searchInput
        }));
    };

    return (
        <div className="clearfix" style={{marginLeft: '-10px', marginBottom: '10px'}}>
            <div style={{width: '80%', float: 'left'}}>
                <SearchInput
                    onFilterChange={handleSearchChange}
                    onBlur={handleSearchBlur}
                />
            </div>
            <div style={{width: '15%', float: 'left', marginLeft: '15px'}}>
                <SortButton sortersList={sorters.list} onSort={handleSort} activeSort={sorters.applied}/>
            </div>
        </div>
    )
}
