import { Tag, TagList } from "@eccenca/gui-elements";
import _ from "lodash";
import React from "react";

import { COLUMN_FILTERS } from "./constants";

const AVAILABLE_FILTERS = _.keyBy(_.flatMap(COLUMN_FILTERS), "action");

interface IProps {
    // column filters
    filters: { [key: string]: string };

    onRemove(key: string);
}

export default function AppliedFilters({ filters, onRemove }: IProps) {
    return (
        <TagList label={"Applied Filters"}>
            {Object.entries(filters).map(([key, filter]: [string, string]) => (
                <Tag key={key} onRemove={() => onRemove(key)}>
                    {AVAILABLE_FILTERS[filter].label}
                </Tag>
            ))}
        </TagList>
    );
}
