import React from "react";

const OverviewItem = ({
    children,
    className = '',
    ...restProps
}: any) => {
    return (
        <div {...restProps} className={'ecc-overviewitem__item '+className}>
            {children}
        </div>
    )
}

export default OverviewItem;
