import React, { memo } from 'react';
import { Label as B_Label } from "@blueprintjs/core";


const Label = memo(({ children }) => {
    return (
        <B_Label>{children}</B_Label>
    );
});

export default Label;
