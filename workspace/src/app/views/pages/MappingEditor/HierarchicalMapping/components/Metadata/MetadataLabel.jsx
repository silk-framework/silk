import React from "react";
import { PropertyValue, PropertyName, Label } from "@eccenca/gui-elements";

const MetadataLabel = ({ label, hasDescription }) => {
    return (
        <>
            {hasDescription ? (
                <PropertyName className="ecc-silk-mapping__rulesviewer__label">
                    <Label text={label} emphasis={"strong"} />
                </PropertyName>
            ) : (
                <>
                    <PropertyName>
                        <Label text={"Mapping label"} emphasis={"strong"} />
                    </PropertyName>
                    <PropertyValue className="ecc-silk-mapping__rulesviewer__label">{label}</PropertyValue>
                </>
            )}
        </>
    );
};

export default MetadataLabel;
