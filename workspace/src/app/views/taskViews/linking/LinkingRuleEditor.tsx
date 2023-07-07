import React from "react";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { ILinkingRule, ILinkingTaskParameters, optionallyLabelledParameterToValue } from "./linking.types";
import { useTranslation } from "react-i18next";
import { IViewActions } from "../../plugins/PluginRegistry";
import RuleEditor from "../../shared/RuleEditor/RuleEditor";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import utils from "./LinkingRuleEditor.utils";
import ruleUtils from "../shared/rules/rule.utils";
import {
    IRuleOperatorNode,
    RuleSaveNodeError,
    RuleSaveResult,
    RuleValidationError,
} from "../../shared/RuleEditor/RuleEditor.typings";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import linkingRuleRequests, { fetchLinkSpec, updateLinkageRule } from "./LinkingRuleEditor.requests";
import { PathWithMetaData } from "../shared/rules/rule.typings";
import { IAutocompleteDefaultResponse, TaskPlugin } from "@ducks/shared/typings";
import { FetchError, FetchResponse } from "../../../services/fetch/responseInterceptor";
import { LinkingRuleEvaluation } from "./evaluation/LinkingRuleEvaluation";
import { LinkingRuleCacheInfo } from "./LinkingRuleCacheInfo";
import { IStickyNote } from "../shared/task.typings";
import { extractSearchWords, matchesAllWords } from "@eccenca/gui-elements/src/components/Typography/Highlighter";
import { DatasetCharacteristics } from "../../shared/typings";
import { requestDatasetCharacteristics } from "@ducks/shared/requests";
import Loading from "../../shared/Loading";

export interface LinkingRuleEditorProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    linkingTaskId: string;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
    /** The instance of the linking editor. This needs to be unique if multiple instances of the linking editor are displayed on the same page. */
    instanceId: string;
}

interface LinkingRuleEditorOptionalContextProps {
    /** When enabled only the rule is shown without side- and toolbar and any other means to edit the rule. */
    showRuleOnly?: boolean;
    /** When enabled the mini map is not displayed. */
    hideMinimap?: boolean;
    /** Defines minimum and maximum of the available zoom levels */
    zoomRange?: [number, number];
    /** When this is defined it will show this rule instead of loading it from the backend. */
    linkingRule?: TaskPlugin<ILinkingTaskParameters>;
    /** After the initial fit to view, zoom to the specified Zoom level to avoid showing too small nodes. */
    initialFitToViewZoomLevel?: number;
}
export const LinkingRuleEditorOptionalContext = React.createContext<LinkingRuleEditorOptionalContextProps>({});

const HIDE_GREY_LISTED_OPERATORS_QUERY_PARAMETER = "hideGreyListedParameters";

const NUMBER_OF_LINKS_TO_SHOW = 5;

