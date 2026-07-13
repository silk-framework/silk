import React from "react";
import { ContextOverlay, IconButton } from "@eccenca/gui-elements";
import { ValidIconName } from "@eccenca/gui-elements/src/components/Icon/canonicalIconNames";

interface IProps {
    /** Icon of the toggler button in the page header. */
    icon: ValidIconName;
    /** Tooltip/aria label of the toggler button. */
    title: string;
    /** Popover content. Only mounted while the popover is open. */
    children: React.ReactNode;
    "data-test-id"?: string;
}

/** Icon button for the page header actions area that opens its content in a popover.
 * Used to keep per-page meta panels (related items, activities) one click away
 * without a permanent side area. */
export const HeaderPopoverButton = ({ icon, title, children, "data-test-id": dataTestId }: IProps) => {
    return (
        <ContextOverlay
            placement="bottom-end"
            content={<div className="max-h-[min(70vh,40rem)] w-[min(28rem,90vw)] overflow-y-auto">{children}</div>}
        >
            <IconButton name={icon} text={title} data-test-id={dataTestId} />
        </ContextOverlay>
    );
};
