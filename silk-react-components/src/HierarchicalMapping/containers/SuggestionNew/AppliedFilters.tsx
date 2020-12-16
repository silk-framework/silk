import React from 'react';
import { Tag, TagList } from "@gui-elements/index";
import { COLUMN_FILTERS } from "./constants";
import _ from 'lodash';

const AVAILABLE_FILTERS = _.keyBy(_.flatMap(COLUMN_FILTERS), 'action')

export default function AppliedFilters({ filters, onRemove }) {

    return <TagList label={'Applied Filters'}>
        {
            Object
                .entries(filters)
                .map(([key, filter]: [string, string]) => (
                    <Tag key={key} onRemove={() => onRemove(key)}>
                        {AVAILABLE_FILTERS[filter].label}
                    </Tag>
        ))}
    </TagList>
}
