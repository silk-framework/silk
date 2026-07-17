import React from "react";
import { PropertyValuePair, PropertyValue, PropertyName, Label, Spacing } from "@eccenca/gui-elements";

const ObjectSourcePath = ({ children }) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__sourcePath">
            <PropertyValuePair singleColumn className="ecc-silk-mapping__rulesviewer__attribute">
                <PropertyName className="ecc-silk-mapping__rulesviewer__attribute-label">
                    <Label text={"Value path"} emphasis={"strong"} />
                </PropertyName>
                <PropertyValue className="ecc-silk-mapping__rulesviewer__attribute-info">{children}</PropertyValue>
            </PropertyValuePair>
            <Spacing size={"small"} />
        </div>
    );
};

export default ObjectSourcePath;
