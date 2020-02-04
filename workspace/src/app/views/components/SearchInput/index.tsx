import React, { memo, useRef } from 'react';
import Spinner from "@wrappers/spinner";
import InputGroup from "@wrappers/input-group";
import { IconNames } from "@wrappers/constants";
import '../../../../theme/override.scss';


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
