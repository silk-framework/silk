import { Checkbox } from "@eccenca/gui-elements";
import React from "react";

interface IProps {
    isChecked?: boolean;

    onSelectFacet(valueId: string);

    label?: any;
    value: string;

    className?: string;
}

export default function FacetItem({ isChecked, onSelectFacet, label, value, ...restProps }: IProps) {
    return (
        <Checkbox
            {...restProps}
            checked={isChecked}
            label={label || value}
            onChange={() => onSelectFacet(value)}
            value={value}
            key={value}
        />
    );
}
