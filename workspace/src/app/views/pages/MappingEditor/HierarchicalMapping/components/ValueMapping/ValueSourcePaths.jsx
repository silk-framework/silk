import React from "react";
import { PropertyValuePair, PropertyValue, PropertyName, Label, Spacing } from "@eccenca/gui-elements";
import getUriOperatorsRecursive from "../../utils/getUriOperators";
import { useGetRuleOperatorPlugins } from "../../../../../../hooks/useGetOperatorPlugins";

const ValueSourcePaths = ({ paths, operator, children }) => {
    const operators = getUriOperatorsRecursive(operator, []);
    const { getPluginDetailLabel } = useGetRuleOperatorPlugins();
    return (
        <div className="ecc-silk-mapping__rulesviewer__sourcePath">
            <PropertyValuePair singleColumn className="ecc-silk-mapping__rulesviewer__attribute">
                <PropertyName className="ecc-silk-mapping__rulesviewer__attribute-label">
                    <Label text={"Value formula"} emphasis={"strong"} additionalElements={children} />
                </PropertyName>
                <PropertyValue className="ecc-silk-mapping__rulesviewer__attribute-info">
                    Formula uses {paths.length} value path{paths.length > 1 ? "s" : ""}:{" "}
                    <code>
                        {"<"}
                        {paths.join(">, <")}
                        {">"}
                    </code>
                    <br />
                    and {operators.length} operator function{operators.length > 1 ? "s" : ""}:{" "}
                    <code>{operators.map(getPluginDetailLabel).join(", ")}</code>.
                </PropertyValue>
            </PropertyValuePair>
            <Spacing size={"small"} />
        </div>
    );
};

export default ValueSourcePaths;
