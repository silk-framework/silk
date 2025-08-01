import React from "react";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { ILinkingRule, ILinkingTaskParameters, optionallyLabelledParameterToValue } from "./linking.types";
import { useTranslation } from "react-i18next";
import { IViewActions } from "../../plugins/PluginRegistry";
import RuleEditor from "../../shared/RuleEditor/RuleEditor";
import { requestRuleOperatorPluginsDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import utils from "./LinkingRuleEditor.utils";
import ruleUtils from "../shared/rules/rule.utils";
import {
    IRuleOperatorNode,
    RULE_EDITOR_NOTIFICATION_INSTANCE,
    RuleSaveNodeError,
    RuleSaveResult,
    RuleValidationError,
} from "../../shared/RuleEditor/RuleEditor.typings";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import linkingRuleRequests, {
    fetchLinkSpec,
    partialAutoCompleteLinkingInputPaths,
    updateLinkageRule,
} from "./LinkingRuleEditor.requests";
import { PathWithMetaData } from "../shared/rules/rule.typings";
import { IAutocompleteDefaultResponse, TaskPlugin } from "@ducks/shared/typings";
import { FetchError, FetchResponse } from "../../../services/fetch/responseInterceptor";
import { LinkingRuleEvaluation } from "./evaluation/LinkingRuleEvaluation";
import { LinkingRuleCacheInfo } from "./LinkingRuleCacheInfo";
import { DatasetCharacteristics } from "../../shared/typings";
import { requestDatasetCharacteristics } from "@ducks/shared/requests";
import Loading from "../../shared/Loading";
import {
    RuleEditorNodeParameterValue,
    ruleEditorNodeParameterValue,
} from "../../../views/shared/RuleEditor/model/RuleEditorModel.typings";
import { invalidValueResult } from "../../../views/shared/RuleEditor/view/ruleNode/ruleNode.utils";
import { diErrorMessage } from "@ducks/error/typings";
import { Notification, highlighterUtils, StickyNote } from "@eccenca/gui-elements";
import { IPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import {languageFilterRegex, PathInputOperatorContext} from "../../shared/RuleEditor/view/ruleNode/PathInputOperator";

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
    const langPref = React.useRef<string>(prefLang)
    langPref.current = prefLang
    // Meta data and label data structures for input paths
    const sourcePathMetaData = React.useRef<PathWithMetaData[]>([]);
    const sourcePathLabels = React.useRef<Map<string, string>>(new Map());
    const targetPathMetaData = React.useRef<PathWithMetaData[]>([]);
    const targetPathLabels = React.useRef<Map<string, string>>(new Map());
    // In which language the path labels are available
    const [pathLabelsAvailableForLang, setPathLabelsAvailableForLang] = React.useState<string | undefined>()
    const [loading, setLoading] = React.useState(true);
    const [initError, setInitError] = React.useState<any | undefined>(undefined);
    const pendingRequests = React.useRef(2);
    const hideGreyListedParameters =
        (
            new URLSearchParams(window.location.search).get(HIDE_GREY_LISTED_OPERATORS_QUERY_PARAMETER) ?? ""
        ).toLowerCase() === "true";
    const optionalContext = React.useContext(LinkingRuleEditorOptionalContext);

    React.useEffect(() => {
        fetchAllPaths()
    }, [projectId, linkingTaskId, prefLang]);

    const reducePendingRequestCount = () => {
        pendingRequests.current = pendingRequests.current - 1;
        if (pendingRequests.current <= 0) {
            setLoading(false);
        }
    };

    const handleInitError = React.useCallback((error: any) => {
        setInitError(error);
    }, []);

    /** Fetches the labels of either the source or target data source and sets them in the corresponding label map. */
    const fetchPaths = async (sourceOrTarget: "source" | "target") => {
        try {
            const paths = await linkingRuleRequests.fetchLinkingCachedPaths(
                projectId,
                linkingTaskId,
                sourceOrTarget,
                true,
                prefLang,
            );
            const createLabelMap = (paths: PathWithMetaData[]): Map<string, string> => {
                const valueToPaths: [string, string][] = paths
                    .filter((p) => p.label)
                    .map((p) => {
                        let value = p.value;
                        // We want to have the actual property without language filter
                        if (languageFilterRegex.test(value)) {
                            value = value.replace(languageFilterRegex, "");
                        }
                        return [value, p.label!];
                    });
                return new Map(valueToPaths);
            };
            if (sourceOrTarget === "source") {
                sourcePathMetaData.current = paths.data as PathWithMetaData[];
                sourcePathLabels.current = createLabelMap(sourcePathMetaData.current);
            } else {
                targetPathMetaData.current = paths.data as PathWithMetaData[];
                targetPathLabels.current = createLabelMap(targetPathMetaData.current);
            }
        } finally {
            reducePendingRequestCount();
        }
    };

    const fetchAllPaths = async () => {
        await fetchPaths("source");
        await fetchPaths("target");
        setPathLabelsAvailableForLang(prefLang)
    }

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
                    err,
                    { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
                );
                setInitError(err);
            }
        }
    };

    const inputPathAutoCompletion =
        (inputType: "source" | "target") =>
        async (term: string, limit: number): Promise<IAutocompleteDefaultResponse[]> => {
            let results: (IAutocompleteDefaultResponse & { valueType?: string })[] =
                inputType === "source" ? sourcePathMetaData.current : targetPathMetaData.current;
            const searchWords = highlighterUtils.extractSearchWords(term, true);
            if (searchWords.length) {
                results = results.filter((path) => {
                    const searchText = `${path.value} ${path.valueType} ${path.label ?? ""}`.toLowerCase();
                    return highlighterUtils.matchesAllWords(searchText, searchWords);
                });
            } else if (results[0]?.value !== "") {
                results.unshift({ value: "", label: `<${t("common.words.emptyPath")}>` });
            }
            return results.slice(0, limit);
        };

    // Return for either a source or target path what type of path it is.
    const inputPathPluginPathType = React.useCallback(
        (pluginId: "sourcePathInput" | "targetPathInput", path: string): string | undefined => {
            const pathsMetaData =
                pluginId === "sourcePathInput" ? sourcePathMetaData.current : targetPathMetaData.current;
            const pathMetaData = pathsMetaData.find((p) => p.value && path.endsWith(p.value));
            return pathMetaData?.valueType;
        },
        [],
    );

    const inputPathLabel = React.useCallback(
        (pluginId: "sourcePathInput" | "targetPathInput", path: string): string | undefined => {
            const pathLabels = pluginId === "sourcePathInput" ? sourcePathLabels.current : targetPathLabels.current;
            return pathLabels.get(path);
        },
        [],
    );

    /** Fetches the list of operators that can be used in a linking task. */
    const fetchLinkingRuleOperatorDetails = async () => {
        try {
            const responseData = (await requestRuleOperatorPluginsDetails(false)).data;
            let operatorPlugins = Object.values(responseData);
            if (hideGreyListedParameters) {
                operatorPlugins = operatorPlugins.filter((pd) => !pd.categories.includes("Excel"));
            }
            return operatorPlugins;
        } catch (err) {
            registerError(
                "LinkingRuleEditor_fetchLinkingRuleOperatorDetails",
                t("taskViews.linkRulesEditor.errors.fetchLinkingRuleOperatorDetails.msg"),
                err,
                { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
            );
            setInitError(err);
        }
    };

    /** Save the rule. */
    const saveLinkageRule = async (
        ruleOperatorNodes: IRuleOperatorNode[],
        stickyNotes: StickyNote[] = [],
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
                        nodeErrors,
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
        description: t("RuleEditor.sidebar.parameter.weightDesc"),
        type: "int",
        advanced: true,
        defaultValue: "1",
        orderIdx: -0.5,
    });

    const thresholdParameterSpec = (pluginDetails: IPluginDetails) => {
        const varyingSpec = () => {
            switch (pluginDetails.distanceMeasureRange) {
                case "normalized":
                    return {
                        description: t(
                            "RuleEditor.sidebar.parameter.thresholdDesc.normalized",
                            "The maximum distance. This distance measure is normalized, i.e., the threshold must be between 0 (exact match) and 1 (no similarity).",
                        ),
                        label: t("RuleEditor.sidebar.parameter.thresholdLabel", "Threshold"),
                        requiredLabel: t("RuleEditor.sidebar.parameter.thresholdRequired.normalized", "required 0..1"),
                    };
                case "unbounded":
                    return {
                        description: t(
                            "RuleEditor.sidebar.parameter.thresholdDesc.unbounded",
                            "The maximum distance. Distances start at 0 (exact match) and increase the more different the values may be.",
                        ),
                        label: t("RuleEditor.sidebar.parameter.thresholdLabel", "Threshold"),
                        requiredLabel: t("RuleEditor.sidebar.parameter.thresholdRequired.unbounded", "required 0..∞"),
                    };
                default:
                    return {
                        label: "",
                        description: "",
                    };
            }
        };

        const customValidation = (distanceMeasureRange) => (parameterValue: RuleEditorNodeParameterValue) => {
            const value = ruleEditorNodeParameterValue(parameterValue);
            const float = Number(value);
            if (Number.isNaN(float)) return invalidValueResult(t("form.validations.float"));
            if (distanceMeasureRange === "normalized" && (float > 1 || float < 0))
                return invalidValueResult(t("form.validations.threshold.normalized"));
            if (distanceMeasureRange === "unbounded" && float < 0)
                return invalidValueResult(t("form.validations.threshold.unbounded"));
            return { valid: true };
        };

        return ruleUtils.parameterSpecification({
            ...varyingSpec(),
            type: "float",
            defaultValue: "0.0",
            customValidation: customValidation(pluginDetails.distanceMeasureRange),
            distanceMeasureRange: pluginDetails.distanceMeasureRange,
            orderIdx: -1,
        });
    };

    const fetchPartialAutoCompletionResult = React.useCallback(
        (inputType: "source" | "target") =>
            async (inputString: string, cursorPosition: number): Promise<IPartialAutoCompleteResult | undefined> => {
                try {
                    const result = await partialAutoCompleteLinkingInputPaths(
                        projectId,
                        linkingTaskId,
                        inputType,
                        inputString,
                        cursorPosition,
                        200,
                        langPref.current
                    );
                    return result.data;
                } catch (err) {
                    registerError(
                        "LinkingRuleEditor_partialAutoCompletion",
                        t("taskViews.linkRulesEditor.errors.partialPathAutoCompletion.msg"),
                        err,
                    );
                }
            },
        [],
    );

    const sourcePathInput = () =>
        ruleUtils.inputPathOperator(
            "sourcePathInput",
            t("RuleEditor.sidebar.operator.sourcePathLabel", "Source path"),
            ["Source path"],
            t("RuleEditor.sidebar.operator.sourcePathDesc", "The value path of the source input of the linking task."),
            inputPathAutoCompletion("source"),
        );

    const targetPathInput = () =>
        ruleUtils.inputPathOperator(
            "targetPathInput",
            t("RuleEditor.sidebar.operator.targetPathLabel", "Target path"),
            ["Target path"],
            t("RuleEditor.sidebar.operator.targetPathDesc", "The value path of the target input of the linking task."),
            inputPathAutoCompletion("target"),
        );

    const tabs = React.useMemo(() => {
        return [
            ruleUtils.sidebarTabs.all,
            utils.inputPathTab(projectId, linkingTaskId, sourcePathInput(), "source", (ex) =>
                registerError(
                    "linking-rule-editor-fetch-source-paths",
                    t("taskViews.linkRulesEditor.errors.fetchLinkingPaths.msg"),
                    ex,
                    { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
                ),
            ),
            utils.inputPathTab(projectId, linkingTaskId, targetPathInput(), "target", (ex) =>
                registerError(
                    "linking-rule-editor-fetch-source-paths",
                    t("taskViews.linkRulesEditor.errors.fetchLinkingPaths.msg"),
                    ex,
                    { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
                ),
            ),
            ruleUtils.sidebarTabs.transform,
            ruleUtils.sidebarTabs.comparison,
            ruleUtils.sidebarTabs.aggregation,
        ];
    }, []);

    const fetchDatasetCharacteristics = async (taskData: TaskPlugin<ILinkingTaskParameters> | undefined) => {
        const result = new Map<string, DatasetCharacteristics>();
        if (taskData) {
            const parameters = taskData.parameters;
            const sourceTaskId = optionallyLabelledParameterToValue(
                optionallyLabelledParameterToValue(parameters.source).inputId,
            );
            const targetTaskId = optionallyLabelledParameterToValue(
                optionallyLabelledParameterToValue(parameters.target).inputId,
            );
            const sourceDatasetRequest = requestDatasetCharacteristics(projectId, sourceTaskId);
            const targetDatasetRequest = requestDatasetCharacteristics(projectId, targetTaskId);
            const handleRequest = async (
                requestFuture: Promise<FetchResponse<DatasetCharacteristics>>,
                pathPluginId: "sourcePathInput" | "targetPathInput",
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
                            ex,
                            { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
                        );
                    }
                }
            };
            await handleRequest(sourceDatasetRequest, "sourcePathInput");
            await handleRequest(targetDatasetRequest, "targetPathInput");
        }
        return result;
    };

    if (initError) {
        return <Notification danger={true}>{diErrorMessage(initError)}</Notification>;
    }

    if (loading) {
        return <Loading />;
    }

    return <PathInputOperatorContext.Provider value={{
        pathLabelsAvailableForLang
    }}>
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
                partialAutoCompletion={fetchPartialAutoCompletionResult}
                saveRule={saveLinkageRule}
                getStickyNotes={utils.getStickyNotes}
                convertRuleOperator={ruleUtils.convertRuleOperator}
                viewActions={viewActions}
                convertToRuleOperatorNodes={utils.convertLinkingTaskToRuleOperatorNodes}
                additionalRuleOperators={[sourcePathInput(), targetPathInput()]}
                addAdditionParameterSpecifications={(pluginDetails) => {
                    switch (pluginDetails.pluginType) {
                        case "ComparisonOperator":
                            return pluginDetails.distanceMeasureRange === "boolean"
                                ? [["weight", weightParameterSpec]]
                                : [
                                      ["threshold", thresholdParameterSpec(pluginDetails)],
                                      ["weight", weightParameterSpec],
                                  ];
                        case "AggregationOperator":
                            return [["weight", weightParameterSpec]];
                        default:
                            return [];
                    }
                }}
                validateConnection={ruleUtils.validateConnection}
                tabs={tabs}
                additionalToolBarComponents={() => [
                    <LinkingRuleCacheInfo key="LinkingRuleCacheInfo" projectId={projectId} taskId={linkingTaskId} />,
                ]}
                showRuleOnly={!!optionalContext.showRuleOnly}
                hideMinimap={!!optionalContext.hideMinimap}
                zoomRange={optionalContext.zoomRange ?? [0.25, 1.5]}
                initialFitToViewZoomLevel={optionalContext.initialFitToViewZoomLevel}
                instanceId={instanceId}
                fetchDatasetCharacteristics={fetchDatasetCharacteristics}
                pathMetaData={{
                    inputPathPluginPathType,
                    inputPathLabel,
                }}
            />
        </LinkingRuleEvaluation>
    </PathInputOperatorContext.Provider>;
};
