import React, { memo } from 'react';
import { Checkbox as B_Checkbox, ICheckboxProps } from "@blueprintjs/core";


const Checkbox = memo(({ label, value, checked, onChange }: ICheckboxProps) => {
    return (
        <B_Checkbox
            checked={checked}
            label={label}
            onChange={onChange}
            value={value}
        />
    );
});

export default Checkbox;
