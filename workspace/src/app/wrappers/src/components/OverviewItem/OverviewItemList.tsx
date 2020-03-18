import React from "react";

const OverviewItemList = ({
    children,
    className = '',
    ...restProps
}: any) => {
    return (
        <div {...restProps} className={'ecc-overviewitem__list '+className}>
            {children}
        </div>
    )
}

export default OverviewItemList;
