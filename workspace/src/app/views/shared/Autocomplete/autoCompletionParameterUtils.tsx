// Displays the 'search in workspace' option in the list.
import { OverviewItem, OverviewItemDescription, OverviewItemLine } from "@gui-elements/src/components/OverviewItem";
import { CLASSPREFIX as eccguiprefix } from "@gui-elements/src/configuration/constants";
import { Icon } from "@gui-elements/index";
import React from "react";

/** Returns a function to be used in an AutoComplete widget */
export const createNewItemRendererFactory = (itemTextRenderer: (query: string) => string, iconName?: string) => (
    query: string,
    active: boolean
) => {
    return (
        <OverviewItem className={active ? `${eccguiprefix}-overviewitem__item--active` : ""} key={query} densityHigh>
            <OverviewItemDescription>
                <OverviewItemLine>
                    {iconName && <Icon name={iconName} small={true} />}
                    <span> {itemTextRenderer(query)}</span>
                </OverviewItemLine>
            </OverviewItemDescription>
        </OverviewItem>
    );
};
