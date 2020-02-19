import React, { useEffect, useState } from "react";
import SearchInput from "../../../components/SearchInput";
import { workspaceOp, workspaceSel } from "../../../../store/ducks/workspace";
import { useDispatch, useSelector } from "react-redux";
import SortButton from "./SortButton";

export default function () {
    const dispatch = useDispatch();
    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);

    const [searchInput, setSearchInput] = useState(textQuery);

    useEffect(() => {
        setSearchInput(textQuery);
    }, [textQuery]);

    const handleSearchChange = (e) => {
        setSearchInput(e.target.value);
    };

    const handleSort = (sortBy: string) => {
        dispatch(workspaceOp.applySorterOp(sortBy));
    };

    const handleSearchEnter = () => {
        dispatch(workspaceOp.applyFiltersOp({
            textQuery: searchInput
        }));
    };

    return (
        <div className="clearfix">
                <SearchInput
                    onFilterChange={handleSearchChange}
                    onEnter={handleSearchEnter}
                    filterValue={searchInput}
                />
                <SortButton sortersList={sorters.list} onSort={handleSort} activeSort={sorters.applied}/>
        </div>
    )
}
