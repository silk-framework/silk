import React, { memo } from 'react';
import InputGroup from "@wrappers/bluprint/input-group";
import { IconNames } from "@wrappers/bluprint/constants";

interface IProps {
    onFilterChange(e);
    onBlur?();
    onEnter();
    filterValue?: string;
}

const SearchInput = memo(({ onFilterChange, filterValue, onEnter, onBlur = () => {} }: IProps) => {
    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            onEnter();
        }
    };

    return (
        <InputGroup
            className={'searchField'}
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
