import React from "react";
import {
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
} from "@eccenca/gui-elements";

export interface PropertyBoxProps extends Omit<React.HTMLAttributes<HTMLDivElement>, "children"> {
    propertyName: string;
    propertyTooltip?: string;
    exampleValues?: JSX.Element;
    exampleTooltip?: string;
    /** handler to forward a filter function */
    onFilter?: () => any;
}

export const PropertyBox = ({
    propertyName,
    propertyTooltip,
    exampleValues,
    exampleTooltip,
    onFilter
}: PropertyBoxProps) => {
    return (
        <OverviewItem>
            <OverviewItemDescription onClick={onFilter}>
                <OverviewItemLine title={!!propertyTooltip ? propertyTooltip : undefined} small>
                    {propertyName}
                </OverviewItemLine>
                {!!exampleValues && (
                    <OverviewItemLine title={!!exampleTooltip ? exampleTooltip : undefined}>
                        {exampleValues}
                    </OverviewItemLine>
                )}
            </OverviewItemDescription>
        </OverviewItem>
    );
}
