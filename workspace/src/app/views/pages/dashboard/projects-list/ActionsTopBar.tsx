import React, { useState } from "react";
import SortButton from "./SortButton";
import SearchInput from "../../../components/search-input/SearchInput";
import { dashboardOp, dashboardSel } from "@ducks/dashboard";
import { useDispatch, useSelector } from "react-redux";

export default function ActionsTopBar() {
    const dispatch = useDispatch();
    const sorters = useSelector(dashboardSel.sortersSelector);

    const [searchInput, setSearchInput] = useState();

    const handleSort = (value: string) => {
        dispatch(dashboardOp.applySorter(value));
    };

    const handleSearchChange = (e) => {
        setSearchInput(e.target.value);
    };

    const handleSearchBlur = () => {
        const filter = {
            field: 'textQuery',
            value: searchInput
        };
        dispatch(dashboardOp.applyFilter(filter));
    };

    return (
        <div className="clearfix">
            <div style={{width: '80%', float: 'left'}}>
                <SearchInput
                    onFilterChange={handleSearchChange}
                    onBlur={handleSearchBlur}
                />
            </div>
            <div style={{width: '15%', float: 'left', marginLeft: '15px'}}>
                <SortButton sortersList={sorters.list} onSort={handleSort}/>
            </div>
        </div>
    )
}
