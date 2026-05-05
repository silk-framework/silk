import React from "react";
import { PropertyValuePair, PropertyValue, PropertyName, Label, Spacing } from "@eccenca/gui-elements";
import ExampleView from "../containers/MappingRule/ExampleView";

const ExampleTarget = ({ uriRuleId }) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__examples">
            <PropertyValuePair singleColumn className="ecc-silk-mapping__rulesviewer__attribute">
                <PropertyName className="ecc-silk-mapping__rulesviewer__attribute-label">
                    <Label text={"Examples of target data"} emphasis={"strong"} />
                </PropertyName>
                <PropertyValue>
                    <ExampleView id={uriRuleId} />
                </PropertyValue>
            </PropertyValuePair>
            <Spacing size={"small"} />
        </div>
    );
};

export default ExampleTarget;
