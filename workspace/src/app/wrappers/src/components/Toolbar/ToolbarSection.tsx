import React from "react";

function ToolbarSection({
    children,
    className = '',
    canGrow = false,
    ...otherProps
}: any) {
    return (
        <div
            {...otherProps}
            className={
                'ecc-toolbar__section' +
                (canGrow ? ' ecc-toolbar__section--cangrow' : '') +
                (className ? ' ' + className : '')
            }
        >
            { children }
        </div>
    )
}

export default ToolbarSection;
