import React from 'react';
import { Checkbox } from '@wrappers/index';

interface IProps {
    isChecked?: boolean;

    onSelectFacet(valueId: string),

    label?: any;
    value: string;
}

export default function FacetItem({isChecked, onSelectFacet, label, value}: IProps) {
    return (
        <Checkbox
            checked={isChecked}
            label={label || value}
            onChange={() => onSelectFacet(value)}
            value={value}
            key={value}/>
    )
}
