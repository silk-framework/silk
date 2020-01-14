import React from 'react';
import Checkbox from '@wrappers/checkbox';

interface IProps {
    isChecked?: boolean;
    onSelectFacet(valueId: string),
    label?: string;
    value: string;
}

export default function FacetItem({ isChecked, onSelectFacet, label, value }: IProps) {
    return (
        <Checkbox
            checked={isChecked}
            label={label || value}
            onChange={() => onSelectFacet(value)}
            value={value}
            key={value} />
    )
}
