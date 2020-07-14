import React from "react";
import { Loading } from "../Loading/Loading";
import { OverviewItemList } from "@wrappers/index";
import { useTranslation } from "react-i18next";

export function Datalist({
    children,
    isLoading = false,
    isEmpty = false,
    emptyContainer = null,
    emptyListMessage = "",
    ...otherProps
}) {
    const [t] = useTranslation();
    if (!emptyListMessage) {
        emptyListMessage = t("common.messages.noResource", "No Resource Found");
    }

    if (isLoading) {
        return <Loading description="Loading data." />;
    } else if (isEmpty) {
        return emptyContainer || <p>{emptyListMessage}</p>;
    }

    return <OverviewItemList {...otherProps}>{children}</OverviewItemList>;
}
