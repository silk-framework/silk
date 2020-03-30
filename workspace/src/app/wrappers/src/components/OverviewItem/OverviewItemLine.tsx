import React from "react";

function OverviewItemLine({
    children,
    className = '',
    small=false,
    large=false,
    ...restProps
}: any) {
    return (
        <div
            {...restProps}
            className={
                'ecc-overviewitem__line ' +
                (small ? 'ecc-overviewitem__line--small ' : '' ) +
                (large ? 'ecc-overviewitem__line--large ' : '' ) +
                className
            }
        >
            {children}
        </div>
    )
}

export default OverviewItemLine;
