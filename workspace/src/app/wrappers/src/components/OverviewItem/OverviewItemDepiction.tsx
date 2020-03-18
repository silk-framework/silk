import React from "react";

const OverviewItemDepiction = ({
    children,
    className = '',
    ...restProps
}: any) => {
    return (
        <div {...restProps} className={'ecc-overviewitem__depiction '+className}>
            {children}
        </div>
    )
}

export default OverviewItemDepiction;
