import React from "react";

function OverviewItemActions ({
    children,
    className = '',
    ...restProps
}: any) {
    return (
        <div {...restProps} className={'ecc-overviewitem__actions '+className}>
            {children}
        </div>
    )
}

export default OverviewItemActions;
