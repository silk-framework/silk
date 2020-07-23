import React, { memo } from "react";
import { SearchField } from "@wrappers/index";

interface IProps {
    onFilterChange(e);
    onBlur?();
    onEnter();
    filterValue?: string;
    onClearanceHandler?();
    // The message that is shown when the search input is empty
    emptySearchInputMessage?: string;
    // Gives the search input the focus if true
    focusOnCreation?: boolean;
}

const SearchInput = ({
    onFilterChange,
    filterValue,
    onEnter,
    onBlur = () => {},
    onClearanceHandler = () => {},
    emptySearchInputMessage = "Enter search term",
    focusOnCreation = false,
    ...restProps
}: IProps) => {
    const handleKeyDown = (e) => {
        if (e.key === "Enter") {
            onEnter();
        }
    };

    return (
        <SearchField
            {...restProps}
            autoFocus={focusOnCreation}
            onChange={onFilterChange}
            onBlur={onBlur}
            onKeyDown={handleKeyDown}
            value={filterValue}
            onClearanceHandler={onClearanceHandler}
            emptySearchInputMessage={emptySearchInputMessage}
        />
    );
};

const areEqual = (p: IProps, n: IProps) => p.filterValue === n.filterValue;
export default memo<IProps>(SearchInput, areEqual);
