import React from "react";
import { PropertyValuePair, PropertyValue, PropertyName, Label, Spacing } from "@eccenca/gui-elements";
import { MAPPING_RULE_TYPE_COMPLEX_URI, MAPPING_RULE_TYPE_URI } from "../../utils/constants";
import getPathsRecursive from "../../utils/getUriPaths";
import buildRuleFormulaStats from "../../utils/buildRuleFormulaStats";
import RuleFormulaStatistics from "../RuleFormulaStatistics";
import ComplexDeleteButton from "../../elements/buttons/ComplexeDeleteButton";
import { IconButton, NotAvailable } from "@eccenca/gui-elements";
import { useGetRuleOperatorPlugins } from "../../../../../../hooks/useGetOperatorPlugins";

interface ObjectUriPatternProps {
    uriRule: any;
    onRemoveUriRule: () => void;
    openMappingEditor: () => void;
    showLabel?: boolean;
}

const ObjectUriPattern = ({ uriRule, onRemoveUriRule, openMappingEditor, showLabel = true }: ObjectUriPatternProps) => {
    const { getPluginDetailLabel } = useGetRuleOperatorPlugins();
    const { type, pattern } = uriRule;

    let uriPattern = <NotAvailable label="automatic default pattern" tooltip={""} noTag />;

    let uriPatternLabel = "URI pattern";
    let tooltipText = "Create URI formula";

    let removeButton = <ComplexDeleteButton onDelete={onRemoveUriRule} />;

    if (type === MAPPING_RULE_TYPE_URI) {
        uriPattern = <code>{pattern}</code>;
        tooltipText = "Convert URI pattern to URI formula";
    } else if (type === MAPPING_RULE_TYPE_COMPLEX_URI) {
        const paths = getPathsRecursive(uriRule.operator);
        const stats = buildRuleFormulaStats({
            paths,
            operator: uriRule.operator,
            getPluginDetailLabel,
        });

        tooltipText = "Edit URI formula";
        uriPatternLabel = "URI formula";

        uriPattern = (
            <RuleFormulaStatistics
                introLabel="URI uses"
                renderPaths={() => <code>{paths.join(", ")}</code>}
                {...stats}
            />
        );
    } else {
        removeButton = <></>;
    }

    return (
        <div className="ecc-silk-mapping__rulesviewer__idpattern">
            <div className="ecc-silk-mapping__rulesviewer__comment">
                <PropertyValuePair singleColumn className="ecc-silk-mapping__rulesviewer__attribute">
                    {showLabel ? (
                        <PropertyName className="ecc-silk-mapping__rulesviewer__attribute-label">
                            <Label
                                text={uriPatternLabel}
                                emphasis={"strong"}
                                additionalElements={
                                    <>
                                        <IconButton
                                            name="item-edit"
                                            data-test-id="complex-rule-edit-button"
                                            onClick={openMappingEditor}
                                            text={tooltipText}
                                            small
                                        />
                                        {removeButton}
                                    </>
                                }
                                isLayoutForElement={"span"}
                            />
                        </PropertyName>
                    ) : null}
                    <PropertyValue className="ecc-silk-mapping__rulesviewer__attribute-info">
                        {uriPattern}
                    </PropertyValue>
                </PropertyValuePair>
            </div>
            <Spacing size="small" />
        </div>
    );
};

export default ObjectUriPattern;
