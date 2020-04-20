import React from "react";

function OverviewItem({
    children,
    className = '',
    densityHigh = false,
    hasSpacing = false,
    ...otherProps
}: any) {

    const item = (
        <div
            {...otherProps}
            className={
                'ecc-overviewitem__item ' +
                (densityHigh ? 'ecc-overviewitem__item--highdensity ' : '') +
                (hasSpacing ? 'ecc-overviewitem__item--hasspacing ' : '') +
                className
            }
        >
            {children}
        </div>
    );

    let accessibilityParameters = {};
    if (
        typeof otherProps.onClick !== 'undefined' ||
        typeof otherProps.onKeyDown !== 'undefined'
    ) {
        accessibilityParameters['tabIndex'] = 0;
    }
    if (
        typeof otherProps.onClick !== 'undefined' &&
        typeof otherProps.onKeyDown !== 'undefined'
    ) {
        accessibilityParameters['role'] = "button";
    }

    return React.cloneElement(
            item,
            accessibilityParameters
    );
}

export default OverviewItem;
