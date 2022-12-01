import React from "react";

import { PropertyTypeInfo } from "./PropertyTypeInfo";

export const PropertyTypeLabel = ({ name, appendedText }) => (
    <PropertyTypeInfo name={name} option="label" appendedText={appendedText} />
);
