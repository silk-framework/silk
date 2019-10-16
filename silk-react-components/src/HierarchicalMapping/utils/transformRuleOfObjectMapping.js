import _ from 'lodash';
import { MAPPING_RULE_TYPE_URI } from './constants';

/**
 * extract needed object mapping data from given rules
 * @param rule
 */
const transformRuleOfObjectMapping = (rule = {}) => {
    if (_.isEmpty(rule)) {
        return {};
    }
    const {
        mappingTarget = {}, sourcePath, metadata = {}, type, rules = {},
    } = rule;
    const { uri, isBackwardProperty } = mappingTarget;
    const { description = '', label = '' } = metadata;
    const { uriRule } = rules;

    return ({
        targetProperty: uri,
        sourceProperty: sourcePath,
        comment: description,
        label,
        targetEntityType: _.map(rules && rules.typeRules, 'typeUri'),
        entityConnection: isBackwardProperty ? 'to' : 'from',
        pattern: (uriRule && uriRule.pattern) || '',
        type,
        uriRuleType: (uriRule && uriRule.type) || MAPPING_RULE_TYPE_URI,
        uriRule,
    });
};

export default transformRuleOfObjectMapping;
