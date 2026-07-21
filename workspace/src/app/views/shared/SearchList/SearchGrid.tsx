import React from "react";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import SearchCard, { SearchCardProps } from "./SearchCard";

interface SearchGridProps extends Omit<SearchCardProps, "item"> {
    data: ISearchResultsServer[];
}

/** Multi-column card grid presentation of the search results. */
export default function SearchGrid({ data, ...cardProps }: SearchGridProps) {
    return (
        <div
            className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3"
            data-test-id="search-result-grid"
        >
            {data.map((item) => (
                <SearchCard key={`${item.id}_${item.projectId}`} item={item} {...cardProps} />
            ))}
        </div>
    );
}
