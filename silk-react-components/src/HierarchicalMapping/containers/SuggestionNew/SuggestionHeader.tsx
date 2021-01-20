import React, { useState } from 'react';

import {
    SearchField
} from "@gui-elements/index";

export default function SuggestionHeader({ onSearch }) {
    /*
    return (
        <TableToolbar>
            <TableToolbarContent>
                <TableToolbarSearch
                    data-test-id={'search_input'}
                    tabIndex={0}
                    onChange={e => onSearch(e.target.value)}
                />
            </TableToolbarContent>
        </TableToolbar>

    )
    */

    const [searchValue, setSearchValue] = useState("");

    const onClearanceHandler = () => {
        onSearch("");
        setSearchValue("");
    };

    const handleChange = (e) => {
        // when input is empty then apply filter
        if (e.target.value === "" && searchValue) {
            onSearch("");
        }
        setSearchValue(e.target.value);
    };

    const handleKeyDown = (e) => {
        if (e.key === "Enter") {
            onSearch(searchValue);
        }
    };

    return (
        <SearchField
            data-test-id={'search_input'}
            value={searchValue}
            emptySearchInputMessage={"Search in table"}
            autoFocus={false}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            onBlur={()=>{}}
            onClearanceHandler={onClearanceHandler}
        />
    )
}
