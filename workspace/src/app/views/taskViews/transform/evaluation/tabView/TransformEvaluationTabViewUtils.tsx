import { FetchResponse } from "../../../../../services/fetch/responseInterceptor";
import React from "react";
import fetch from "../../../../../services/fetch";
import { legacyTransformEndpoint } from "../../../../../utils/getApiEndpoint";
import { EvaluatedEntityOperator, EvaluatedRuleEntityResult, EvaluatedRuleOperator } from "./typing";
import { Icon, Tag, TagList, TreeNodeInfo } from "@eccenca/gui-elements";
import { IPluginDetails } from "@ducks/common/typings";
import { OperatorLabel } from "../../../../../views/taskViews/shared/evaluations/OperatorLabel";
import { useTranslation } from "react-i18next";

export const getEvaluatedEntities = async (
    projectId: string,
    taskId: string,
    ruleId: string,
    limit: number,
    showOnlyEntitiesWithUris: boolean
): Promise<FetchResponse<EvaluatedRuleEntityResult>> =>
    fetch({
        method: "GET",
        url: legacyTransformEndpoint(`/tasks/${projectId}/${taskId}/rule/${ruleId}/evaluated`),
        body: {
            showOnlyEntitiesWithUris: Number(showOnlyEntitiesWithUris),
        },
    });

const operatorMapping = {
    pathInput: "Source path",
    transformInput: "Transform",
} as const;

interface NodeTagValuesProps {
    values: EvaluatedEntityOperator["values"];
    error?: string;
    cutAfter?: number;
}

export const NodeTagValues: React.FC<NodeTagValuesProps> = React.memo(
    ({ values, error, cutAfter = 3, ...otherTagProps }) => {
        const [t] = useTranslation();
        const remainingNodes =
            values.length > cutAfter ? (
                <Tag className="diapp-linking-evaluation__cutinfo" round intent="info">
                    +{values.length - cutAfter}
                </Tag>
            ) : (
                <></>
            );

        return (
            <TagList>
                {!values.length && !error && (
                    <Tag
                        htmlTitle={error || t("common.messages.noValuesAvailable")}
                        round={true}
                        intent={"neutral"}
                        emphasis={"weak"}
                    >
                        N/A
                    </Tag>
                )}
                {!values.length && error && <Icon intent="warning" name="state-warning" tooltipText={error} />}
                {values.slice(0, cutAfter).map((v, i) => (
                    <Tag key={i} round emphasis="stronger" interactive {...otherTagProps}>
                        {v}
                    </Tag>
                ))}
                {remainingNodes}
            </TagList>
        );
    }
);

export const newNode = ({
    rule,
    values,
    operatorPlugins,
    error,
}: {
    rule: EvaluatedRuleOperator;
    values: EvaluatedEntityOperator["values"];
    operatorPlugins: Array<IPluginDetails>;
    error?: string;
}): TreeNodeInfo<Partial<{ root: boolean; label: string }>> => {
    return {
        id: `id_${rule.id}`,
        hasCaret: false,
        isExpanded: true,
        label: (
            <OperatorLabel tagPluginType={operatorMapping[rule.type]} operator={rule} operatorPlugins={operatorPlugins}>
                <NodeTagValues values={values} error={error} />
            </OperatorLabel>
        ),
        nodeData: {
            root: false,
        },
    };
};
