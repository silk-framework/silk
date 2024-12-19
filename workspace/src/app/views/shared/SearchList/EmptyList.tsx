import React from "react";
import {
    Card,
    ClassNames,
    OverviewItem,
    OverviewItemActions,
    Depiction,
    OverviewItemDescription,
    OverviewItemLine,
} from "@eccenca/gui-elements";

function EmptyList({
    depiction, // use large icon here
    textInfo,
    textCallout,
    actionButtons,
}) {
    return (
        <Card isOnlyLayout className={ClassNames.Intent.INFO}>
            <OverviewItem hasSpacing>
                {depiction && <Depiction image={depiction} ratio="1:1" backgroundColor="dark" padding="medium" />}
                <OverviewItemDescription>
                    {textInfo && <OverviewItemLine>{textInfo}</OverviewItemLine>}
                    {textCallout && <OverviewItemLine>{textCallout}</OverviewItemLine>}
                </OverviewItemDescription>
                {actionButtons && <OverviewItemActions>{actionButtons}</OverviewItemActions>}
            </OverviewItem>
        </Card>
    );
}

export default EmptyList;
