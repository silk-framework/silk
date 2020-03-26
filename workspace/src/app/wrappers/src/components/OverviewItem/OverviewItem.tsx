import React from "react";

const OverviewItem = ({
    children,
    className = '',
    densityHigh = false,
    hasSpacing = false,
    ...restProps
}: any) => {
    return (
        <div
            {...restProps}
            className={
                'ecc-overviewitem__item ' +
                (densityHigh ? 'ecc-overviewitem__item--highdensity ' : '') +
                (hasSpacing ? 'ecc-overviewitem__item--hasspacing ' : '') +
                className
            }
        >
            {children}
        </div>
    )
}

export default OverviewItem;
