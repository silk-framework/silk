import React, { memo } from 'react';
import { Switch as BlueprintSwitch } from "@blueprintjs/core";

function Switch({className, ...otherProps}:any) {
    const handleChange = (e) => {
        otherProps.onChange(e.target.checked)
    };

    return <BlueprintSwitch
        className="ecc-switch"
        {...otherProps}
        onChange={handleChange}
    />
}

export default memo(Switch);
