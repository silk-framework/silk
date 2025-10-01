import React from "react";
import { Loading } from "../Loading/Loading";
import { OverviewItemList, Notification } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

interface IProps {
    children: React.JSX.Element | React.JSX.Element[];
    isLoading: boolean;
    isEmpty: boolean;
    emptyContainer?: React.JSX.Element;
    emptyListMessage?: string;
    [key: string]: any;
}

export function Datalist({
    children,
    isLoading = false,
    isEmpty = false,
    emptyListMessage = "",
    emptyContainer,
    ...otherProps
}: IProps) {
    const [t] = useTranslation();
    if (!emptyListMessage) {
        emptyListMessage = t("common.messages.noItems", { items: "Resource" });
    }

    if (isLoading) {
        return <Loading delay={0} description={t("DataList.loading", "Loading data.")} />;
    } else if (isEmpty) {
        return emptyContainer || <Notification>{emptyListMessage}</Notification>;
    }

    return <OverviewItemList {...otherProps}>{children}</OverviewItemList>;
}
