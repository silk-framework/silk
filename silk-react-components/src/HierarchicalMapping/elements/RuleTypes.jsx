import React from 'react';
import _ from 'lodash';
import { NotAvailable } from '@eccenca/gui-elements';
import { ThingName } from '../components/ThingName';

import {
    MAPPING_RULE_TYPE_ROOT,
} from '../utils/constants';
import { MAPPING_RULE_TYPE_COMPLEX, MAPPING_RULE_TYPE_DIRECT, MAPPING_RULE_TYPE_OBJECT } from '../utils/constants';

const RuleTypes = ({ rule, ...otherProps }) => {
    switch (rule.type) {
    case MAPPING_RULE_TYPE_OBJECT:
        let types = _.get(rule, 'rules.typeRules', []);
        types = _.isEmpty(types)
            ? <NotAvailable />
            : types
                .map(({ typeUri }) => (
                    <ThingName id={typeUri} key={typeUri} />
                ))
                .reduce((prev, curr) => [prev, ', ', curr]);
        return <span {...otherProps}>{types}</span>;
    case MAPPING_RULE_TYPE_DIRECT:
    case MAPPING_RULE_TYPE_COMPLEX:
        let appendText = _.get(rule, 'mappingTarget.valueType.lang', '');
        if (appendText) { // add language tag if available
            appendText = ` (${appendText})`;
        }
        return (
            <span {...otherProps}>
                {_.get(rule, 'mappingTarget.valueType.nodeType', <NotAvailable />) + appendText}
            </span>
        );
    case MAPPING_RULE_TYPE_ROOT:
        return <span />;
    }
};

export default RuleTypes;
