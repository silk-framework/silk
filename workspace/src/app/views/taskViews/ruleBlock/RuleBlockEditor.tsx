import React from "react";
import { StickyNote } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { requestRuleOperatorPluginsDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { IProjectTask } from "@ducks/shared/typings";
import { requestTaskData } from "@ducks/shared/requests";
import { requestUpdateProjectTask } from "@ducks/workspace/requests";
import { IViewActions } from "../../plugins/PluginRegistry";
import RuleEditor, { RuleOperatorFetchFnType } from "../../shared/RuleEditor/RuleEditor";
import {
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSideBarFilterTabConfig,
    RULE_EDITOR_NOTIFICATION_INSTANCE,
    RuleSaveResult,
    RuleValidationError,
} from "../../shared/RuleEditor/RuleEditor.typings";
import { CodeAutocompleteFieldPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import ruleUtils from "../shared/rules/rule.utils";
import { FetchError } from "../../../services/fetch/responseInterceptor";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { IRuleBlockModel, IRuleBlockTaskParameters } from "./ruleBlock.types";
import ruleBlockUtils from "./ruleBlock.utils";

export interface RuleBlockEditorProps {
    projectId: string;
    ruleBlockTaskId: string;
    viewActions?: IViewActions;
    instanceId: string;
}

type RuleBlockTaskData = IProjectTask<IRuleBlockTaskParameters>;

/** Editor for reusable rule block tasks. */
export const RuleBlockEditor = ({ projectId, ruleBlockTaskId, viewActions, instanceId }: RuleBlockEditorProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();

    const fetchRuleBlockTask = async (
        currentProjectId: string,
        taskId: string,
    ): Promise<RuleBlockTaskData | undefined> => {
        try {
            return (await requestTaskData<IRuleBlockTaskParameters>(currentProjectId, taskId)).data;
        } catch (err) {
            registerError("RuleBlockEditor_fetchRuleBlockTask", t("taskViews.ruleBlock.errors.fetchTaskData"), err, {
                errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE,
            });
        }
    };

    const fetchTransformRuleOperatorList = async (): Promise<IPluginDetails[] | undefined> => {
        try {
            const response = (await requestRuleOperatorPluginsDetails(true)).data;
            return Object.values(response).filter((plugin) => plugin.pluginType === "TransformOperator");
        } catch (err) {
            registerError(
                "RuleBlockEditor_fetchTransformRuleOperatorDetails",
                t("taskViews.ruleBlock.errors.fetchTransformOperators"),
                err,
                { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
            );
        }
    };

    const inputPortOperator = React.useMemo<IRuleOperator>(
        () => ({
            pluginType: "InputPortOperator",
            pluginId: "inputPort",
            label: t("taskViews.ruleBlock.inputPortOperator"),
            description: t("taskViews.ruleBlock.inputPortOperatorDescription"),
            parameterSpecification: {
                label: ruleUtils.parameterSpecification({
                    label: t("form.field.label"),
                    required: false,
                    orderIdx: 0,
                }),
                description: ruleUtils.parameterSpecification({
                    label: t("common.words.description"),
                    required: false,
                    type: "textArea",
                    orderIdx: 1,
                }),
                exampleValues: ruleUtils.parameterSpecification({
                    label: t("taskViews.ruleBlock.exampleValues"),
                    required: false,
                    type: "code-yaml",
                    orderIdx: 2,
                }),
                displayOrder: ruleUtils.parameterSpecification({
                    label: t("taskViews.ruleBlock.displayOrder"),
                    required: false,
                    type: "int",
                    orderIdx: 3,
                }),
                deprecated: ruleUtils.parameterSpecification({
                    label: t("taskViews.ruleBlock.deprecated"),
                    required: false,
                    type: "boolean",
                    defaultValue: "false",
                    orderIdx: 4,
                }),
            },
            portSpecification: {
                minInputPorts: 0,
                maxInputPorts: 0,
            },
            categories: [t("taskViews.ruleBlock.inputPortsTab")],
            tags: [],
            inputsCanBeSwitched: false,
        }),
        [t],
    );

    const saveRuleBlock = async (
        ruleOperatorNodes: IRuleOperatorNode[],
        stickyNotes: StickyNote[],
        originalTask: RuleBlockTaskData,
    ): Promise<RuleSaveResult> => {
        try {
            const currentModel = originalTask.data.parameters.ruleBlockModel ?? ruleBlockUtils.emptyRuleBlockModel();
            const [operatorNodeMap, rootNodes] = ruleUtils.convertToRuleOperatorNodeMap(ruleOperatorNodes, true);
            const inputPortNodes = ruleOperatorNodes.filter((node) => node.pluginType === "InputPortOperator");
            const { nodeErrors, portDefinitions } = ruleBlockUtils.collectPortDefinitions(
                currentModel.ports,
                inputPortNodes,
                t("taskViews.ruleBlock.errors.invalidDisplayOrder"),
                (portId) => t("taskViews.ruleBlock.errors.conflictingPortDefinitions", { portId }),
            );

            if (nodeErrors.length) {
                return new RuleValidationError(t("taskViews.ruleBlock.errors.invalidPorts"), nodeErrors);
            }

            const updatedModel: IRuleBlockModel = {
                ...currentModel,
                ports: portDefinitions ?? currentModel.ports,
                operatorTree: rootNodes[0]
                    ? ruleUtils.convertRuleOperatorNodeToValueInput(rootNodes[0], operatorNodeMap)
                    : undefined,
                layout: ruleUtils.ruleLayout(ruleOperatorNodes),
                uiAnnotations: {
                    ...(currentModel.uiAnnotations ?? {}),
                    stickyNotes,
                },
            };

            await requestUpdateProjectTask(projectId, ruleBlockTaskId, {
                data: {
                    parameters: {
                        ruleBlockModel: updatedModel,
                    },
                },
            });
            return {
                success: true,
            };
        } catch (err) {
            if ((err as RuleValidationError).isRuleValidationError) {
                return err;
            }
            if (err?.isHttpError && err.httpStatus === 400 && Array.isArray((err as FetchError).body?.issues)) {
                return new RuleValidationError(
                    t("taskViews.ruleBlock.errors.save"),
                    (err as FetchError).body.issues.map((issue) => ({
                        nodeId: issue.id,
                        message: issue.message,
                    })),
                );
            }
            return {
                success: false,
                errorMessage: `${t("taskViews.ruleBlock.errors.save")}${err.message ? ": " + err.message : ""}`,
            };
        }
    };

    const convertToRuleOperatorNodes = (
        ruleBlockTask: RuleBlockTaskData,
        ruleOperator: RuleOperatorFetchFnType,
    ): IRuleOperatorNode[] => {
        const operatorTree = ruleBlockTask.data.parameters.ruleBlockModel?.operatorTree;
        if (!operatorTree) {
            return [];
        }
        const portDefinitions = new Map(
            (ruleBlockTask.data.parameters.ruleBlockModel?.ports ?? []).map((port) => [port.id, port] as const),
        );
        const operatorNodes: IRuleOperatorNode[] = [];
        ruleUtils.extractOperatorNodeFromValueInput(operatorTree, operatorNodes, undefined, ruleOperator);
        operatorNodes.forEach((node) => {
            if (node.pluginType === "InputPortOperator") {
                const portId = ruleBlockUtils.resolvePortId(node);
                const portDefinition = portDefinitions.get(portId);
                node.label = portDefinition?.label || t("taskViews.ruleBlock.inputPortOperator");
                node.description = t("taskViews.ruleBlock.inputPortOperatorDescription");
                node.parameters = {
                    ...node.parameters,
                    label: portDefinition?.label ?? "",
                    description: portDefinition?.description ?? "",
                    exampleValues: portDefinition?.exampleValues ?? "",
                    displayOrder:
                        portDefinition?.displayOrder != null ? String(portDefinition.displayOrder) : "",
                    deprecated: portDefinition?.deprecated ? "true" : "false",
                };
            }
        });
        const nodePositions = ruleBlockTask.data.parameters.ruleBlockModel?.layout?.nodePositions ?? {};
        operatorNodes.forEach((node) => {
            const position = nodePositions[node.nodeId];
            if (position) {
                node.position = {
                    x: position.x,
                    y: position.y,
                };
                node.dimension = {
                    ...node.dimension,
                    width: position.width ?? undefined,
                    height: position.height ?? undefined,
                };
            }
        });
        return operatorNodes;
    };

    const getStickyNotes = (ruleBlockTask: RuleBlockTaskData | undefined): StickyNote[] =>
        ruleBlockUtils.normalizeStickyNotes(ruleBlockTask?.data.parameters.ruleBlockModel?.uiAnnotations?.stickyNotes);

    const partialAutoCompletion = React.useCallback(
        (_inputType: "source" | "target") =>
            async (
                _inputString: string,
                _cursorPosition: number,
            ): Promise<CodeAutocompleteFieldPartialAutoCompleteResult | undefined> => undefined,
        [],
    );

    const tabs = React.useMemo<IRuleSideBarFilterTabConfig[]>(
        () => [
            ruleUtils.sidebarTabs.all as IRuleSideBarFilterTabConfig,
            {
                id: "inputPorts",
                label: t("taskViews.ruleBlock.inputPortsTab"),
                filterAndSort: (operators) => operators.filter((op) => op.pluginType === "InputPortOperator"),
                showOperatorsFromPreConfiguredOperatorTabsForQuery: false,
            },
            ruleUtils.sidebarTabs.transform as IRuleSideBarFilterTabConfig,
        ],
        [t],
    );

    return (
        <RuleEditor<RuleBlockTaskData, IPluginDetails>
            projectId={projectId}
            taskId={ruleBlockTaskId}
            fetchRuleData={fetchRuleBlockTask}
            fetchRuleOperators={fetchTransformRuleOperatorList}
            saveRule={saveRuleBlock}
            convertRuleOperator={ruleUtils.convertRuleOperator}
            convertToRuleOperatorNodes={convertToRuleOperatorNodes}
            partialAutoCompletion={partialAutoCompletion}
            viewActions={viewActions}
            getStickyNotes={getStickyNotes}
            additionalRuleOperators={[inputPortOperator]}
            validateConnection={ruleUtils.validateConnection}
            tabs={tabs}
            showRuleOnly={false}
            instanceId={instanceId}
            saveInitiallyEnabled={false}
        />
    );
};
