import React from "react";
import { HoverToggler, Tag, Toolbar, ToolbarSection } from "@eccenca/gui-elements";

interface ArrowProps {
    color?: string;
}

/** Arrow giond to the left. */
export const ArrowLeft = ({ color = "#000" }: ArrowProps) => {
    return <div className={"diapp-linking-connectionenabled__arrow-left"} style={{ color }} />;
};

/** Arrow going to the right. */
export const ArrowRight = ({ color = "#000" }: ArrowProps) => {
    return <div className={"diapp-linking-connectionenabled__arrow-right"} style={{ color }} />;
};

export interface ConnectionEnabledProps {
    /**
     * Action buttons (or other content) displayed when the connection element is hovered.
     */
    actions?: React.JSX.Element;
    /**
     * Color used to display the connection.
     */
    color?: string;
    /**
     * Label display as Tag on the connection.
     */
    label: string;
}

const ConnectionEnabled = ({ label, actions, color }: ConnectionEnabledProps) => {
    const connection = (
        <Toolbar style={{ height: "100%" }} className={"diapp-linking-connectionenabled__arrow"}>
            <ToolbarSection canGrow={true}>
                <ArrowLeft color={color} />
            </ToolbarSection>
            <ToolbarSection>
                <Tag backgroundColor={color}>{label}</Tag>
            </ToolbarSection>
            <ToolbarSection canGrow={true}>
                <ArrowRight color={color} />
            </ToolbarSection>
        </Toolbar>
    );
    return !!actions ? (
        <HoverToggler
            className={"diapp-linking-connectionenabled"}
            style={{ height: "100%" }}
            baseContent={connection}
            baseContentProps={{ style: { width: "100%" } }}
            hoverContent={actions}
        />
    ) : (
        connection
    );
};

export default ConnectionEnabled;
