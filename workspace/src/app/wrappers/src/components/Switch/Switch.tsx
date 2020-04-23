import React, { memo } from 'react';
import { Switch as BlueprintSwitch } from "@blueprintjs/core";

function Switch(props) {
    const handleChange = (e) => {
        props.onChange(e.target.checked)
    };

    return <BlueprintSwitch
        {...props}
        onChange={handleChange}
    />
}

export default memo(Switch);
