import React from "react";
import {
    AlertDialog,
    Button,
    ContextOverlay,
    ContextMenu,
    IconButton,
    MenuItem,
    Notification,
    Spacing,
    StickyNote,
    ToolbarSection,
    Card,
    CardContent,
    Divider,
    CardActions,
    CardActionsAux,
    Checkbox,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import i18next from "i18next";
import { requestRuleOperatorPluginsDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { IProjectTask } from "@ducks/shared/typings";
import { requestRelatedItems, requestTaskData } from "@ducks/shared/requests";
import { requestUpdateProjectTask } from "@ducks/workspace/requests";
import { IViewActions } from "../../plugins/PluginRegistry";
import RuleEditor, {
    RuleEditorExternalApi,
    RuleOperatorFetchFnType,
    additionalToolbarComponentsPlace,
} from "../../shared/RuleEditor/RuleEditor";
import {
    HandleRuleEditorSidebarDropRequest,
    IRuleOperatorNode,
    IRuleSideBarFilterTabConfig,
    RULE_EDITOR_NOTIFICATION_INSTANCE,
    RuleSaveResult,
    RuleValidationError,
} from "../../shared/RuleEditor/RuleEditor.typings";
import type { PreparedClipboardPaste, RuleClipboardTask } from "../../shared/RuleEditor/model/RuleEditorModel.typings";
import { CodeAutocompleteFieldPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import { XYPosition } from "react-flow-renderer/dist/types";
import ruleUtils from "../shared/rules/rule.utils";
import { FetchError } from "../../../services/fetch/responseInterceptor";
import useErrorHandler from "../../../hooks/useErrorHandler";
import {
    IRuleBlockInputExample,
    RuleBlockSnapshot,
    IRuleBlockModel,
    RuleBlockPort,
    IRuleBlockTaskParameters,
} from "./ruleBlock.types";
import ruleBlockUtils from "./ruleBlock.utils";
import ruleBlockEditorUtils from "./RuleBlockEditor.utils";
import ruleBlockPasteUtils from "./ruleBlockPaste.utils";
import { ExternalSidebarContext } from "../../shared/RuleEditor/contexts/ExternalSidebarContext";
import ExampleValuesDialog from "./ExampleValuesDialog";
import InputPortDialog, { InputPortDialogSubmitValue } from "./InputPortDialog";
import RuleBlockEvaluation from "./RuleBlockEvaluation";
import { RuleBlockEvaluationOptionalContext } from "./RuleBlockEvaluationOptionalContext";

export interface RuleBlockEditorProps {
    projectId: string;
    ruleBlockTaskId: string;
    viewActions?: IViewActions;
    instanceId: string;
}

interface RuleBlockEditorOptionalContextProps {
    /** When enabled only the rule canvas is shown without side- and toolbar or other editing controls. */
    showRuleOnly?: boolean;
    /** If set the embedded editor is permanently read-only. */
    readOnly?: boolean;
    /** When this is defined it will show this rule block snapshot instead of loading the task from the backend. */
    ruleBlockSnapshot?: RuleBlockSnapshot;
    /** Optional example values injected for read-only internal evaluation of a concrete rule block usage. */
    inputExamples?: IRuleBlockInputExample[];
    /** Optional label used for the synthetic task backing an externally supplied snapshot. */
    ruleBlockLabel?: string;
}

export const RuleBlockEditorOptionalContext = React.createContext<RuleBlockEditorOptionalContextProps>({});

type RuleBlockTaskData = IProjectTask<IRuleBlockTaskParameters>;
type RuleBlockExternalSavedState = {
    ports: RuleBlockPort[];
    inputExamples: IRuleBlockInputExample[];
};
type ExampleValuesDialogState =
    | {
          highlightedPortId?: string;
      }
    | undefined;
type InputPortDialogState =
    | { mode: "create"; dropPosition?: XYPosition }
    | { mode: "edit"; portId: string }
    | undefined;
type DeleteInputPortDialogState = { portId: string; instanceCount: number } | undefined;
type RuleBlockUsageState = {
    isInUse: boolean;
    refreshFailed: boolean;
    refreshRunning: boolean;
};

const selectedEvaluationExampleIdsForAvailableExamples = (
    selectedExampleIdsForEvaluation: string[],
    availableExamples: IRuleBlockInputExample[],
): string[] =>
    selectedExampleIdsForEvaluation.filter((selectedExampleId) =>
        availableExamples.some((example) => example.id === selectedExampleId),
    );

const createRuleBlockTaskFromSnapshot = (
    projectId: string,
    ruleBlockTaskId: string,
    snapshot: RuleBlockSnapshot,
    inputExamples?: IRuleBlockInputExample[],
    ruleBlockLabel?: string,
): RuleBlockTaskData => ({
    metadata: {
        label: ruleBlockLabel ?? ruleBlockTaskId,
    },
    taskType: "RuleBlock",
    id: ruleBlockTaskId,
    project: projectId,
    data: {
        type: "RuleBlock",
        parameters: {
            ruleBlockModel: {
                ...snapshot,
                inputExamples: ruleBlockUtils.cloneInputExamples(inputExamples),
            },
        },
    },
});

/** Editor for reusable rule block tasks. */
export const RuleBlockEditor = ({ projectId, ruleBlockTaskId, viewActions, instanceId }: RuleBlockEditorProps) => {
    const { i18n } = useTranslation();
    const { registerError } = useErrorHandler();
    const optionalContext = React.useContext(RuleBlockEditorOptionalContext);
    const evaluationOptionalContext = React.useContext(RuleBlockEvaluationOptionalContext);
    const externalRuleBlockTask = React.useMemo(
        () =>
            optionalContext.ruleBlockSnapshot
                ? createRuleBlockTaskFromSnapshot(
                      projectId,
                      ruleBlockTaskId,
                      optionalContext.ruleBlockSnapshot,
                      optionalContext.inputExamples,
                      optionalContext.ruleBlockLabel,
                  )
                : undefined,
        [
            optionalContext.inputExamples,
            optionalContext.ruleBlockLabel,
            optionalContext.ruleBlockSnapshot,
            projectId,
            ruleBlockTaskId,
        ],
    );
    const showRuleOnly = !!optionalContext.showRuleOnly;
    const readOnly = !!optionalContext.readOnly;
    const isExternalSnapshotMode = !!externalRuleBlockTask;
    const hasExternalEvaluationResults = evaluationOptionalContext.externalEvaluationResults !== undefined;
    const [ports, setPorts] = React.useState(ruleBlockUtils.emptyRuleBlockModel().ports);
    const [persistedPorts, setPersistedPorts] = React.useState(ruleBlockUtils.emptyRuleBlockModel().ports);
    const [inputExamples, setInputExamples] = React.useState(ruleBlockUtils.emptyRuleBlockModel().inputExamples);
    const [selectedExampleIdsForEvaluation, setSelectedExampleIdsForEvaluation] = React.useState<string[]>([]);
    const [sidebarReloadToken, setSidebarReloadToken] = React.useState(0);
    const [inputPortDialogState, setInputPortDialogState] = React.useState<InputPortDialogState>(undefined);
    const [deleteInputPortDialogState, setDeleteInputPortDialogState] =
        React.useState<DeleteInputPortDialogState>(undefined);
    const [exampleValuesDialogState, setExampleValuesDialogState] = React.useState<ExampleValuesDialogState>(undefined);
    const [ruleBlockUsageState, setRuleBlockUsageState] = React.useState<RuleBlockUsageState>({
        isInUse: false,
        refreshFailed: false,
        refreshRunning: false,
    });
    const ruleEditorRef = React.useRef<RuleEditorExternalApi>(null);
    const portsRef = React.useRef(ports);
    const persistedPortsRef = React.useRef(persistedPorts);
    const inputExamplesRef = React.useRef(inputExamples);
    const selectedExampleIdsForEvaluationRef = React.useRef(selectedExampleIdsForEvaluation);
    const ruleBlockUsageStateRef = React.useRef(ruleBlockUsageState);
    portsRef.current = ports;
    persistedPortsRef.current = persistedPorts;
    inputExamplesRef.current = inputExamples;
    selectedExampleIdsForEvaluationRef.current = selectedExampleIdsForEvaluation;
    ruleBlockUsageStateRef.current = ruleBlockUsageState;

    const bumpSidebarReloadToken = React.useCallback(() => {
        setSidebarReloadToken((currentToken) => currentToken + 1);
    }, []);

    const applyPorts = React.useCallback(
        (nextPorts: RuleBlockPort[]) => {
            ruleBlockUtils.assertValidPorts(nextPorts);
            const normalizedPorts = ruleBlockUtils.sortRuleBlockPorts(nextPorts);
            setPorts(normalizedPorts);
            setInputExamples((currentInputExamples) =>
                ruleBlockUtils.pruneInputExamplesToPorts(currentInputExamples, normalizedPorts),
            );
            bumpSidebarReloadToken();
        },
        [bumpSidebarReloadToken],
    );

    const applyPersistedPorts = React.useCallback((nextPorts: RuleBlockPort[]) => {
        ruleBlockUtils.assertValidPorts(nextPorts);
        setPersistedPorts(ruleBlockUtils.sortRuleBlockPorts(nextPorts));
    }, []);

    const applyInputExamples = React.useCallback(
        (nextInputExamples: IRuleBlockInputExample[], nextPorts: RuleBlockPort[] = portsRef.current) => {
            setInputExamples(ruleBlockUtils.pruneInputExamplesToPorts(nextInputExamples, nextPorts));
        },
        [],
    );

    const refreshRuleBlockUsage = React.useCallback(async () => {
        if (isExternalSnapshotMode) {
            return;
        }
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
    }, [bumpSidebarReloadToken, isExternalSnapshotMode, projectId, ruleBlockTaskId]);

    const applyLoadedRuleBlockTask = React.useCallback(
        (taskData: RuleBlockTaskData) => {
            const loadedPorts = taskData.data.parameters.ruleBlockModel?.ports ?? [];
            const loadedInputExamples = taskData.data.parameters.ruleBlockModel?.inputExamples ?? [];
            applyPersistedPorts(loadedPorts);
            applyPorts(loadedPorts);
            applyInputExamples(loadedInputExamples, loadedPorts);
            setSelectedExampleIdsForEvaluation([]);
        },
        [applyInputExamples, applyPersistedPorts, applyPorts],
    );

    const fetchRuleBlockTask = React.useCallback(
        async (currentProjectId: string, taskId: string): Promise<RuleBlockTaskData | undefined> => {
            if (externalRuleBlockTask) {
                applyLoadedRuleBlockTask(externalRuleBlockTask);
                setRuleBlockUsageState({
                    isInUse: false,
                    refreshFailed: false,
                    refreshRunning: false,
                });
                return externalRuleBlockTask;
            }
            try {
                const taskData = (await requestTaskData<IRuleBlockTaskParameters>(currentProjectId, taskId)).data;
                applyLoadedRuleBlockTask(taskData);
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
        [applyLoadedRuleBlockTask, applyPersistedPorts, externalRuleBlockTask, refreshRuleBlockUsage, registerError],
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

    const openCreateInputPortDialog = React.useCallback((dropPosition?: XYPosition) => {
        setInputPortDialogState({ mode: "create", dropPosition });
    }, []);

    const openEditInputPortDialog = React.useCallback((portId: string) => {
        setInputPortDialogState({ mode: "edit", portId });
    }, []);

    const handleSidebarDropRequest = React.useCallback<HandleRuleEditorSidebarDropRequest>(
        (request, position) => {
            if (request.type !== "createInputPort") {
                return false;
            }
            openCreateInputPortDialog(position);
            return true;
        },
        [openCreateInputPortDialog],
    );

    const openExampleValuesDialog = React.useCallback((highlightedPortId?: string) => {
        setExampleValuesDialogState({ highlightedPortId });
    }, []);

    const closeExampleValuesDialog = React.useCallback(() => {
        setExampleValuesDialogState(undefined);
    }, []);

    const isRuleBlockPortArray = (savedState: unknown): savedState is RuleBlockPort[] =>
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

    const isRuleBlockInputExampleArray = (savedState: unknown): savedState is IRuleBlockInputExample[] =>
        Array.isArray(savedState) &&
        savedState.every(
            (example) =>
                typeof example === "object" &&
                example !== null &&
                typeof example.id === "string" &&
                (typeof example.label === "undefined" || typeof example.label === "string") &&
                typeof example.inputs === "object" &&
                example.inputs !== null &&
                Object.values(example.inputs).every(
                    (values) => Array.isArray(values) && values.every((value) => typeof value === "string"),
                ),
        );

    const isRuleBlockExternalSavedState = (savedState: unknown): savedState is RuleBlockExternalSavedState =>
        typeof savedState === "object" &&
        savedState !== null &&
        isRuleBlockPortArray((savedState as RuleBlockExternalSavedState).ports) &&
        isRuleBlockInputExampleArray((savedState as RuleBlockExternalSavedState).inputExamples);

    const captureExternalSavedState = React.useCallback(() => {
        return {
            ports: ruleBlockUtils.sortRuleBlockPorts(portsRef.current),
            inputExamples: ruleBlockUtils.cloneInputExamples(inputExamplesRef.current),
        };
    }, []);

    const restoreExternalSavedState = React.useCallback(
        (savedState: unknown) => {
            if (isRuleBlockExternalSavedState(savedState)) {
                applyPersistedPorts(savedState.ports);
                applyPorts(savedState.ports);
                applyInputExamples(savedState.inputExamples, savedState.ports);
            } else if (isRuleBlockPortArray(savedState)) {
                applyPersistedPorts(savedState);
                applyPorts(savedState);
            }
        },
        [applyInputExamples, applyPersistedPorts, applyPorts],
    );

    const getPorts = React.useCallback(() => portsRef.current, []);
    const getInputExamples = React.useCallback(() => inputExamplesRef.current, []);
    const getEvaluationInputExamples = React.useCallback(
        () =>
            selectedExampleIdsForEvaluationRef.current.length > 0
                ? inputExamplesRef.current.filter((example) =>
                      selectedExampleIdsForEvaluationRef.current.includes(example.id),
                  )
                : inputExamplesRef.current,
        [],
    );
    const getSelectedEvaluationExampleIds = React.useCallback(() => selectedExampleIdsForEvaluationRef.current, []);
    const getPersistedPorts = React.useCallback(() => persistedPortsRef.current, []);
    const isRuleBlockInUse = React.useCallback(() => ruleBlockUsageStateRef.current.isInUse, []);

    const inputPortInstanceNodeIds = React.useCallback((portId: string): string[] => {
        const ruleEditorApi = ruleEditorRef.current;
        if (!ruleEditorApi) {
            return [];
        }
        return ruleEditorApi
            .ruleOperatorNodes()
            .filter((node) => node.pluginType === "InputPortOperator" && ruleBlockUtils.requirePortId(node) === portId)
            .map((node) => node.nodeId);
    }, []);

    const syncInputPortNodeMetaData = React.useCallback((updatedPort: RuleBlockPort) => {
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
        (previousPorts: RuleBlockPort[], nextPorts: RuleBlockPort[]) => {
            ruleBlockUtils.portsWithChangedDisplayOrder(previousPorts, nextPorts).forEach(syncInputPortNodeMetaData);
        },
        [syncInputPortNodeMetaData],
    );

    const deleteInputPort = React.useCallback(
        (portId: string) => {
            const ruleEditorApi = ruleEditorRef.current;
            const previousPorts = portsRef.current;
            const previousInputExamples = ruleBlockUtils.cloneInputExamples(inputExamplesRef.current);
            const nextPorts = previousPorts.filter((port) => port.id !== portId);
            const nextInputExamples = ruleBlockUtils.pruneInputExamplesToPorts(previousInputExamples, nextPorts);
            const affectedNodeIds = inputPortInstanceNodeIds(portId);
            if (ruleEditorApi) {
                ruleEditorApi.startChangeTransaction();
                ruleEditorApi.executeExternalRuleModelChange({
                    do: () => {
                        applyPorts(nextPorts);
                        applyInputExamples(nextInputExamples, nextPorts);
                    },
                    undo: () => {
                        applyPorts(previousPorts);
                        applyInputExamples(previousInputExamples, previousPorts);
                    },
                });
                if (affectedNodeIds.length > 0) {
                    ruleEditorApi.deleteNodes(affectedNodeIds);
                }
            } else {
                applyPorts(nextPorts);
            }
        },
        [applyInputExamples, applyPorts, inputPortInstanceNodeIds],
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
                const createdPort: RuleBlockPort = {
                    id: ruleBlockUtils.generateInputPortId(),
                    ...value,
                };
                const updatedPorts = [...previousPorts, createdPort];
                if (ruleEditorApi) {
                    ruleEditorApi.startChangeTransaction();
                    ruleEditorApi.executeExternalRuleModelChange({
                        do: () => applyPorts(updatedPorts),
                        undo: () => applyPorts(previousPorts),
                    });
                    if (inputPortDialogState?.mode === "create" && inputPortDialogState.dropPosition) {
                        ruleEditorApi.addNodeByPlugin(
                            "InputPortOperator",
                            "inputPort",
                            inputPortDialogState.dropPosition,
                            { portId: createdPort.id },
                            ruleBlockEditorUtils.inputPortNodeMetaData(createdPort),
                            true,
                        );
                    }
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
                filterAndSort: (operators) =>
                    operators.filter((operator) => operator.pluginType !== "InputPortOperator"),
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
                const currentModel =
                    originalTask.data.parameters.ruleBlockModel ?? ruleBlockUtils.emptyRuleBlockModel();
                const [operatorNodeMap, rootNodes] = ruleUtils.convertToRuleOperatorNodeMap(ruleOperatorNodes, true);
                const inputPortNodes = ruleOperatorNodes.filter((node) => node.pluginType === "InputPortOperator");
                const nextPortDefinitions = ruleBlockUtils.sortRuleBlockPorts(portsRef.current);
                const missingPortIdErrors = ruleBlockUtils.validateMissingPortIds(inputPortNodes, () =>
                    i18next.t("taskViews.ruleBlock.errors.missingPortId"),
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
                    inputExamples: ruleBlockUtils.cloneInputExamples(inputExamplesRef.current),
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
            ): Promise<CodeAutocompleteFieldPartialAutoCompleteResult | undefined> =>
                undefined,
        [],
    );

    const convertToRuleOperatorNodes = React.useCallback(
        (ruleBlockTask: RuleBlockTaskData, ruleOperator: RuleOperatorFetchFnType) =>
            ruleBlockEditorUtils.convertRuleBlockTaskToRuleOperatorNodes(ruleBlockTask, ruleOperator),
        [],
    );

    const applyExampleValues = React.useCallback(
        (nextInputExamples: IRuleBlockInputExample[]) => {
            const ruleEditorApi = ruleEditorRef.current;
            const previousInputExamples = inputExamplesRef.current;
            const previousSelectedExampleIdsForEvaluation = selectedExampleIdsForEvaluationRef.current;
            const normalizedInputExamples = ruleBlockUtils.cloneInputExamples(nextInputExamples);
            const nextSelectedExampleIdsForEvaluation = selectedEvaluationExampleIdsForAvailableExamples(
                previousSelectedExampleIdsForEvaluation,
                normalizedInputExamples,
            );
            if (ruleEditorApi) {
                ruleEditorApi.startChangeTransaction();
                ruleEditorApi.executeExternalRuleModelChange({
                    do: () => {
                        applyInputExamples(normalizedInputExamples);
                        setSelectedExampleIdsForEvaluation(nextSelectedExampleIdsForEvaluation);
                    },
                    undo: () => {
                        applyInputExamples(previousInputExamples);
                        setSelectedExampleIdsForEvaluation(previousSelectedExampleIdsForEvaluation);
                    },
                });
            } else {
                applyInputExamples(normalizedInputExamples);
                setSelectedExampleIdsForEvaluation(nextSelectedExampleIdsForEvaluation);
            }
            closeExampleValuesDialog();
        },
        [applyInputExamples, closeExampleValuesDialog],
    );

    const additionalToolBarComponents = React.useCallback(
        (place: additionalToolbarComponentsPlace) => {
            switch (place) {
                case "afterSaveButton":
                    return (
                        <RuleBlockUsageStatusControl
                            usageState={ruleBlockUsageState}
                            onRefresh={refreshRuleBlockUsage}
                        />
                    );
                case "beforeTools":
                    return (
                        <>
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
                            <Spacing vertical size={"small"} />
                        </>
                    );
                default:
                    return null;
            }
        },
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
                <MenuItem
                    key={`edit-input-port-example-values-${node.nodeId}`}
                    icon="item-settings"
                    text={i18next.t("taskViews.ruleBlock.editExampleValues", "Edit example values")}
                    onClick={() => {
                        closeMenu();
                        openExampleValuesDialog(portId);
                    }}
                />,
            ];
        },
        [openEditInputPortDialog, openExampleValuesDialog],
    );

    const extendClipboardCopy = React.useCallback(
        (_task: RuleClipboardTask, nodeIds: string[]) =>
            ruleBlockPasteUtils.collectRuleBlockClipboardCopy({
                selectedNodeIds: nodeIds,
                ruleOperatorNodes: ruleEditorRef.current?.ruleOperatorNodes() ?? [],
                existingPorts: portsRef.current,
            }),
        [],
    );

    const prepareClipboardPaste = React.useCallback(
        (task: RuleClipboardTask): PreparedClipboardPaste => {
            const previousPorts = portsRef.current;
            const preparedPaste = ruleBlockPasteUtils.prepareRuleBlockClipboardPaste(task, {
                currentProjectId: projectId,
                currentTaskId: ruleBlockTaskId,
                existingPorts: previousPorts,
                createInputPortId: ruleBlockUtils.generateInputPortId,
                inputPortNodeMetaData: ruleBlockEditorUtils.inputPortNodeMetaData,
            });
            if (preparedPaste.createdPorts.length === 0) {
                return {
                    taskData: preparedPaste.taskData,
                };
            }
            const nextPorts = [...previousPorts, ...preparedPaste.createdPorts];
            return {
                taskData: preparedPaste.taskData,
                externalChange: {
                    do: () => applyPorts(nextPorts),
                    undo: () => applyPorts(previousPorts),
                },
            };
        },
        [applyPorts, projectId, ruleBlockTaskId],
    );

    const externalSidebarContextValue = React.useMemo(
        () => ({ reloadToken: sidebarReloadToken }),
        [sidebarReloadToken],
    );
    const ruleEditor = (
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
            extraRuleNodeMenuItems={readOnly ? undefined : extraRuleNodeMenuItems}
            validateConnection={ruleUtils.validateConnection}
            tabs={ruleEditorTabs}
            captureExternalSavedState={captureExternalSavedState}
            restoreExternalSavedState={restoreExternalSavedState}
            extendClipboardCopy={extendClipboardCopy}
            prepareClipboardPaste={prepareClipboardPaste}
            handleSidebarDropRequest={handleSidebarDropRequest}
            showRuleOnly={showRuleOnly}
            readOnly={readOnly}
            instanceId={instanceId}
            saveInitiallyEnabled={false}
        />
    );

    return (
        <ExternalSidebarContext.Provider value={externalSidebarContextValue}>
            <>
                {!isExternalSnapshotMode || hasExternalEvaluationResults ? (
                    <RuleBlockEvaluation
                        projectId={projectId}
                        ruleBlockTaskId={ruleBlockTaskId}
                        numberOfEntitiesToShow={5}
                        getPorts={getPorts}
                        getInputExamples={getInputExamples}
                        getEvaluationInputExamples={getEvaluationInputExamples}
                        getSelectedEvaluationExampleIds={getSelectedEvaluationExampleIds}
                        onOpenExampleValuesDialog={!isExternalSnapshotMode ? openExampleValuesDialog : undefined}
                    >
                        {ruleEditor}
                    </RuleBlockEvaluation>
                ) : (
                    ruleEditor
                )}
                {!isExternalSnapshotMode ? (
                    <>
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
                        {exampleValuesDialogState ? (
                            <ExampleValuesDialog
                                ports={ports}
                                inputExamples={inputExamples}
                                highlightedPortId={exampleValuesDialogState.highlightedPortId}
                                selectedExampleIdsForEvaluation={selectedExampleIdsForEvaluation}
                                onClose={closeExampleValuesDialog}
                                onSelectedExampleIdsForEvaluationChange={setSelectedExampleIdsForEvaluation}
                                onApply={applyExampleValues}
                            />
                        ) : null}
                        {deleteInputPortDialogState ? (
                            <ConfirmDeleteInputPortDialog
                                instanceCount={deleteInputPortDialogState.instanceCount}
                                portLabel={deletePort?.label}
                                onConfirm={confirmDeleteInputPort}
                                onClose={closeDeleteInputPortDialog}
                            />
                        ) : null}
                    </>
                ) : null}
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
    const RULEBLOCK_STATUS_DISPLAY_KEY = "RULEBLOCK_STATUS_DISPLAY_KEY";
    const [t] = useTranslation();
    const [showOverlay, setShowOverlay] = React.useState(
        window.localStorage.getItem(RULEBLOCK_STATUS_DISPLAY_KEY) === "display",
    );
    const [showInitial, setShowInitial] = React.useState(window.localStorage.getItem(RULEBLOCK_STATUS_DISPLAY_KEY));
    const isVisible = usageState.isInUse || usageState.refreshFailed;
    const iconName = usageState.refreshFailed ? "state-warning" : "state-info";

    const content = usageState.refreshFailed
        ? t("taskViews.ruleBlock.usageRefreshError")
        : t("taskViews.ruleBlock.usageInUse");
    const tooltip = `${content} ${t("taskViews.ruleBlock.usageRefreshHint")}`;
    const overlayContent = (
        <Card style={{ maxWidth: "50em" }} elevation={-1} whitespaceAmount={"small"}>
            <CardContent>
                <Notification intent={usageState.refreshFailed ? "warning" : "info"}>{tooltip}</Notification>
            </CardContent>
            <Divider />
            <CardActions>
                <Button onClick={onRefresh} elevated>
                    {t("common.action.refreshStatus")}
                </Button>
                <Button onClick={() => setShowOverlay(false)}>{t("common.action.close")}</Button>
                <CardActionsAux>
                    <Checkbox
                        label={t("common.action.noAutoDisplay")}
                        style={{ marginBottom: 0 }}
                        checked={showInitial === "hide"}
                        onChange={(e) => {
                            window.localStorage.setItem(
                                RULEBLOCK_STATUS_DISPLAY_KEY,
                                e.target.checked ? "hide" : "display",
                            );
                            setShowInitial(window.localStorage.getItem(RULEBLOCK_STATUS_DISPLAY_KEY));
                        }}
                    />
                </CardActionsAux>
            </CardActions>
        </Card>
    );

    if (!isVisible) {
        return null;
    }

    return (
        <>
            <ContextOverlay isOpen={showOverlay} onClose={() => setShowOverlay(false)} content={overlayContent}>
                <IconButton
                    onClick={() => setShowOverlay(true)}
                    intent={usageState.refreshFailed ? "warning" : "accent"}
                    loading={usageState.refreshRunning}
                    disabled={usageState.refreshRunning}
                    name={iconName}
                    text={t("taskViews.ruleBlock.refreshUsage")}
                />
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
