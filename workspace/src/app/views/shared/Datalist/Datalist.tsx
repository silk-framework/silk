import React from "react";
import { Loading } from "../Loading/Loading";
import { OverviewItemList, Notification } from "@gui-elements/index";
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
        emptyListMessage = t("common.messages.noItems", { items: "Resource" });
    }

    if (isLoading) {
        return <Loading description={t("DataList.loading", "Loading data.")} />;
    } else if (isEmpty) {
        return emptyContainer || <Notification>{emptyListMessage}</Notification>;
    }

    return <OverviewItemList {...otherProps}>{children}</OverviewItemList>;
}
