import React from "react";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { ILinkingRule, ILinkingTaskParameters } from "./linking.types";
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
import linkingRuleRequests, {
    autoCompleteLinkingInputPaths,
    fetchLinkSpec,
    updateLinkageRule,
} from "./LinkingRuleEditor.requests";
import { PathWithMetaData } from "../shared/rules/rule.typings";
import { IAutocompleteDefaultResponse, TaskPlugin } from "@ducks/shared/typings";
import { FetchError } from "../../../services/fetch/responseInterceptor";
import { LinkingRuleEvaluation } from "./evaluation/LinkingRuleEvaluation";
import { LinkingRuleCacheInfo } from "./LinkingRuleCacheInfo";
import { requestTaskMetadata } from "@ducks/shared/requests";
import { IStickyNote } from "../shared/task.typings";

export interface LinkingRuleEditorProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    linkingTaskId: string;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
}

interface LinkingRuleEditorOptionalContextProps {
    /** When enabled only the rule is shown without side- and toolbar and any other means to edit the rule. */
    showRuleOnly?: boolean;
    /** When enabled the mini map is not displayed. */
    hideMinimap?: boolean;
    /** When this is defined it will show this rule instead of loading it from the backend. */
    linkingRule?: TaskPlugin<ILinkingTaskParameters>;
}
export const LinkingRuleEditorOptionalContext = React.createContext<LinkingRuleEditorOptionalContextProps>({});

const HIDE_GREY_LISTED_OPERATORS_QUERY_PARAMETER = "hideGreyListedParameters";

const NUMBER_OF_LINKS_TO_SHOW = 5;

/** Editor for creating and changing linking rule operator trees. */
export const LinkingRuleEditor = ({ projectId, linkingTaskId, viewActions }: LinkingRuleEditorProps) => {
    // The linking task parameters
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const prefLang = useSelector(commonSel.localeSelector);
    // Label for source paths
    const [sourcePathLabels] = React.useState<Map<string, string>>(new Map());
    const [targetPathLabels] = React.useState<Map<string, string>>(new Map());
    const [editorTitle, setEditorTitle] = React.useState<string | undefined>(undefined);
    const hideGreyListedParameters =
        (
            new URLSearchParams(window.location.search).get(HIDE_GREY_LISTED_OPERATORS_QUERY_PARAMETER) ?? ""
        ).toLowerCase() === "true";
    const optionalContext = React.useContext(LinkingRuleEditorOptionalContext);

    React.useEffect(() => {
        fetchLabels("source");
        fetchLabels("target");
    }, [projectId, linkingTaskId, prefLang]);

    /** Fetches the labels of either the source or target data source and sets them in the corresponding label map. */
    const fetchLabels = async (sourceOrTarget: "source" | "target") => {
        const paths = await linkingRuleRequests.fetchLinkingCachedPaths(
            projectId,
            linkingTaskId,
            sourceOrTarget,
            true,
            prefLang
        );
        let labelMap = sourcePathLabels;
        if (sourceOrTarget === "source") {
            sourcePathLabels.clear();
        } else {
            targetPathLabels.clear();
            labelMap = targetPathLabels;
        }
        paths.data.forEach((path) => {
            if ((path as PathWithMetaData)?.label) {
                const pathWithMetaData = path as PathWithMetaData;
                labelMap.set(pathWithMetaData.value, pathWithMetaData.label!!);
            }
        });
    };
    /** Fetches the parameters of the linking task */
    const fetchTaskData = async (projectId: string, taskId: string) => {
        if (optionalContext.linkingRule) {
            return optionalContext.linkingRule;
        } else {
            try {
                const taskData = (await fetchLinkSpec(projectId, taskId, true, prefLang)).data;
                if (viewActions?.integratedView) {
                    const taskMetaData = (await requestTaskMetadata(taskId, projectId)).data;
                    setEditorTitle(taskMetaData.label);
                }
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
            try {
                const response = await autoCompleteLinkingInputPaths(projectId, linkingTaskId, inputType, term, limit);
                const results = response.data;
                if (term.trim() === "") {
                    results.unshift({ value: "", label: `<${t("common.words.emptyPath")}>` });
                }
                return results;
            } catch (err) {
                registerError(
                    "LinkingRuleEditor_inputPathAutoCompletion",
                    t("taskViews.linkRulesEditor.errors.inputPathAutoCompletion.msg"),
                    err
                );
                return [];
            }
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
        label: "Weight",
        description:
            "The weight parameter can be used by the parent aggregation when combining " +
            "its input values. Only certain aggregations will consider weighted inputs. Examples are the weighted average " +
            "aggregation, quadraticMean and geometricMean.",
        type: "int",
        advanced: true,
        defaultValue: "1",
    });

    const thresholdParameterSpec = ruleUtils.parameterSpecification({
        label: "Threshold",
        description:
            "The maximum distance. For normalized distance measures, the threshold should be between 0.0 and 1.0.",
        type: "float",
        defaultValue: "0.0",
    });

    const sourcePathInput = () =>
        ruleUtils.inputPathOperator(
            "sourcePathInput",
            "Source path",
            "The value path of the source input of the linking task.",
            inputPathAutoCompletion("source")
        );

    const targetPathInput = () =>
        ruleUtils.inputPathOperator(
            "targetPathInput",
            "Target path",
            "The value path of the target input of the linking task.",
            inputPathAutoCompletion("target")
        );

    return (
        <LinkingRuleEvaluation
            projectId={projectId}
            linkingTaskId={linkingTaskId}
            numberOfLinkToShow={NUMBER_OF_LINKS_TO_SHOW}
        >
            <RuleEditor<TaskPlugin<ILinkingTaskParameters>, IPluginDetails>
                editorTitle={editorTitle}
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
                    <LinkingRuleCacheInfo projectId={projectId} taskId={linkingTaskId} />,
                ]}
                showRuleOnly={!!optionalContext.showRuleOnly}
                hideMinimap={!!optionalContext.hideMinimap}
            />
        </LinkingRuleEvaluation>
    );
};
