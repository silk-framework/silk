import React from "react";
import { Spacing, Tag, TagList } from "@eccenca/gui-elements";
import { IPluginDetails } from "@ducks/common/typings";
import { getOperatorLabel } from "../../../../views/taskViews/linking/evaluation/tabView/LinkingEvaluationViewUtils";
import { tagColor } from "../../../../views/shared/RuleEditor/view/sidebar/RuleOperator";
import { useTranslation } from "react-i18next";

interface OperatorLabelProps {
    tagPluginType: "Input" | "Transform" | "Comparison" | "Aggregation" | "Source path" | "Target path";
    operator: any;
    operatorPlugins: Array<IPluginDetails>;
    children: React.ReactNode;
}

export const OperatorLabel: React.FC<OperatorLabelProps> = React.memo(
    ({ tagPluginType, operator, operatorPlugins, children }) => {
        const [t] = useTranslation();
        const emptyPathLabel = `<${t("common.words.emptyPath")}>`;
        return (
            <TagList className="diapp-linking-evaluation__operatorlabel">
                <Tag key={operator.id} backgroundColor={tagColor(tagPluginType)}>
                    {getOperatorLabel(operator, operatorPlugins, emptyPathLabel)}
                </Tag>
                {children}
            </TagList>
        );
    },
);
