import { ThingIcon } from "../../components/ThingIcon";
import _ from "lodash";
import { SourcePath } from "../../components/SourcePath";
import React from "react";
import RuleTypes from "../../elements/RuleTypes";
import { getRuleLabel } from "../../utils/getRuleLabel";
import { OverviewItemDescription, OverviewItemLine, OverflowText } from "@eccenca/gui-elements";

class MappingRuleRow extends React.Component {
    render() {
        const { mappingTarget, metadata, rules, sourcePath, type } = this.props;

        const label = _.get(metadata, "label", "");
        const ruleLabelData = getRuleLabel({ label, uri: mappingTarget.uri });
        const statusType = _.get(this.props, "status[0].type", false);
        const statusMsg = _.get(this.props, "status[0].message", false);
        return (
            <>
                <OverviewItemDescription style={{ width: "40%" }}>
                    <OverviewItemLine className="nodrag ecc-silk-mapping__ruleitem-headline">
                        <OverflowText>
                            <ThingIcon type={type} status={statusType} message={statusMsg} />
                            {ruleLabelData.displayLabel}
                        </OverflowText>
                    </OverviewItemLine>
                    {ruleLabelData.uri && (
                        <OverviewItemLine small className="nodrag">
                            <OverflowText>{ruleLabelData.uri}</OverflowText>
                        </OverviewItemLine>
                    )}
                </OverviewItemDescription>
                <OverviewItemDescription style={{ width: "20%" }}>
                    <OverviewItemLine className="nodrag">
                        <OverflowText>
                            <span className="hide-in-table">DataType:</span>{" "}
                            <RuleTypes
                                rule={{
                                    type,
                                    mappingTarget,
                                    rules,
                                }}
                            />
                        </OverflowText>
                    </OverviewItemLine>
                </OverviewItemDescription>
                <OverviewItemDescription style={{ width: "40%" }}>
                    <OverviewItemLine className="nodrag">
                        <OverflowText>
                            <span className="hide-in-table">from</span>{" "}
                            <SourcePath
                                rule={{
                                    type,
                                    sourcePath,
                                }}
                            />
                        </OverflowText>
                    </OverviewItemLine>
                </OverviewItemDescription>
            </>
        );
    }
}

export default MappingRuleRow;
