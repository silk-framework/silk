import React, { memo, useEffect } from 'react';
import { InputGroup, Spinner } from "@blueprintjs/core";

interface IProps {
    onFilterChange: (e) => any;
    onBlur: () => any;
    filterValue?: string;
}

const SearchInput = memo(({ onFilterChange, filterValue, onBlur }: IProps) => {
    const maybeSpinner = filterValue ? <Spinner /> : undefined;

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            onBlur();
        }
    };

    return (
        <InputGroup
            type={'search'}
            leftIcon={'search'}
            onChange={onFilterChange}
            onBlur={onBlur}
            onKeyDown={handleKeyDown}
            rightElement={maybeSpinner}
            value={filterValue}
        />
    )
});

export default SearchInput;
