import React from "react";
import {
    AlertDialog,
    Button,
    ContextOverlay,
    ContextMenu,
    Icon,
    IconButton,
    MenuItem,
    Spacing,
    StickyNote,
    ToolbarSection
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import i18next from "i18next";
import { requestRuleOperatorPluginsDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { IProjectTask } from "@ducks/shared/typings";
import { requestRelatedItems, requestTaskData } from "@ducks/shared/requests";
import { requestUpdateProjectTask } from "@ducks/workspace/requests";
import { IViewActions } from "../../plugins/PluginRegistry";
import RuleEditor, { RuleEditorExternalApi, RuleOperatorFetchFnType } from "../../shared/RuleEditor/RuleEditor";
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
import RuleBlockEvaluation from "./RuleBlockEvaluation";

export interface RuleBlockEditorProps {
    projectId: string;
    ruleBlockTaskId: string;
    viewActions?: IViewActions;
    instanceId: string;
}

type RuleBlockTaskData = IProjectTask<IRuleBlockTaskParameters>;
type InputPortDialogState = { mode: "create" } | { mode: "edit"; portId: string } | undefined;
type DeleteInputPortDialogState = { portId: string; instanceCount: number } | undefined;
type RuleBlockUsageState = {
    isInUse: boolean;
    refreshFailed: boolean;
    refreshRunning: boolean;
};

/** Editor for reusable rule block tasks. */
export const RuleBlockEditor = ({ projectId, ruleBlockTaskId, viewActions, instanceId }: RuleBlockEditorProps) => {
    const { i18n } = useTranslation();
    const { registerError } = useErrorHandler();
    const [ports, setPorts] = React.useState(ruleBlockUtils.emptyRuleBlockModel().ports);
    const [persistedPorts, setPersistedPorts] = React.useState(ruleBlockUtils.emptyRuleBlockModel().ports);
    const [sidebarReloadToken, setSidebarReloadToken] = React.useState(0);
    const [inputPortDialogState, setInputPortDialogState] = React.useState<InputPortDialogState>(undefined);
    const [deleteInputPortDialogState, setDeleteInputPortDialogState] =
        React.useState<DeleteInputPortDialogState>(undefined);
    const [ruleBlockUsageState, setRuleBlockUsageState] = React.useState<RuleBlockUsageState>({
        isInUse: false,
        refreshFailed: false,
        refreshRunning: false,
    });
    const ruleEditorRef = React.useRef<RuleEditorExternalApi>(null);
    const portsRef = React.useRef(ports);
    const persistedPortsRef = React.useRef(persistedPorts);
    const ruleBlockUsageStateRef = React.useRef(ruleBlockUsageState);
    portsRef.current = ports;
    persistedPortsRef.current = persistedPorts;
    ruleBlockUsageStateRef.current = ruleBlockUsageState;

    const bumpSidebarReloadToken = React.useCallback(() => {
        setSidebarReloadToken((currentToken) => currentToken + 1);
    }, []);

    const applyPorts = React.useCallback((nextPorts: IRuleBlockPort[]) => {
        ruleBlockUtils.assertValidPorts(nextPorts);
        setPorts(ruleBlockUtils.sortRuleBlockPorts(nextPorts));
        bumpSidebarReloadToken();
    }, [bumpSidebarReloadToken]);

    const applyPersistedPorts = React.useCallback((nextPorts: IRuleBlockPort[]) => {
        ruleBlockUtils.assertValidPorts(nextPorts);
        setPersistedPorts(ruleBlockUtils.sortRuleBlockPorts(nextPorts));
    }, []);

    const refreshRuleBlockUsage = React.useCallback(async () => {
        setRuleBlockUsageState((currentState) => ({
            ...currentState,
            refreshFailed: false,
            refreshRunning: true,
        }));
        try {
            const relatedItemsResponse = await requestRelatedItems(projectId, ruleBlockTaskId, "", 1);
            setRuleBlockUsageState({
                isInUse: relatedItemsResponse.data.total > 0,
                refreshFailed: false,
                refreshRunning: false,
            });
            bumpSidebarReloadToken();
        } catch {
            setRuleBlockUsageState({
                isInUse: false,
                refreshFailed: true,
                refreshRunning: false,
            });
            bumpSidebarReloadToken();
        }
    }, [bumpSidebarReloadToken, projectId, ruleBlockTaskId]);

    const fetchRuleBlockTask = React.useCallback(
        async (currentProjectId: string, taskId: string): Promise<RuleBlockTaskData | undefined> => {
            try {
                const taskData = (await requestTaskData<IRuleBlockTaskParameters>(currentProjectId, taskId)).data;
                const loadedPorts = taskData.data.parameters.ruleBlockModel?.ports ?? [];
                applyPersistedPorts(loadedPorts);
                applyPorts(loadedPorts);
                await refreshRuleBlockUsage();
                return taskData;
            } catch (err) {
                registerError(
                    "RuleBlockEditor_fetchRuleBlockTask",
                    i18next.t("taskViews.ruleBlock.errors.fetchTaskData"),
                    err,
                    {
                        errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE,
                    },
                );
            }
        },
        [applyPersistedPorts, applyPorts, refreshRuleBlockUsage, registerError],
    );

    const fetchTransformRuleOperatorList = React.useCallback(async (): Promise<IPluginDetails[] | undefined> => {
        try {
            const response = (await requestRuleOperatorPluginsDetails(true)).data;
            return Object.values(response).filter((plugin) => plugin.pluginType === "TransformOperator");
        } catch (err) {
            registerError(
                "RuleBlockEditor_fetchTransformRuleOperatorDetails",
                i18next.t("taskViews.ruleBlock.errors.fetchTransformOperators"),
                err,
                { errorNotificationInstanceId: RULE_EDITOR_NOTIFICATION_INSTANCE },
            );
        }
    }, [registerError]);

    const inputPortOperator = React.useMemo(() => ruleBlockEditorUtils.createInputPortOperator(), [i18n.language]);
    const additionalRuleOperators = React.useMemo(() => [inputPortOperator], [inputPortOperator]);
    const editedPort =
        inputPortDialogState?.mode === "edit"
            ? ports.find((port) => port.id === inputPortDialogState.portId)
            : undefined;
    const inputPortDialogInitialValue = React.useMemo<InputPortDialogSubmitValue>(() => {
        if (editedPort) {
            return {
                label: editedPort.label,
                description: editedPort.description,
                displayOrder: editedPort.displayOrder,
                deprecated: editedPort.deprecated,
            };
        }
        return ruleBlockUtils.nextInputPortDefaults(ports);
    }, [editedPort, ports]);

    const closeInputPortDialog = React.useCallback(() => {
        setInputPortDialogState(undefined);
    }, []);

    const closeDeleteInputPortDialog = React.useCallback(() => {
        setDeleteInputPortDialogState(undefined);
    }, []);

    const openCreateInputPortDialog = React.useCallback(() => {
        setInputPortDialogState({ mode: "create" });
    }, []);

    const openEditInputPortDialog = React.useCallback((portId: string) => {
        setInputPortDialogState({ mode: "edit", portId });
    }, []);

    const isRuleBlockPortArray = (savedState: unknown): savedState is IRuleBlockPort[] =>
        Array.isArray(savedState) &&
        savedState.every(
            (port) =>
                typeof port === "object" &&
                port !== null &&
                typeof port.id === "string" &&
                typeof port.label === "string" &&
                typeof port.description === "string" &&
                typeof port.displayOrder === "number" &&
                typeof port.deprecated === "boolean",
        );

    const captureExternalSavedState = React.useCallback(() => {
        return ruleBlockUtils.sortRuleBlockPorts(portsRef.current);
    }, []);

    const restoreExternalSavedState = React.useCallback(
        (savedState: unknown) => {
            if (isRuleBlockPortArray(savedState)) {
                applyPersistedPorts(savedState);
                applyPorts(savedState);
            }
        },
        [applyPersistedPorts, applyPorts],
    );

    const getPorts = React.useCallback(() => portsRef.current, []);
    const getPersistedPorts = React.useCallback(() => persistedPortsRef.current, []);
    const isRuleBlockInUse = React.useCallback(
        () => ruleBlockUsageStateRef.current.isInUse,
        [],
    );

    const inputPortInstanceNodeIds = React.useCallback((portId: string): string[] => {
        const ruleEditorApi = ruleEditorRef.current;
        if (!ruleEditorApi) {
            return [];
        }
        return ruleEditorApi
            .ruleOperatorNodes()
            .filter(
                (node) =>
                    node.pluginType === "InputPortOperator" && ruleBlockUtils.requirePortId(node) === portId,
            )
            .map((node) => node.nodeId);
    }, []);

    const syncInputPortNodeMetaData = React.useCallback((updatedPort: IRuleBlockPort) => {
        const ruleEditorApi = ruleEditorRef.current;
        if (!ruleEditorApi) {
            return;
        }
        const affectedNodeIds = ruleEditorApi
            .ruleOperatorNodes()
            .filter(
                (node) =>
                    node.pluginType === "InputPortOperator" && ruleBlockUtils.requirePortId(node) === updatedPort.id,
            )
            .map((node) => node.nodeId);
        if (affectedNodeIds.length === 0) {
            return;
        }
        ruleEditorApi.updateRuleOperatorNodeMetaData(affectedNodeIds, () =>
            ruleBlockEditorUtils.inputPortNodeMetaData(updatedPort),
        );
    }, []);

    const syncChangedInputPortNodeMetaData = React.useCallback(
        (previousPorts: IRuleBlockPort[], nextPorts: IRuleBlockPort[]) => {
            ruleBlockUtils.portsWithChangedDisplayOrder(previousPorts, nextPorts).forEach(syncInputPortNodeMetaData);
        },
        [syncInputPortNodeMetaData],
    );

    const deleteInputPort = React.useCallback(
        (portId: string) => {
            const ruleEditorApi = ruleEditorRef.current;
            const previousPorts = portsRef.current;
            const nextPorts = previousPorts.filter((port) => port.id !== portId);
            const affectedNodeIds = inputPortInstanceNodeIds(portId);
            if (ruleEditorApi) {
                ruleEditorApi.startChangeTransaction();
                ruleEditorApi.executeExternalRuleModelChange({
                    do: () => applyPorts(nextPorts),
                    undo: () => applyPorts(previousPorts),
                });
                if (affectedNodeIds.length > 0) {
                    ruleEditorApi.deleteNodes(affectedNodeIds);
                }
            } else {
                applyPorts(nextPorts);
            }
        },
        [applyPorts, inputPortInstanceNodeIds],
    );

    const requestDeleteInputPort = React.useCallback(
        (portId: string) => {
            const instanceCount = inputPortInstanceNodeIds(portId).length;
            if (instanceCount > 0) {
                setDeleteInputPortDialogState({ portId, instanceCount });
            } else {
                deleteInputPort(portId);
            }
        },
        [deleteInputPort, inputPortInstanceNodeIds],
    );

    const confirmDeleteInputPort = React.useCallback(() => {
        if (!deleteInputPortDialogState) {
            return;
        }
        deleteInputPort(deleteInputPortDialogState.portId);
        closeDeleteInputPortDialog();
    }, [closeDeleteInputPortDialog, deleteInputPort, deleteInputPortDialogState]);

    const normalizeInputPortOrder = React.useCallback(() => {
        const ruleEditorApi = ruleEditorRef.current;
        const previousPorts = portsRef.current;
        const nextPorts = ruleBlockUtils.normalizePortDisplayOrder(previousPorts);
        if (
            previousPorts.length === nextPorts.length &&
            previousPorts.every(
                (port, index) => port.id === nextPorts[index].id && port.displayOrder === nextPorts[index].displayOrder,
            )
        ) {
            return;
        }
        if (ruleEditorApi) {
            ruleEditorApi.startChangeTransaction();
            ruleEditorApi.executeExternalRuleModelChange({
                do: () => applyPorts(nextPorts),
                undo: () => applyPorts(previousPorts),
            });
            syncChangedInputPortNodeMetaData(previousPorts, nextPorts);
        } else {
            applyPorts(nextPorts);
            syncChangedInputPortNodeMetaData(previousPorts, nextPorts);
        }
    }, [applyPorts, syncChangedInputPortNodeMetaData]);

    const submitInputPortDialog = React.useCallback(
        (value: InputPortDialogSubmitValue) => {
            const ruleEditorApi = ruleEditorRef.current;
            const previousPorts = portsRef.current;
            if (inputPortDialogState?.mode === "edit" && editedPort) {
                const updatedPorts = previousPorts.map((port) =>
                        port.id === editedPort.id
                            ? {
                                  ...port,
                                  ...value,
                              }
                            : port,
                    );
                const updatedPort = updatedPorts.find((port) => port.id === editedPort.id);
                if (ruleEditorApi) {
                    ruleEditorApi.startChangeTransaction();
                    ruleEditorApi.executeExternalRuleModelChange({
                        do: () => applyPorts(updatedPorts),
                        undo: () => applyPorts(previousPorts),
                    });
                    if (updatedPort) {
                        syncInputPortNodeMetaData(updatedPort);
                    }
                } else {
                    applyPorts(updatedPorts);
                    if (updatedPort) {
                        syncInputPortNodeMetaData(updatedPort);
                    }
                }
            } else {
                const updatedPorts = [
                    ...previousPorts,
                    {
                        id: ruleBlockUtils.generateInputPortId(),
                        ...value,
                    },
                ];
                if (ruleEditorApi) {
                    ruleEditorApi.startChangeTransaction();
                    ruleEditorApi.executeExternalRuleModelChange({
                        do: () => applyPorts(updatedPorts),
                        undo: () => applyPorts(previousPorts),
                    });
                } else {
                    applyPorts(updatedPorts);
                }
            }
            closeInputPortDialog();
        },
        [applyPorts, closeInputPortDialog, editedPort, inputPortDialogState?.mode, syncInputPortNodeMetaData],
    );

    const ruleEditorTabs = React.useMemo<
        (IRuleSideBarFilterTabConfig | ReturnType<typeof ruleBlockEditorUtils.createInputPortsTab>)[]
    >(
        () => [
            {
                ...(ruleUtils.sidebarTabs.all as IRuleSideBarFilterTabConfig),
                // The generic InputPortOperator is needed by the editor model, but the rule block UI should show
                // logical input-port entries from local state instead of this internal operator definition.
                filterAndSort: (operators) => operators.filter((operator) => operator.pluginType !== "InputPortOperator"),
                showOperatorsFromPreConfiguredOperatorTabsAlways: true,
            },
            ruleBlockEditorUtils.createInputPortsTab(
                getPorts,
                getPersistedPorts,
                isRuleBlockInUse,
                openCreateInputPortDialog,
                openEditInputPortDialog,
                requestDeleteInputPort,
            ),
            ruleUtils.sidebarTabs.transform as IRuleSideBarFilterTabConfig,
        ],
        [
            getPersistedPorts,
            getPorts,
            i18n.language,
            isRuleBlockInUse,
            openCreateInputPortDialog,
            openEditInputPortDialog,
            requestDeleteInputPort,
        ],
    );

    const deletePort = deleteInputPortDialogState
        ? ports.find((port) => port.id === deleteInputPortDialogState.portId)
        : undefined;
    const canNormalizePortOrder = !ruleBlockUtils.isNormalizedPortDisplayOrder(ports);
    const ruleBlockIsInUse = ruleBlockUsageState.isInUse;

    const saveRuleBlock = React.useCallback(
        async (
            ruleOperatorNodes: IRuleOperatorNode[],
            stickyNotes: StickyNote[],
            originalTask: RuleBlockTaskData,
        ): Promise<RuleSaveResult> => {
            try {
                const currentModel = originalTask.data.parameters.ruleBlockModel ?? ruleBlockUtils.emptyRuleBlockModel();
                const [operatorNodeMap, rootNodes] = ruleUtils.convertToRuleOperatorNodeMap(ruleOperatorNodes, true);
                const inputPortNodes = ruleOperatorNodes.filter((node) => node.pluginType === "InputPortOperator");
                const nextPortDefinitions = ruleBlockUtils.sortRuleBlockPorts(portsRef.current);
                const missingPortIdErrors = ruleBlockUtils.validateMissingPortIds(
                    inputPortNodes,
                    () => i18next.t("taskViews.ruleBlock.errors.missingPortId"),
                );
                if (missingPortIdErrors.length) {
                    return new RuleValidationError(
                        i18next.t("taskViews.ruleBlock.errors.invalidPorts"),
                        missingPortIdErrors,
                    );
                }
                const relatedItemsResponse = await requestRelatedItems(projectId, ruleBlockTaskId, "", 1);
                setRuleBlockUsageState({
                    isInUse: relatedItemsResponse.data.total > 0,
                    refreshFailed: false,
                    refreshRunning: false,
                });
                bumpSidebarReloadToken();
                if (relatedItemsResponse.data.total > 0) {
                    const compatibilityValidation = ruleBlockUtils.validateUsedPortCompatibility(
                        currentModel.ports,
                        nextPortDefinitions,
                        inputPortNodes,
                        (portLabel) => i18next.t("taskViews.ruleBlock.errors.usedPortRemoved", { portLabel }),
                        (portLabel) => i18next.t("taskViews.ruleBlock.errors.usedPortReordered", { portLabel }),
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
                    (displayOrder) => i18next.t("taskViews.ruleBlock.errors.duplicateDisplayOrder", { displayOrder }),
                );
                if (duplicateDisplayOrderErrors.length) {
                    return new RuleValidationError(
                        i18next.t("taskViews.ruleBlock.errors.invalidPorts"),
                        duplicateDisplayOrderErrors,
                    );
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
                applyPersistedPorts(nextPortDefinitions);
                return {
                    success: true,
                };
            } catch (err) {
                if ((err as RuleValidationError).isRuleValidationError) {
                    return err;
                }
                if (err?.isHttpError && err.httpStatus === 400 && Array.isArray((err as FetchError).body?.issues)) {
                    return new RuleValidationError(
                        i18next.t("taskViews.ruleBlock.errors.save"),
                        (err as FetchError).body.issues.map((issue) => ({
                            nodeId: issue.id,
                            message: issue.message,
                        })),
                    );
                }
                if (err?.isHttpError) {
                    return {
                        success: false,
                        errorMessage: `${i18next.t("taskViews.ruleBlock.errors.save")}${err.message ? ": " + err.message : ""}`,
                    };
                }
                return {
                    success: false,
                    errorMessage: `${i18next.t("taskViews.ruleBlock.errors.save")}${err.message ? ": " + err.message : ""}`,
                };
            }
        },
        [applyPersistedPorts, bumpSidebarReloadToken, projectId, ruleBlockTaskId],
    );

    const partialAutoCompletion = React.useCallback(
        (_inputType: "source" | "target") =>
            async (
                _inputString: string,
                _cursorPosition: number,
            ): Promise<CodeAutocompleteFieldPartialAutoCompleteResult | undefined> => undefined,
        [],
    );

    const convertToRuleOperatorNodes = React.useCallback(
        (ruleBlockTask: RuleBlockTaskData, ruleOperator: RuleOperatorFetchFnType) =>
            ruleBlockEditorUtils.convertRuleBlockTaskToRuleOperatorNodes(ruleBlockTask, ruleOperator),
        [],
    );

    const additionalToolBarComponents = React.useCallback(
        () => (
            <ToolbarSection>
                <RuleBlockUsageStatusControl
                    usageState={ruleBlockUsageState}
                    onRefresh={refreshRuleBlockUsage}
                />
                <ContextMenu
                    togglerElement={
                        <IconButton
                            name="data-targetschema"
                            text={i18next.t("taskViews.ruleBlock.portMenu")}
                            tooltipAsTitle
                        />
                    }
                >
                    <MenuItem
                        onClick={normalizeInputPortOrder}
                        disabled={!canNormalizePortOrder}
                        text={i18next.t("taskViews.ruleBlock.normalizePortOrder")}
                        htmlTitle={i18next.t("taskViews.ruleBlock.normalizePortOrderTooltip")}
                    />
                </ContextMenu>
                <Spacing vertical={true} hasDivider={true} />
            </ToolbarSection>
        ),
        [canNormalizePortOrder, normalizeInputPortOrder, refreshRuleBlockUsage, ruleBlockUsageState],
    );

    const extraRuleNodeMenuItems = React.useCallback(
        (node: IRuleOperatorNode, closeMenu: () => void): React.JSX.Element[] | undefined => {
            if (node.pluginType !== "InputPortOperator") {
                return undefined;
            }
            const portId = ruleBlockUtils.requirePortId(node);
            return [
                <MenuItem
                    key={`edit-input-port-${node.nodeId}`}
                    icon="item-edit"
                    text={i18next.t("taskViews.ruleBlock.editInputPort", "Edit input port")}
                    onClick={() => {
                        closeMenu();
                        openEditInputPortDialog(portId);
                    }}
                />,
            ];
        },
        [openEditInputPortDialog],
    );

    const externalSidebarContextValue = React.useMemo(() => ({ reloadToken: sidebarReloadToken }), [sidebarReloadToken]);

    return (
        <ExternalSidebarContext.Provider value={externalSidebarContextValue}>
            <>
                <RuleBlockEvaluation
                    projectId={projectId}
                    ruleBlockTaskId={ruleBlockTaskId}
                    numberOfEntitiesToShow={5}
                    getPorts={getPorts}
                >
                    <RuleEditor<RuleBlockTaskData, IPluginDetails>
                        ref={ruleEditorRef}
                        projectId={projectId}
                        taskId={ruleBlockTaskId}
                        fetchRuleData={fetchRuleBlockTask}
                        fetchRuleOperators={fetchTransformRuleOperatorList}
                        saveRule={saveRuleBlock}
                        convertRuleOperator={ruleUtils.convertRuleOperator}
                        convertToRuleOperatorNodes={convertToRuleOperatorNodes}
                        partialAutoCompletion={partialAutoCompletion}
                        viewActions={viewActions}
                        getStickyNotes={ruleBlockEditorUtils.getStickyNotes}
                        // Register the internal operator definition so existing canvas nodes and drag/drop-created nodes
                        // can be materialized. User-facing sidebar entries come from the pre-configured input-port tab.
                        additionalRuleOperators={additionalRuleOperators}
                        additionalToolBarComponents={additionalToolBarComponents}
                        extraRuleNodeMenuItems={extraRuleNodeMenuItems}
                        validateConnection={ruleUtils.validateConnection}
                        tabs={ruleEditorTabs}
                        captureExternalSavedState={captureExternalSavedState}
                        restoreExternalSavedState={restoreExternalSavedState}
                        showRuleOnly={false}
                        instanceId={instanceId}
                        saveInitiallyEnabled={false}
                    />
                </RuleBlockEvaluation>
                <InputPortDialog
                    isOpen={!!inputPortDialogState}
                    mode={inputPortDialogState?.mode ?? "create"}
                    initialPort={inputPortDialogInitialValue}
                    existingPorts={ports}
                    persistedPorts={persistedPorts}
                    isRuleBlockInUse={ruleBlockIsInUse}
                    editedPortId={editedPort?.id}
                    onClose={closeInputPortDialog}
                    onSubmit={submitInputPortDialog}
                />
                {
                    deleteInputPortDialogState ?
                        <ConfirmDeleteInputPortDialog
                            instanceCount={deleteInputPortDialogState.instanceCount}
                            portLabel={deletePort?.label}
                            onConfirm={confirmDeleteInputPort}
                            onClose={closeDeleteInputPortDialog}
                        /> :
                        null
                }
            </>
        </ExternalSidebarContext.Provider>
    );
};

interface ConfirmDeleteInputPortDialogProps {
    instanceCount: number;
    portLabel?: string;
    onConfirm: () => void;
    onClose: () => void;
}

interface RuleBlockUsageStatusControlProps {
    usageState: RuleBlockUsageState;
    onRefresh: () => void;
}

const RuleBlockUsageStatusControl = ({ usageState, onRefresh }: RuleBlockUsageStatusControlProps) => {
    const [t] = useTranslation();
    const [showOverlay, setShowOverlay] = React.useState(false);
    const isVisible = usageState.isInUse || usageState.refreshFailed;

    const content = usageState.refreshFailed
        ? t("taskViews.ruleBlock.usageRefreshError")
        : t("taskViews.ruleBlock.usageInUse");
    const tooltip = `${content} ${t("taskViews.ruleBlock.usageRefreshHint")}`;

    React.useEffect(() => {
        if (!isVisible) {
            setShowOverlay(false);
            return;
        }
        setShowOverlay(true);
        const timeoutId = window.setTimeout(() => setShowOverlay(false), 5000);
        return () => window.clearTimeout(timeoutId);
    }, [isVisible, tooltip]);

    if (!isVisible) {
        return null;
    }

    return (
        <>
            <ContextOverlay
                isOpen={showOverlay}
                onClose={() => setShowOverlay(false)}
                content={<div style={{ maxWidth: "26rem", padding: "0.5rem" }}>{tooltip}</div>}
            >
                <Button
                    onClick={onRefresh}
                    loading={usageState.refreshRunning}
                    disabled={usageState.refreshRunning}
                    tooltip={tooltip}
                    tooltipProps={{ hoverCloseDelay: 0 }}
                    aria-label={t("taskViews.ruleBlock.refreshUsage")}
                >
                    <Icon name={"state-warning"} />
                </Button>
            </ContextOverlay>
            <Spacing vertical={true} size="small" />
        </>
    );
};

const ConfirmDeleteInputPortDialog = ({
    instanceCount,
    portLabel,
    onConfirm,
    onClose,
}: ConfirmDeleteInputPortDialogProps) => {
    const [t] = useTranslation();

    return (
        <AlertDialog
            danger
            isOpen={true}
            title={t("taskViews.ruleBlock.deleteInputPort.title", "Delete input port")}
            onClose={onClose}
            actions={[
                <Button key="delete-input-port" disruptive onClick={onConfirm}>
                    {t("common.action.delete", "Delete")}
                </Button>,
                <Button key="cancel-delete-input-port" onClick={onClose}>
                    {t("common.action.cancel", "Cancel")}
                </Button>,
            ]}
        >
            {t("taskViews.ruleBlock.deleteInputPort.confirmMessage", {
                defaultValue:
                    "Delete input port '{{portLabel}}'? This will also remove {{count}} instances from the rule tree.",
                portLabel: portLabel ?? t("taskViews.ruleBlock.inputPortOperator", "Input port"),
                count: instanceCount,
            })}
        </AlertDialog>
    );
};
