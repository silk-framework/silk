import React from "react";
import { Spacing, Tag, TagList } from "@eccenca/gui-elements";
import { IPluginDetails } from "@ducks/common/typings";
import { getOperatorLabel } from "../../../../views/taskViews/linking/evaluation/tabView/LinkingEvaluationViewUtils";
import { tagColor } from "../../../../views/shared/RuleEditor/view/sidebar/RuleOperator";

interface OperatorLabelProps {
    tagPluginType: "Input" | "Transform" | "Comparison" | "Aggregation" | "Source path" | "Target path";
    operator: any;
    operatorPlugins: Array<IPluginDetails>;
}

export const OperatorLabel: React.FC<OperatorLabelProps> = React.memo(
    ({ tagPluginType, operator, operatorPlugins, children }) => (
        <TagList>
            <Tag key={operator.id} backgroundColor={tagColor(tagPluginType)}>
                {getOperatorLabel(operator, operatorPlugins)}
            </Tag>
            <Spacing vertical size="tiny" />
            {children}
        </TagList>
    )
);
