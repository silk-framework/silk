import React from 'react';
import { Tag as BlueprintTag } from "@blueprintjs/core";

function Tag({
    children,
    className = '',
    emphasized = false,
    ...otherProps
}: any) {
    return (
        <BlueprintTag
            {...otherProps}
            className={
                'ecc-tag__item' +
                (className ? ' ' + className : '')
            }
            minimal={!emphasized}
        >
            {children}
        </BlueprintTag>
    );
};

export default Tag;
