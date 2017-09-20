
export const MAPPING_RULE_TYPE_ROOT = 'root';
export const MAPPING_RULE_TYPE_OBJECT = 'object';
export const MAPPING_RULE_TYPE_DIRECT = 'direct';

export const isObjectMappingRule = (type) => {
    return MAPPING_RULE_TYPE_ROOT === type || MAPPING_RULE_TYPE_OBJECT === type;
};