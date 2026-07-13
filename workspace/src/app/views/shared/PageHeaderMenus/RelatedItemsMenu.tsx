import React from "react";
import { useTranslation } from "react-i18next";
import { RelatedItems } from "../RelatedItems/RelatedItems";
import { HeaderPopoverButton } from "./HeaderPopoverButton";

interface IProps {
    projectId?: string;
    taskId?: string;
    /** Forwarded to `RelatedItems`, reload trigger for iframe message events. */
    messageEventReloadTrigger?: (messageId: string) => boolean;
}

/** Page header button showing the related items of the current task in a popover. */
export const RelatedItemsMenu = (props: IProps) => {
    const [t] = useTranslation();
    return (
        <HeaderPopoverButton
            icon="operation-link"
            title={t("RelatedItems.title", "Related items")}
            data-test-id="header-related-items-menu"
        >
            <RelatedItems {...props} />
        </HeaderPopoverButton>
    );
};
