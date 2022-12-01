import { CardContent, Info } from "gui-elements-deprecated";
import React from "react";

const EmptyList = () => {
    return (
        <CardContent>
            <Info vertSpacing border>
                No existing mapping rules.
            </Info>
        </CardContent>
    );
};

export default EmptyList;
