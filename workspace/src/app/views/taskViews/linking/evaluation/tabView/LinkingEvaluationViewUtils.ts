import fetch from "../../../../../services/fetch";
import { legacyLinkingEndpoint } from "../../../../../utils/getApiEndpoint";
import { FetchResponse } from "../../../../../services/fetch/responseInterceptor";
import {
    EvaluationLinkInputValue,
    LinkEvaluationFilters,
    LinkEvaluationSortBy,
    LinkingEvaluationResult,
    LinkStats,
    ReferenceLinkType,
} from "./typings";
import { IAggregationOperator, IComparisonOperator, ILinkingRule } from "../../linking.types";
import { IPluginDetails } from "@ducks/common/typings";
import { IPathInput, ITransformOperator } from "views/taskViews/shared/rules/rule.typings";
import { TreeNodeInfo } from "@blueprintjs/core";

/**
 * Get evaluated links.
 * @param projectId  Project ID
 * @param taskId     Linking Task ID
 * @param pagination Pagination object
 * @param query
 * @param filters
 * @param sortBy
 */
export const getEvaluatedLinks = async (
    projectId: string,
    taskId: string,
    pagination: { current: number; total: number; limit: number },
    query: string = "",
    filters: LinkEvaluationFilters[] = [],
    sortBy: LinkEvaluationSortBy[] = [],
    includeReferenceLinks: boolean = false
): Promise<FetchResponse<{ links: LinkingEvaluationResult[]; linkRule: ILinkingRule; stats: LinkStats }> | undefined> =>
    fetch({
        method: "POST",
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${taskId}/evaluate`),
        body: {
            query,
            offset: (pagination.current - 1) * pagination.limit,
            limit: pagination.limit,
            filters,
            sortBy,
            includeReferenceLinks,
        },
    });

//update reference link state to either positive, negative or unlabelled
export const updateReferenceLink = async (
    projectId: string,
    taskId: string,
    source: string,
    target: string,
    linkType: ReferenceLinkType
): Promise<FetchResponse<any>> =>
    fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${taskId}/referenceLink`),
        method: linkType === "unlabeled" ? "DELETE" : "PUT",
        query: {
            linkType: linkType === "unlabeled" ? undefined : linkType,
            source,
            target,
        },
    });

export const getOperatorPath = (operatorInput: any): Array<{ id: string; path: string }> => {
    if (operatorInput.path) {
        return [{ path: operatorInput.path, id: operatorInput.id }];
    }

    return operatorInput.inputs.reduce((acc, val) => {
        acc = [...acc, ...getOperatorPath(val)];
        return acc;
    }, [] as Array<{ id: string; path: string }>);
};

export const getLinkRuleInputPaths = (operatorInput: any) =>
    ["sourceInput", "targetInput"].reduce(
        (linkRuleInputPaths, inputPathType) => {
            const label = inputPathType.replace("Input", "");
            //no intermittent inputs but are direct paths
            if (!operatorInput[inputPathType]?.inputs) {
                linkRuleInputPaths[label][operatorInput[inputPathType].path] = operatorInput[inputPathType].id;
            } else {
                operatorInput[inputPathType].inputs.forEach((i) => {
                    getOperatorPath(i).forEach(({ path, id }) => {
                        linkRuleInputPaths[label][path] = id;
                    });
                });
            }
            return linkRuleInputPaths;
        },
        { source: {}, target: {} } as EvaluationLinkInputValue<string>
    );

export const getOperatorLabel = (operator: any, operatorPlugins: IPluginDetails[]): string | undefined => {
    switch (operator.type) {
        case "Aggregation":
            return operatorPlugins.find((plugin) => plugin.pluginId === (operator as IAggregationOperator).aggregator)
                ?.title;
        case "Comparison":
            return operatorPlugins.find((plugin) => plugin.pluginId === (operator as IComparisonOperator).metric)
                ?.title;
        case "transformInput":
            return operatorPlugins.find((plugin) => plugin.pluginId === (operator as ITransformOperator).function)
                ?.title;
        case "pathInput":
            return (operator as IPathInput).path;
        default:
            return undefined;
    }
};

export const getParentNodes = (tree: TreeNodeInfo, nodeId: string, ancestors = [] as string[]): Array<string> => {
    let match = [] as string[];
    const traverse = (tree: TreeNodeInfo, nodeId: string, ancestors = [] as string[]) => {
        if (tree.id === nodeId) {
            match = ancestors;
        }

        if (!tree.childNodes?.length) return [];

        tree.childNodes.forEach((subTree) => {
            traverse(subTree, nodeId, [...ancestors, tree.id as string]);
        });
    };
    traverse(tree, nodeId);
    return match;
};
