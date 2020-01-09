import React, { memo } from 'react';
import Spinner from "@wrappers/spinner";
import InputGroup from "@wrappers/input-group";
import { IconNames } from "@wrappers/constants";

interface IProps {
    onFilterChange: (e) => any;
    onBlur: () => any;
    filterValue?: string;
}

const SearchInput = memo(({ onFilterChange, filterValue, onBlur }: IProps) => {
    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            onBlur();
        }
    };

    return (
        <InputGroup
            type={'search'}
            leftIcon={IconNames.SEARCH}
            onChange={onFilterChange}
            onBlur={onBlur}
            onKeyDown={handleKeyDown}
            value={filterValue}
        />
    )
});

export default SearchInput;
