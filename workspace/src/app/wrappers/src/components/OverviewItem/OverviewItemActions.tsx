import React from "react";

const OverviewItemActions = ({
    children,
    className = '',
    ...restProps
}: any) => {
    return (
        <div {...restProps} className={'ecc-overviewitem__actions '+className}>
            {children}
        </div>
    )
}

export default OverviewItemActions;
