import _ from 'lodash';
import { NotAvailable } from '@eccenca/gui-elements';
import React from 'react';

export const SourcePath = ({rule}) => {
    const path = _.get(rule, 'sourcePath', <NotAvailable inline/>);
    return <span>{_.isArray(path) ? path.join(', ') : path}</span>;
};
