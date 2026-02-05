import React from "react";
import { PropertyValuePair, PropertyValue, PropertyName, Label, Spacing } from "@eccenca/gui-elements";
import { ThingName } from "./ThingName";
import { ThingDescription } from "./ThingDescription";
import TargetCardinality from "./TargetCardinality";
import { TextToggler } from "../components/TextToggler";

const TargetProperty = ({ mappingTargetUri, isObjectMapping, isAttribute = false }) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__targetProperty">
            <TargetCardinality isAttribute={isAttribute} isObjectMapping={isObjectMapping} editable={false} />
            <Spacing size={"small"} />
            <PropertyValuePair singleColumn className="ecc-silk-mapping__rulesviewer__attribute">
                <PropertyName className="ecc-silk-mapping__rulesviewer__attribute-label">
                    <Label emphasis={"strong"} text={"Target property"} />
                </PropertyName>
                <PropertyValue>
                    <ThingName id={mappingTargetUri} />{" "}
                    <code>
                        {"<"}
                        {mappingTargetUri}
                        {">"}
                    </code>{" "}
                    <TextToggler text={<ThingDescription id={mappingTargetUri} />} />
                </PropertyValue>
            </PropertyValuePair>
            <Spacing size={"small"} />
        </div>
    );
};

export default TargetProperty;
