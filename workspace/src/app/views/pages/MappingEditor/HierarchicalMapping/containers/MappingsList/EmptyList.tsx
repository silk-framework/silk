import React from "react";
import { CardContent, Notification } from "@eccenca/gui-elements";

const EmptyList = () => {
    return (
        <CardContent>
            <Notification>No existing mapping rules.</Notification>
        </CardContent>
    );
};

export default EmptyList;
