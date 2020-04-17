import React from "react";
import Spinner from "@wrappers/blueprint/spinner";
import { Button, OverviewItemList } from '@wrappers/index'


export function Datalist({children, isLoading = false, isEmpty = false, emptyContainer = null, ...otherProps}) {
    if (isLoading) {
        return <Spinner/>;
    } else if (isEmpty) {
        return emptyContainer || <p>No Resource Found</p>
    }

    return <OverviewItemList {...otherProps}>{children}</OverviewItemList>

}
