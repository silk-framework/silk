import React from "react";
import Spinner from "@wrappers/blueprint/spinner";
import { OverviewItemList } from "@wrappers/index";

export function Datalist({
    children,
    isLoading = false,
    isEmpty = false,
    emptyContainer = null,
    emptyListMessage = "No Resource Found",
    ...otherProps
}) {
    if (isLoading) {
        return <Spinner />;
    } else if (isEmpty) {
        return emptyContainer || <p>{emptyListMessage}</p>;
    }

    return <OverviewItemList {...otherProps}>{children}</OverviewItemList>;
}
