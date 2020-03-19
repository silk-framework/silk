import React from "react";

const OverviewItemList = ({
    children,
    className = '',
    densityHigh = false,
    hasDivider = false,
    hasSpacing = false,
    ...restProps
}: any) => {
    return (
        <ol
            {...restProps}
            className={
                'ecc-overviewitem__list ' +
                (densityHigh ? 'ecc-overviewitem__list--highdensity ' : '') +
                (hasDivider ? 'ecc-overviewitem__list--hasdivider ' : '') +
                (hasSpacing ? 'ecc-overviewitem__list--hasspacing ' : '') +
                className
            }
        >
            {
                React.Children.map(children, (child, i) => {
                    return <li>{ child }</li>
                })
            }
        </ol>
    )
}

export default OverviewItemList;
