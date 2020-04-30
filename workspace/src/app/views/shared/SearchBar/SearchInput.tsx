import React, { memo } from "react";
import { SearchField } from "@wrappers/index";

interface IProps {
    onFilterChange(e);
    onBlur?();
    onEnter();
    filterValue?: string;
    onClearanceHandler?();
}

const SearchInput = ({ onFilterChange, filterValue, onEnter, onBlur = () => {}, onClearanceHandler = () => {} }) => {
    const handleKeyDown = (e) => {
        if (e.key === "Enter") {
            onEnter();
        }
    };

    return (
        <SearchField
            onChange={onFilterChange}
            onBlur={onBlur}
            onKeyDown={handleKeyDown}
            value={filterValue}
            onClearanceHandler={onClearanceHandler}
        />
    );
};

const areEqual = (p: IProps, n: IProps) => p.filterValue === n.filterValue;
export default memo<IProps>(SearchInput, areEqual);
