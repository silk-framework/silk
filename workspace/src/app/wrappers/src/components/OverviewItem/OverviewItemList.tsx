import React from "react";

function OverviewItemList({
    children,
    className = '',
    densityHigh = false,
    hasDivider = false,
    hasSpacing = false,
    columns=1,
    ...restProps
}: any) {
    return (
        <ol
            {...restProps}
            className={
                'ecc-overviewitem__list ' +
                (densityHigh ? 'ecc-overviewitem__list--highdensity ' : '') +
                (hasDivider ? 'ecc-overviewitem__list--hasdivider ' : '') +
                (hasSpacing ? 'ecc-overviewitem__list--hasspacing ' : '') +
                (columns > 1 ? 'ecc-overviewitem__list--hascolumns ' : '') + // TODO: add number
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
