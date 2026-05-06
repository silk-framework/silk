import { NotAvailable } from "@eccenca/gui-elements";
import { URIInfo } from "./URIInfo";
import React from "react";

export const ThingDescription = ({ id }) => {
    const fallbackInfo = <NotAvailable label="No description available." tooltip={""} noTag />;
    return <URIInfo uri={id} field="description" fallback={fallbackInfo} />;
};
