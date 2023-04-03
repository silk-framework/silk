import { FetchResponse } from "../../../../../services/fetch/responseInterceptor";
import React from "react";
import fetch from "../../../../../services/fetch";
import { legacyTransformEndpoint } from "../../../../../utils/getApiEndpoint";
import { EvaluatedEntityOperator, EvaluatedRuleEntityResult, EvaluatedRuleOperator } from "./typing";
import { Icon, Tag, TagList, TreeNodeInfo } from "@eccenca/gui-elements";
import { IPluginDetails } from "@ducks/common/typings";
import { OperatorLabel } from "../../../../../views/taskViews/shared/evaluations/OperatorLabel";

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

export const newNodeValues = (values: EvaluatedEntityOperator["values"]) => (
    <TagList>
        {!values.length && <Icon intent="warning" name="state-warning" tooltipText="No value" />}
        {values.map((v, i) => (
            <Tag key={i} round emphasis="stronger" interactive>
                {v}
            </Tag>
        ))}
    </TagList>
);

export const newNode = (
    rule: EvaluatedRuleOperator,
    values: EvaluatedEntityOperator["values"],
    operatorPlugins: Array<IPluginDetails>
): TreeNodeInfo<Partial<{ root: boolean; label: string }>> => {
    return {
        id: rule.id,
        hasCaret: false,
        isExpanded: true,
        label: (
            <OperatorLabel tagPluginType={operatorMapping[rule.type]} operator={rule} operatorPlugins={operatorPlugins}>
                {newNodeValues(values)}
            </OperatorLabel>
        ),
        nodeData: {
            root: false,
        },
    };
};
