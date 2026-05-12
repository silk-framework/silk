import React from "react";
import { PropertyValuePair, PropertyValue, PropertyName, Label, Spacing } from "@eccenca/gui-elements";
import { ThingName } from "../ThingName";
import { ThingDescription } from "../ThingDescription";
import { TextToggler } from "../TextToggler";

const ObjectTypeRules = ({ typeRules }) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__targetEntityType">
            <PropertyValuePair singleColumn className="ecc-silk-mapping__rulesviewer__attribute">
                <PropertyName className="ecc-silk-mapping__rulesviewer__attribute-label">
                    <Label
                        emphasis={"strong"}
                        text={typeRules.length > 1 ? "Target entity types" : "Target entity type"}
                    />
                </PropertyName>
                {typeRules.map((typeRule, idx) => (
                    <PropertyValue key={`TargetEntityType_${idx}`}>
                        <ThingName id={typeRule.typeUri} />{" "}
                        <code>
                            {"<"}
                            {typeRule.typeUri}
                            {">"}
                        </code>{" "}
                        <TextToggler text={<ThingDescription id={typeRule.typeUri} />} />
                    </PropertyValue>
                ))}
            </PropertyValuePair>
            <Spacing size="small" />
        </div>
    );
};

export default ObjectTypeRules;
