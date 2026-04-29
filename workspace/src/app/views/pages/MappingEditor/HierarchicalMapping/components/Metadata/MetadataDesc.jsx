import React from "react";
import { PropertyValue, PropertyName, Label } from "@eccenca/gui-elements";
import { TextToggler } from "../TextToggler";

const MetadataDesc = ({ description, hasLabel }) => {
    return (
        <>
            {hasLabel ? (
                <PropertyValue className="ecc-silk-mapping__rulesviewer__comment">{description}</PropertyValue>
            ) : (
                <>
                    <PropertyName>
                        <Label text={"Mapping description"} emphasis={"strong"} />
                    </PropertyName>
                    <PropertyName className="ecc-silk-mapping__rulesviewer__comment">
                        <TextToggler text={description} />
                    </PropertyName>
                </>
            )}
        </>
    );
};

export default MetadataDesc;
