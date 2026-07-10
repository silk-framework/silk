import React from "react";
import { PropertyValuePair, PropertyValue, PropertyName, Label, Spacing } from "@eccenca/gui-elements";
import RuleFormulaStatistics from "../RuleFormulaStatistics";
import buildRuleFormulaStats from "../../utils/buildRuleFormulaStats";
import { useGetRuleOperatorPlugins } from "../../../../../../hooks/useGetOperatorPlugins";

const ValueSourcePaths = ({ paths, operator, children }) => {
    const { getPluginDetailLabel } = useGetRuleOperatorPlugins();
    const stats = buildRuleFormulaStats({ paths, operator, getPluginDetailLabel });
    return (
        <div className="ecc-silk-mapping__rulesviewer__sourcePath">
            <PropertyValuePair singleColumn className="ecc-silk-mapping__rulesviewer__attribute">
                <PropertyName className="ecc-silk-mapping__rulesviewer__attribute-label">
                    <Label
                        text={"Value formula"}
                        emphasis={"strong"}
                        additionalElements={children}
                        isLayoutForElement={"span"}
                    />
                </PropertyName>
                <PropertyValue className="ecc-silk-mapping__rulesviewer__attribute-info">
                    <RuleFormulaStatistics
                        introLabel="Formula uses"
                        renderPaths={() => (
                            <code>
                                {"<"}
                                {paths.join(">, <")}
                                {">"}
                            </code>
                        )}
                        {...stats}
                    />
                </PropertyValue>
            </PropertyValuePair>
            <Spacing size={"small"} />
        </div>
    );
};

export default ValueSourcePaths;
