import { MenuItem } from "@gui-elements/index";
import React from "react";

/** Returns a function to be used in an AutoComplete widget for rendering custom elements based on the query string.
 *
 * @param itemTextRenderer The text that should be displayed for the new custom item suggestion.
 * @param iconName Optional icon to show left to the text.
 */
export const createNewItemRendererFactory = (itemTextRenderer: (query: string) => string, iconName?: string) => {
    // Return custom render function
    return (query: string, active: boolean, handleClick: React.MouseEventHandler<HTMLElement>) => {
        return (
            <MenuItem
                icon={iconName}
                active={active}
                key={query}
                onClick={handleClick}
                text={itemTextRenderer(query)}
            />
        );
    };
};
