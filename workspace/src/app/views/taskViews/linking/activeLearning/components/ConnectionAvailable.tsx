import React from "react";
import { Toolbar, ToolbarSection } from "@eccenca/gui-elements";

/** A dashed line. */
export const DashedLine = () => <div className="h-0 w-auto border-t border-dashed border-border" />;

export interface ConnectionAvailableProps {
    /**
     * Action buttons (or other content) displayed when the connection element is hovered.
     */
    actions: React.JSX.Element;
}

const ConnectionAvailable = ({ actions }: ConnectionAvailableProps) => {
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
