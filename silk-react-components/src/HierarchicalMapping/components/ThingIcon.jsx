import { MAPPING_RULE_TYPE_COMPLEX, MAPPING_RULE_TYPE_DIRECT, MAPPING_RULE_TYPE_OBJECT } from '../utils/constants';
import { Icon } from '@eccenca/gui-elements';
import React from 'react';

export const ThingIcon = ({type, status, message}) => {
    let iconName = 'help_outline';
    let tooltip = '';
    switch (type) {
        case MAPPING_RULE_TYPE_DIRECT:
        case MAPPING_RULE_TYPE_COMPLEX:
            tooltip = 'Value mapping';
            iconName = 'insert_drive_file';
            break;
        case MAPPING_RULE_TYPE_OBJECT:
            tooltip = 'Object mapping';
            iconName = 'folder';
            break;
        default:
            iconName = 'help_outline';
    }
    
    return (
        <Icon
            className="ecc-silk-mapping__ruleitem-icon"
            name={status === 'error' ? 'warning' : iconName}
            tooltip={status === 'error' ? `${tooltip} (${message})` : tooltip}
        />
    );
};
