import React from "react";
import { Toolbar, ToolbarSection } from "@eccenca/gui-elements";

/** A dashed line. */
export const DashedLine = () => {
    return (
        <div
            style={{
                height: "0px",
                width: "auto",
                borderTop: "2px dashed lightgray",
            }}
        />
    );
};

export interface ConnectionAvailableProps {
    /**
     * Action buttons (or other content) displayed when the connection element is hovered.
     */
    actions: JSX.Element;
    /**
     * Color used to display the connection.
     */
    color?: string;
}

const ConnectionAvailable = ({ actions, color }: ConnectionAvailableProps) => {
    return (
        <Toolbar style={{ height: "100%" }} noWrap>
            <ToolbarSection canGrow={true}>
                <DashedLine />
            </ToolbarSection>
            <ToolbarSection>{actions}</ToolbarSection>
            <ToolbarSection canGrow={true}>
                <DashedLine />
            </ToolbarSection>
        </Toolbar>
    );
};

export default ConnectionAvailable;
