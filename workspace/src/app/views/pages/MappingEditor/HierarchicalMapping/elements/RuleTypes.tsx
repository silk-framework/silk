import React from "react";
import _ from "lodash";
import { NotAvailable } from "gui-elements-deprecated";
import { ThingName } from "../components/ThingName";

import { MAPPING_RULE_TYPE_ROOT } from "../utils/constants";
import { MAPPING_RULE_TYPE_COMPLEX, MAPPING_RULE_TYPE_DIRECT, MAPPING_RULE_TYPE_OBJECT } from "../utils/constants";
import { GlobalMappingEditorContext } from "../../contexts/GlobalMappingEditorContext";

const RuleTypes = ({ rule, ...otherProps }) => {
    const mappingEditorContext = React.useContext(GlobalMappingEditorContext);
    switch (rule.type) {
        case MAPPING_RULE_TYPE_ROOT:
        case MAPPING_RULE_TYPE_OBJECT:
            let types: any = _.get(rule, "rules.typeRules", []);
            types = _.isEmpty(types) ? (
                <NotAvailable />
            ) : (
                types
                    .map(({ typeUri }) => <ThingName id={typeUri} key={typeUri} />)
                    .reduce((prev, curr) => [prev, ", ", curr])
            );
            return <span {...otherProps}>{types}</span>;
        case MAPPING_RULE_TYPE_DIRECT:
        case MAPPING_RULE_TYPE_COMPLEX:
            let appendText = _.get(rule, "mappingTarget.valueType.lang", "");
            if (appendText) {
                // add language tag if available
                appendText = ` (${appendText})`;
            }
            let dataTypeLabel: string | React.JSX.Element = _.get(
                rule,
                "mappingTarget.valueType.nodeType",
                <NotAvailable />,
            );
            if (typeof dataTypeLabel === "string") {
                const label = mappingEditorContext.valueTypeLabels.get(dataTypeLabel);
                if (label) {
                    dataTypeLabel = label;
                }
                dataTypeLabel = dataTypeLabel + appendText;
            }
            return <span {...otherProps}>{dataTypeLabel}</span>;
        default:
            return <></>;
    }
};

export default RuleTypes;
