import React, { memo } from 'react';
import InputGroup from "@wrappers/blueprint/input-group";
import { IconNames } from "@wrappers/blueprint/constants";

interface IProps {
    onFilterChange(e);
    onBlur?();
    onEnter();
    filterValue?: string;
}

const SearchInput = ({ onFilterChange, filterValue, onEnter, onBlur = () => {} }) => {
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
};

const areEqual = (p: IProps, n: IProps) => p.filterValue === n.filterValue;
export default memo<IProps>(SearchInput, areEqual);
