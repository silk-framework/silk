import React, { useEffect, useRef, useState } from "react";
import { ISortersState } from "@ducks/workspace/typings";
import { Toolbar, ToolbarSection } from "@wrappers/index";
import SearchInput from "./SearchInput";
import SortButton from "../buttons/SortButton";

interface IProps {
    textQuery?: string;
    sorters?: ISortersState;

    onSort?(sortBy: string): void;

    onSearch(textQuery: string): void;
    focusOnCreation?: boolean;
}

export function SearchBar({ textQuery = "", sorters, onSort, onSearch, focusOnCreation = false }: IProps) {
    const [searchInput, setSearchInput] = useState(textQuery);

    const searchBarRef = useRef(null);
    const findInput = (element: any) => {
        // TODO: hacky, but works, find better solution?
        if (element.localName === "input") {
            return element;
        } else {
            if (element.childNodes) {
                let matchingChild = null;
                Array.prototype.forEach.call(element.childNodes, (child) => {
                    if (child.localName === "input" || child.localName === "div") {
                        const recursiveResult = findInput(child);
                        if (recursiveResult) {
                            matchingChild = recursiveResult;
                        }
                    }
                });
                return matchingChild;
            } else {
                return null;
            }
        }
    };
    useEffect(() => {
        if (searchBarRef !== null && searchBarRef.current !== null && focusOnCreation) {
            const inputElem = findInput(searchBarRef.current);
            if (inputElem) {
                inputElem.focus();
            }
        }
    }, []);

    useEffect(() => {
        setSearchInput(textQuery);
    }, [textQuery]);

    const handleSearchChange = (e) => {
        // when input is empty then apply filter
        if (e.target.value === "" && searchInput) {
            setSearchInput("");
            onSearch("");
        } else {
            setSearchInput(e.target.value);
        }
    };

    const onClearanceHandler = () => {
        setSearchInput("");
        onSearch("");
    };

    const handleSearchEnter = () => {
        onSearch(searchInput);
    };

    return (
        <Toolbar>
            <ToolbarSection canGrow>
                <div ref={searchBarRef}>
                    <SearchInput
                        onFilterChange={handleSearchChange}
                        onEnter={handleSearchEnter}
                        filterValue={searchInput}
                        onClearanceHandler={onClearanceHandler}
                    />
                </div>
            </ToolbarSection>
            <ToolbarSection>
                {!!sorters && onSort && (
                    <SortButton sortersList={sorters.list} onSort={onSort} activeSort={sorters.applied} />
                )}
            </ToolbarSection>
        </Toolbar>
    );
}
