import React from "react";
import { PropertyValuePair, PropertyValue, PropertyName, Label, Spacing } from "@eccenca/gui-elements";
import { PropertyTypeLabel } from "../Property/PropertyTypeLabel";
import { PropertyTypeDescription } from "../Property/PropertyTypeDescription";

const propertyTypeLabel = (valueType) => {
    // Adds optional properties of the property type to the label, e.g. language tag
    if (typeof valueType.lang === "string") {
        return ` (${valueType.lang})`;
    }
    if (typeof valueType.uri === "string") {
        return ` (${valueType.uri})`;
    }
    return "";
};

const ValueNodeType = ({ valueType, nodeType }) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__propertyType">
            <PropertyValuePair singleColumn className="ecc-silk-mapping__rulesviewer__attribute">
                <PropertyName className="ecc-silk-mapping__rulesviewer__attribute-label">
                    <Label text={"Data type"} emphasis={"strong"} />
                </PropertyName>
                <PropertyValue>
                    <PropertyTypeLabel name={nodeType} appendedText={propertyTypeLabel(valueType)} />
                    <PropertyTypeDescription name={nodeType} />
                </PropertyValue>
            </PropertyValuePair>
            <Spacing size={"small"} />
        </div>
    );
};

export default ValueNodeType;
