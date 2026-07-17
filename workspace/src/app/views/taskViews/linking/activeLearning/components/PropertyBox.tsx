import React from "react";
import {
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemActions,
    IconButton,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

export interface PropertyBoxProps extends Omit<React.HTMLAttributes<HTMLDivElement>, "children"> {
    propertyName: string;
    propertyTooltip?: string;
    exampleValues?: React.JSX.Element;
    exampleTooltip?: string;
    /** handler to forward a filter function */
    onFilter?: () => any;
    filtered?: boolean;
}

export const PropertyBox = ({
    propertyName,
    propertyTooltip,
    exampleValues,
    exampleTooltip,
    onFilter,
    filtered,
}: PropertyBoxProps) => {
    const [t] = useTranslation();
    return (
        <div className="diapp-linking-learningdata__propertybox">
            <OverviewItem>
                <OverviewItemDescription>
                    <OverviewItemLine title={!!propertyTooltip ? propertyTooltip : undefined} small>
                        {propertyName}
                    </OverviewItemLine>
                    {!!exampleValues && (
                        <OverviewItemLine title={!!exampleTooltip ? exampleTooltip : undefined}>
                            {exampleValues}
                        </OverviewItemLine>
                    )}
                </OverviewItemDescription>
                {onFilter && !filtered && (
                    <OverviewItemActions>
                        <IconButton name="operation-filter" text={t("common.action.filterOn")} onClick={onFilter} />
                    </OverviewItemActions>
                )}
            </OverviewItem>
        </div>
    );
};
