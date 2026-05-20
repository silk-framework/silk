import React from "react";
import { StickyNote } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { requestRuleOperatorPluginsDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { IProjectTask } from "@ducks/shared/typings";
import { requestRelatedItems, requestTaskData } from "@ducks/shared/requests";
import { requestUpdateProjectTask } from "@ducks/workspace/requests";
import { IViewActions } from "../../plugins/PluginRegistry";
import RuleEditor from "../../shared/RuleEditor/RuleEditor";
import {
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
import { IRuleBlockModel, IRuleBlockPort, IRuleBlockTaskParameters } from "./ruleBlock.types";
import ruleBlockUtils from "./ruleBlock.utils";
import ruleBlockEditorUtils from "./RuleBlockEditor.utils";
import { ExternalSidebarContext } from "../../shared/RuleEditor/contexts/ExternalSidebarContext";
import InputPortDialog, { InputPortDialogSubmitValue } from "./InputPortDialog";

export interface RuleBlockEditorProps {
    projectId: string;
    ruleBlockTaskId: string;
    viewActions?: IViewActions;
    instanceId: string;
}

type RuleBlockTaskData = IProjectTask<IRuleBlockTaskParameters>;
type InputPortDialogState = { mode: "create" } | { mode: "edit"; portId: string } | undefined;

/** Editor for reusable rule block tasks. */
export const RuleBlockEditor = ({ projectId, ruleBlockTaskId, viewActions, instanceId }: RuleBlockEditorProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [ports, setPorts] = React.useState(ruleBlockUtils.emptyRuleBlockModel().ports);
    const [sidebarReloadTokensByTabId, setSidebarReloadTokensByTabId] = React.useState<Record<string, number>>({});
    const [inputPortDialogState, setInputPortDialogState] = React.useState<InputPortDialogState>(undefined);

    const bumpSidebarReloadToken = React.useCallback((tabId: string) => {
        setSidebarReloadTokensByTabId((currentTokens) => ({
            ...currentTokens,
            [tabId]: (currentTokens[tabId] ?? 0) + 1,
        }));
    }, []);

    const fetchRuleBlockTask = async (
        currentProjectId: string,
        taskId: string,
    ): Promise<RuleBlockTaskData | undefined> => {
        try {
            const taskData = (await requestTaskData<IRuleBlockTaskParameters>(currentProjectId, taskId)).data;
            setPorts(ruleBlockUtils.sortRuleBlockPorts(taskData.data.parameters.ruleBlockModel?.ports ?? []));
            bumpSidebarReloadToken("inputPorts");
            return taskData;
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

    const inputPortOperator = React.useMemo(() => ruleBlockEditorUtils.createInputPortOperator(), [t]);
    const editedPort =
        inputPortDialogState?.mode === "edit"
            ? ports.find((port) => port.id === inputPortDialogState.portId)
            : undefined;
    const inputPortDialogInitialValue = React.useMemo<InputPortDialogSubmitValue>(() => {
        if (editedPort) {
            return {
                label: editedPort.label,
                description: editedPort.description,
                exampleValues: editedPort.exampleValues,
                displayOrder: editedPort.displayOrder,
                deprecated: editedPort.deprecated,
            };
        }
        return ruleBlockUtils.nextInputPortDefaults(ports);
    }, [editedPort, ports]);

    const closeInputPortDialog = React.useCallback(() => {
        setInputPortDialogState(undefined);
    }, []);

    const openCreateInputPortDialog = React.useCallback(() => {
        setInputPortDialogState({ mode: "create" });
    }, []);

    const openEditInputPortDialog = React.useCallback((portId: string) => {
        setInputPortDialogState({ mode: "edit", portId });
    }, []);

    const updatePorts = React.useCallback((nextPorts: IRuleBlockPort[]) => {
        setPorts(ruleBlockUtils.sortRuleBlockPorts(nextPorts));
        bumpSidebarReloadToken("inputPorts");
    }, [bumpSidebarReloadToken]);

    const submitInputPortDialog = React.useCallback(
        (value: InputPortDialogSubmitValue) => {
            if (inputPortDialogState?.mode === "edit" && editedPort) {
                updatePorts(
                    ports.map((port) =>
                        port.id === editedPort.id
                            ? {
                                  ...port,
                                  ...value,
                              }
                            : port,
                    ),
                );
            } else {
                updatePorts([
                    ...ports,
                    {
                        id: ruleBlockUtils.generateInputPortId(),
                        ...value,
                    },
                ]);
            }
            closeInputPortDialog();
        },
        [closeInputPortDialog, editedPort, inputPortDialogState?.mode, ports, updatePorts],
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
            const nextPortDefinitions = ruleBlockUtils.sortRuleBlockPorts(ports);
            const relatedItemsResponse = await requestRelatedItems(projectId, ruleBlockTaskId, "", 1);
            if (relatedItemsResponse.data.total > 0) {
                const compatibilityValidation = ruleBlockUtils.validateUsedPortCompatibility(
                    currentModel.ports,
                    nextPortDefinitions,
                    inputPortNodes,
                    (portLabel) => t("taskViews.ruleBlock.errors.usedPortRemoved", { portLabel }),
                    (portLabel) => t("taskViews.ruleBlock.errors.usedPortReordered", { portLabel }),
                );
                if (compatibilityValidation.errorMessage) {
                    return new RuleValidationError(
                        compatibilityValidation.errorMessage,
                        compatibilityValidation.nodeErrors,
                    );
                }
            }

            const duplicateDisplayOrderErrors = ruleBlockUtils.validateDuplicateDisplayOrders(
                nextPortDefinitions,
                inputPortNodes,
                (displayOrder) => t("taskViews.ruleBlock.errors.duplicateDisplayOrder", { displayOrder }),
            );
            if (duplicateDisplayOrderErrors.length) {
                return new RuleValidationError(t("taskViews.ruleBlock.errors.invalidPorts"), duplicateDisplayOrderErrors);
            }

            const updatedModel: IRuleBlockModel = {
                ...currentModel,
                ports: nextPortDefinitions,
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
            if (err?.isHttpError) {
                return {
                    success: false,
                    errorMessage: `${t("taskViews.ruleBlock.errors.save")}${err.message ? ": " + err.message : ""}`,
                };
            }
            return {
                success: false,
                errorMessage: `${t("taskViews.ruleBlock.errors.save")}${err.message ? ": " + err.message : ""}`,
            };
        }
    };

    const partialAutoCompletion = React.useCallback(
        (_inputType: "source" | "target") =>
            async (
                _inputString: string,
                _cursorPosition: number,
            ): Promise<CodeAutocompleteFieldPartialAutoCompleteResult | undefined> => undefined,
        [],
    );

    const tabs = React.useMemo<
        (IRuleSideBarFilterTabConfig | ReturnType<typeof ruleBlockEditorUtils.createInputPortsTab>)[]
    >(
        () => [
            ruleUtils.sidebarTabs.all as IRuleSideBarFilterTabConfig,
            ruleBlockEditorUtils.createInputPortsTab(ports, openCreateInputPortDialog, openEditInputPortDialog),
            ruleUtils.sidebarTabs.transform as IRuleSideBarFilterTabConfig,
        ],
        [openCreateInputPortDialog, openEditInputPortDialog, ports, t],
    );

    return (
        <ExternalSidebarContext.Provider value={{ reloadTokensByTabId: sidebarReloadTokensByTabId }}>
            <>
                <RuleEditor<RuleBlockTaskData, IPluginDetails>
                    projectId={projectId}
                    taskId={ruleBlockTaskId}
                    fetchRuleData={fetchRuleBlockTask}
                    fetchRuleOperators={fetchTransformRuleOperatorList}
                    saveRule={saveRuleBlock}
                    convertRuleOperator={ruleUtils.convertRuleOperator}
                    convertToRuleOperatorNodes={(ruleBlockTask, ruleOperator) =>
                        ruleBlockEditorUtils.convertRuleBlockTaskToRuleOperatorNodes(ruleBlockTask, ruleOperator)
                    }
                    partialAutoCompletion={partialAutoCompletion}
                    viewActions={viewActions}
                    getStickyNotes={ruleBlockEditorUtils.getStickyNotes}
                    additionalRuleOperators={[inputPortOperator]}
                    validateConnection={ruleUtils.validateConnection}
                    tabs={tabs}
                    showRuleOnly={false}
                    instanceId={instanceId}
                    saveInitiallyEnabled={false}
                />
                <InputPortDialog
                    isOpen={!!inputPortDialogState}
                    mode={inputPortDialogState?.mode ?? "create"}
                    initialPort={inputPortDialogInitialValue}
                    existingPorts={ports}
                    editedPortId={editedPort?.id}
                    onClose={closeInputPortDialog}
                    onSubmit={submitInputPortDialog}
                />
            </>
        </ExternalSidebarContext.Provider>
    );
};
