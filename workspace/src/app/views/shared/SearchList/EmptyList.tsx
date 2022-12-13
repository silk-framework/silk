import {
    Card,
    HelperClasses,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
} from "@eccenca/gui-elements";
import React from "react";

function EmptyList({
    depiction, // use large icon here
    textInfo,
    textCallout,
    actionButtons,
}) {
    return (
        <Card isOnlyLayout className={HelperClasses.Intent.INFO}>
            <OverviewItem hasSpacing>
                {depiction && <OverviewItemDepiction>{depiction}</OverviewItemDepiction>}
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
