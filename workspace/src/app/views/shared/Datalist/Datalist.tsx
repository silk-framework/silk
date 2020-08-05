import React from "react";
import { Loading } from "../Loading/Loading";
import { OverviewItemList } from "@gui-elements/index";

export function Datalist({
    children,
    isLoading = false,
    isEmpty = false,
    emptyContainer = null,
    emptyListMessage = "No Resource Found",
    ...otherProps
}) {
    if (isLoading) {
        return <Loading description="Loading data." />;
    } else if (isEmpty) {
        return emptyContainer || <p>{emptyListMessage}</p>;
    }

    return <OverviewItemList {...otherProps}>{children}</OverviewItemList>;
}
