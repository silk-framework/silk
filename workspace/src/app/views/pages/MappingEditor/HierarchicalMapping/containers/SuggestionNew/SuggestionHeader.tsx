import { SearchField } from "@eccenca/gui-elements";
import React, { useState } from "react";

/** Mapping suggestion header including search widget etc. */
export default function SuggestionHeader({ onSearch }) {
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
            data-test-id={"search_input"}
            value={searchValue}
            emptySearchInputMessage={"Search in table"}
            autoFocus={false}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            onBlur={() => {}}
            onClearanceHandler={onClearanceHandler}
        />
    );
}