/** Editor for creating and changing linking rule operator trees. */
export const LinkingRuleEditor = ({ projectId, linkingTaskId, viewActions, instanceId }: LinkingRuleEditorProps) => {
    // The linking task parameters
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const prefLang = useSelector(commonSel.localeSelector);
    // Label for input paths
    const sourcePathLabels = React.useRef<PathWithMetaData[]>([]);
    const targetPathLabels = React.useRef<PathWithMetaData[]>([]);
    const [loading, setLoading] = React.useState(true)
    const pendingRequests = React.useRef(2)
    const hideGreyListedParameters =
        (
            new URLSearchParams(window.location.search).get(HIDE_GREY_LISTED_OPERATORS_QUERY_PARAMETER) ?? ""
        ).toLowerCase() === "true";
    const optionalContext = React.useContext(LinkingRuleEditorOptionalContext);

    React.useEffect(() => {
        fetchPaths("source");
        fetchPaths("target");
    }, [projectId, linkingTaskId, prefLang]);

    const reducePendingRequestCount = () => {
        pendingRequests.current = pendingRequests.current - 1
        if(pendingRequests.current <= 0) {
            setLoading(false)
        }
    }

    /** Fetches the labels of either the source or target data source and sets them in the corresponding label map. */
    const fetchPaths = async (sourceOrTarget: "source" | "target") => {
        try {
            const paths = await linkingRuleRequests.fetchLinkingCachedPaths(
                projectId,
                linkingTaskId,
                sourceOrTarget,
                true,
                prefLang
            );
            if (sourceOrTarget === "source") {
                sourcePathLabels.current = paths.data as PathWithMetaData[];
            } else {
                targetPathLabels.current = paths.data as PathWithMetaData[];
            }
        } finally {
            reducePendingRequestCount()
        }
    };
    /** Fetches the parameters of the linking task */
    const fetchTaskData = async (projectId: string, taskId: string) => {
        if (optionalContext.linkingRule) {
            return optionalContext.linkingRule;
        } else {
            try {
                const taskData = (await fetchLinkSpec(projectId, taskId, true, prefLang)).data;
                return taskData;
            } catch (err) {
                registerError(
                    "LinkingRuleEditor_fetchLinkingTask",
                    t("taskViews.linkRulesEditor.errors.fetchTaskData.msg"),
                    err
                );
            }
        }
    };

    const inputPathAutoCompletion =
        (inputType: "source" | "target") =>
        async (term: string, limit: number): Promise<IAutocompleteDefaultResponse[]> => {
            let results: (IAutocompleteDefaultResponse & { valueType?: string })[] =
                inputType === "source" ? sourcePathLabels.current : targetPathLabels.current;
            const searchWords = extractSearchWords(term, true);
            if (searchWords.length) {
                results = results.filter((path) => {
                    const searchText = `${path.value} ${path.valueType} ${path.label ?? ""}`.toLowerCase();
                    return matchesAllWords(searchText, searchWords);
                });
            } else if (results[0]?.value !== "") {
                results.unshift({ value: "", label: `<${t("common.words.emptyPath")}>` });
            }
            return results.slice(0, limit);
        };

    // Return for either a source or target path what type of path it is.
    const inputPathPluginPathType =
        (pluginId: "sourcePathInput" | "targetPathInput", path: string): string | undefined => {
            const pathsMetaData = pluginId === "sourcePathInput" ? sourcePathLabels.current : targetPathLabels.current;
            const pathMetaData = pathsMetaData.find(p => p.value && path.endsWith(p.value))
            return pathMetaData?.valueType
        };

    /** Fetches the list of operators that can be used in a linking task. */
    const fetchLinkingRuleOperatorDetails = async () => {
        try {
            let operatorPlugins = Object.values((await requestRuleOperatorPluginDetails(false)).data);
            if (hideGreyListedParameters) {
                operatorPlugins = operatorPlugins.filter((pd) => !pd.categories.includes("Excel"));
            }
            return operatorPlugins;
        } catch (err) {
            registerError(
                "LinkingRuleEditor_fetchLinkingRuleOperatorDetails",
                t("taskViews.linkRulesEditor.errors.fetchLinkingRuleOperatorDetails.msg"),
                err
            );
        }
    };

    /** Save the rule. */
    const saveLinkageRule = async (
        ruleOperatorNodes: IRuleOperatorNode[],
        stickyNotes: IStickyNote[] = []
    ): Promise<RuleSaveResult> => {
        try {
            const ruleTree = utils.constructLinkageRuleTree(ruleOperatorNodes);
            const originalRule = (await fetchLinkSpec(projectId, linkingTaskId, false)).data.parameters
                .rule as ILinkingRule;
            await updateLinkageRule(projectId, linkingTaskId, {
                ...originalRule,
                operator: ruleTree,
                layout: ruleUtils.ruleLayout(ruleOperatorNodes),
                uiAnnotations: {
                    stickyNotes,
                },
            });
            return {
                success: true,
            };
        } catch (err) {
            if ((err as RuleValidationError).isRuleValidationError) {
                return err;
            } else {
                if (err.isHttpError && err.httpStatus === 400 && Array.isArray((err as FetchError).body?.issues)) {
                    const fetchError = err as FetchError;
                    const nodeErrors: RuleSaveNodeError[] = fetchError.body.issues.map((issue) => ({
                        nodeId: issue.id,
                        message: issue.message,
                    }));
                    return new RuleValidationError(
                        t("taskViews.linkRulesEditor.errors.saveLinkageRule.msg"),
                        nodeErrors
                    );
                } else {
                    return {
                        success: false,
                        errorMessage: `${t("taskViews.linkRulesEditor.errors.saveLinkageRule.msg")}${
                            err.message ? ": " + err.message : ""
                        }`,
                    };
                }
            }
        }
    };

    // FIXME: Add i18n to parameter specs
    const weightParameterSpec = ruleUtils.parameterSpecification({
        label: t("RuleEditor.sidebar.parameter.weightLabel", "Weight"),
        description: t(
            "RuleEditor.sidebar.parameter.weightDesc",
            "The weight parameter can be used by the parent aggregation when combining " +
                "its input values. Only certain aggregations will consider weighted inputs. Examples are the weighted average " +
                "aggregation, quadraticMean and geometricMean."
        ),
        type: "int",
        advanced: true,
        defaultValue: "1",
    });

    const thresholdParameterSpec = ruleUtils.parameterSpecification({
        label: t("RuleEditor.sidebar.parameter.thresholdLabel", "Threshold"),
        description: t(
            "RuleEditor.sidebar.parameter.thresholdDesc",
            "The maximum distance. For normalized distance measures, the threshold should be between 0.0 and 1.0."
        ),
        type: "float",
        defaultValue: "0.0",
    });

    const sourcePathInput = () =>
        ruleUtils.inputPathOperator(
            "sourcePathInput",
            t("RuleEditor.sidebar.operator.sourcePathLabel", "Source path"),
            ["Source path"],
            t("RuleEditor.sidebar.operator.sourcePathDesc", "The value path of the source input of the linking task."),
            inputPathAutoCompletion("source")
        );

    const targetPathInput = () =>
        ruleUtils.inputPathOperator(
            "targetPathInput",
            t("RuleEditor.sidebar.operator.targetPathLabel", "Target path"),
            ["Target path"],
            t("RuleEditor.sidebar.operator.targetPathDesc", "The value path of the target input of the linking task."),
            inputPathAutoCompletion("target")
        );

    const fetchDatasetCharacteristics = async (taskData: TaskPlugin<ILinkingTaskParameters> | undefined) => {
        const result = new Map<string, DatasetCharacteristics>();
        if (taskData) {
            const parameters = taskData.parameters;
            const sourceTaskId = optionallyLabelledParameterToValue(
                optionallyLabelledParameterToValue(parameters.source).inputId
            );
            const targetTaskId = optionallyLabelledParameterToValue(
                optionallyLabelledParameterToValue(parameters.target).inputId
            );
            const sourceDatasetRequest = requestDatasetCharacteristics(projectId, sourceTaskId);
            const targetDatasetRequest = requestDatasetCharacteristics(projectId, targetTaskId);
            const handleRequest = async (
                requestFuture: Promise<FetchResponse<DatasetCharacteristics>>,
                pathPluginId: "sourcePathInput" | "targetPathInput"
            ) => {
                try {
                    const response = await requestFuture;
                    result.set(pathPluginId, response.data);
                } catch (ex) {
                    // Return 404 if the dataset does not exist or the task is not a dataset
                    if (ex.httpStatus !== 404) {
                        registerError(
                            "LinkingRuleEditor-fetchDatasetCharacteristics",
                            "Dataset characteristics could not be fetched. UI-support for language filters will not be available.",
                            ex
                        );
                    }
                }
            };
            await handleRequest(sourceDatasetRequest, "sourcePathInput");
            await handleRequest(targetDatasetRequest, "targetPathInput");
        }
        return result;
    };

    if(loading) {
        return <Loading />
    }

    return (
        <LinkingRuleEvaluation
            projectId={projectId}
            linkingTaskId={linkingTaskId}
            numberOfLinkToShow={NUMBER_OF_LINKS_TO_SHOW}
        >
            <RuleEditor<TaskPlugin<ILinkingTaskParameters>, IPluginDetails>
                projectId={projectId}
                taskId={linkingTaskId}
                fetchRuleData={fetchTaskData}
                fetchRuleOperators={fetchLinkingRuleOperatorDetails}
                saveRule={saveLinkageRule}
                getStickyNotes={utils.getStickyNotes}
                convertRuleOperator={ruleUtils.convertRuleOperator}
                viewActions={viewActions}
                convertToRuleOperatorNodes={utils.convertLinkingTaskToRuleOperatorNodes}
                additionalRuleOperators={[sourcePathInput(), targetPathInput()]}
                addAdditionParameterSpecifications={(pluginDetails) => {
                    switch (pluginDetails.pluginType) {
                        case "ComparisonOperator":
                            return [
                                ["threshold", thresholdParameterSpec],
                                ["weight", weightParameterSpec],
                            ];
                        case "AggregationOperator":
                            return [["weight", weightParameterSpec]];
                        default:
                            return [];
                    }
                }}
                validateConnection={ruleUtils.validateConnection}
                tabs={[
                    ruleUtils.sidebarTabs.all,
                    utils.inputPathTab(projectId, linkingTaskId, sourcePathInput(), "source", (ex) =>
                        registerError(
                            "linking-rule-editor-fetch-source-paths",
                            t("taskViews.linkRulesEditor.errors.fetchLinkingPaths.msg"),
                            ex
                        )
                    ),
                    utils.inputPathTab(projectId, linkingTaskId, targetPathInput(), "target", (ex) =>
                        registerError(
                            "linking-rule-editor-fetch-source-paths",
                            t("taskViews.linkRulesEditor.errors.fetchLinkingPaths.msg"),
                            ex
                        )
                    ),
                    ruleUtils.sidebarTabs.transform,
                    ruleUtils.sidebarTabs.comparison,
                    ruleUtils.sidebarTabs.aggregation,
                ]}
                additionalToolBarComponents={() => [
                    <LinkingRuleCacheInfo key="LinkingRuleCacheInfo" projectId={projectId} taskId={linkingTaskId} />,
                ]}
                showRuleOnly={!!optionalContext.showRuleOnly}
                hideMinimap={!!optionalContext.hideMinimap}
                zoomRange={optionalContext.zoomRange ?? [0.25, 1.5]}
                initialFitToViewZoomLevel={optionalContext.initialFitToViewZoomLevel}
                instanceId={instanceId}
                fetchDatasetCharacteristics={fetchDatasetCharacteristics}
                inputPathPluginPathType={inputPathPluginPathType}
            />
        </LinkingRuleEvaluation>
    );
};
