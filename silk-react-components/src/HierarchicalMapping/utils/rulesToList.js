import _ from 'lodash';

const rulesToList = (rawRules, parentId) => {
    return _.map(rawRules, (rule, i) => ({
        id: i,
        key: rule.id,
        props: {
            pos: i,
            parentId,
            count: rawRules.length,
            key: `MappingRule_${rule.id}`,
            ...rule,
        },
        errorInfo:
            _.get(rule, 'status[0].type', false) === 'error'
                ? _.get(rule, 'status[0].message', false)
                : false,
    }));
};

export default rulesToList;
