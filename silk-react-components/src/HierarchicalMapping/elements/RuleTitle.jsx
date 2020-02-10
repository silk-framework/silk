import React from 'react';
import _ from 'lodash';
import { NotAvailable } from '@eccenca/gui-elements';
import { ThingName } from '../components/ThingName';

import {
    MAPPING_RULE_TYPE_ROOT,
} from '../utils/constants';
import { MAPPING_RULE_TYPE_COMPLEX, MAPPING_RULE_TYPE_DIRECT, MAPPING_RULE_TYPE_OBJECT } from '../utils/constants';

const RuleTitle = ({ rule, ...otherProps }) => {
    let uri;
    const label = _.get(rule, 'metadata.label', '');
    if (label) {
        return <span>{label}</span>;
    }
    if (!rule.type) {
        return <NotAvailable />;
    }

    switch (rule.type) {
    case MAPPING_RULE_TYPE_ROOT:
        uri = _.get(rule, 'rules.typeRules[0].typeUri', false);
        return uri ? (
            <ThingName id={uri} {...otherProps} />
        ) : (
            <NotAvailable />
        );
    case MAPPING_RULE_TYPE_DIRECT:
    case MAPPING_RULE_TYPE_OBJECT:
    case MAPPING_RULE_TYPE_COMPLEX:
        uri = _.get(rule, 'mappingTarget.uri', false);
        return uri ? (
            <ThingName id={uri} {...otherProps} />
        ) : (
            <NotAvailable />
        );
    }
};

export default RuleTitle;
