import React from "react";

const OverviewItemDescription = ({
    children,
    className = '',
    ...restProps
}: any) => {
    return (
        <div {...restProps} className={'ecc-overviewitem__description '+className}>
            {children}
        </div>
    )
}

export default OverviewItemDescription;
