import { NotAvailable } from "gui-elements-deprecated";
import React from "react";

import { URIInfo } from "./URIInfo";

export const ThingDescription = ({ id }) => {
    const fallbackInfo = <NotAvailable inline label="No description available." />;
    return <URIInfo uri={id} field="description" fallback={fallbackInfo} />;
};
