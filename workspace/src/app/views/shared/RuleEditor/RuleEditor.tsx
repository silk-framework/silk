import { RuleEditorModel } from "./model/RuleEditorModel";
import React from "react";
import { RuleEditorView } from "./view/RuleEditorView";
import { RuleEditorContext } from "./contexts/RuleEditorContext";
import { RuleEditorModelContext } from "./contexts/RuleEditorModelContext";
import { IViewActions } from "../../plugins/PluginRegistry";
import {
    IParameterSpecification,
    IRuleOperator,
    IRuleOperatorNode,
    RuleEditorPatchableNodeProjection,
    IRuleSideBarFilterTabConfig,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
    RuleOperatorPluginType,
    PathMetaDataFunctions,
    RuleEditorValidationNode,
    RuleSaveResult,
    HandleRuleEditorSidebarDropRequest,
    RuleOperatorNodeParameters,
} from "./RuleEditor.typings";
import {
    ExternalRuleModelChangeCallbacks,
    PrepareClipboardPaste,
    RuleClipboardTask,
} from "./model/RuleEditorModel.typings";
import ErrorBoundary from "../../../ErrorBoundary";
import { ReactFlowProvider } from "react-flow-renderer";
import utils from "./RuleEditor.utils";
import { DatasetCharacteristics } from "../typings";
import { ReactFlowHotkeyContext } from "@eccenca/gui-elements/src/cmem/react-flow/extensions/ReactFlowHotkeyContext";
import { StickyNote } from "@eccenca/gui-elements";
import { CodeAutocompleteFieldPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/organisms/AutoSuggestion/AutoSuggestion";
import { InitialRuleHighlighting } from "../../taskViews/transform/transform.types";
import { PluginType } from "@ducks/shared/typings";

/** Function to fetch the rule operator spec. */
export type RuleOperatorFetchFnType = (
    pluginId: string,
    pluginType?: RuleOperatorPluginType | PluginType,
) => IRuleOperator | undefined;

/** Place definitions where additional components could be inserted. */
export type additionalToolbarComponentsPlace = "afterSaveButton" | "beforeTools" | "beforeActionWidget";

/** Properties that are used in multiple relevant interfaces in the rule editor. */
export interface RuleEditorBaseProps {
    /** The project context. */
    projectId: string;
    /** Optional title that is shown above the toolbar. */
    editorTitle?: string;
    /** Validate a connection. Specifies which connections are allowed between nodes. */
    validateConnection: (
        fromRuleOperatorNode: RuleEditorValidationNode,
        toRuleOperatorNode: RuleEditorValidationNode,
        targetPortIdx: number,
    ) => boolean;
    /** Tabs that allow to show different rule operators or only a subset. The first tab will always be selected first. */
    tabs?: (IRuleSideBarFilterTabConfig | IRuleSidebarPreConfiguredOperatorsTabConfig)[];
    /** Task view actions. */
    viewActions?: IViewActions;
    /** Optional functions to get more information about specific properties/paths. */
    pathMetaData?: PathMetaDataFunctions;
    /**
     * Fetches partial auto-completion results for the transforms task input paths, i.e. any part of a path could be auto-completed
     * without replacing the complete path.
     */
    partialAutoCompletion: (
        inputType: "source" | "target",
    ) => (
        inputString: string,
        cursorPosition: number,
    ) => Promise<CodeAutocompleteFieldPartialAutoCompleteResult | undefined>;
    /** True if the save button should be initially enabled even when nothing has been changed by the user.
     * This should be enabled for example when the initial rule is not persisted yet in the backend. */
    saveInitiallyEnabled: boolean;
    /** Initially highlights the given operator nodes and shows a message explaining why the nodes are highlighted.
     * When the notification is closed the highlighting of the nodes is removed again.  */
    initialHighlighting?: InitialRuleHighlighting;
    /** Additional components that will be placed in the toolbar left to the save button. */
    additionalToolBarComponents?: (
        component: additionalToolbarComponentsPlace,
    ) => React.JSX.Element | React.JSX.Element[] | null;
    /** Optional additional menu entries for a specific rule node. These are rendered right before the Remove entry. */
    extraRuleNodeMenuItems?: (node: IRuleOperatorNode, closeMenu: () => void) => React.JSX.Element[] | undefined;
    /** Optional hook to attach editor-owned clipboard data, e.g. logical entities referenced by copied nodes. */
    extendClipboardCopy?: (task: RuleClipboardTask, nodeIds: string[]) => unknown;
    /** Optional hook to validate or rewrite a clipboard payload and enqueue parent-owned side effects for undo/redo. */
    prepareClipboardPaste?: PrepareClipboardPaste;
    /** Optional hook for special sidebar entries that open a creation flow on drop instead of materializing a normal node directly. */
    handleSidebarDropRequest?: HandleRuleEditorSidebarDropRequest;
    /** Optional hook that is called whenever the current rule-node projection changes due to canvas edits. */
    onRuleOperatorNodesChange?: (ruleOperatorNodes: IRuleOperatorNode[]) => void;
    /** When enabled only the rule is shown without side- and toolbar and any other means to edit the rule. */
    showRuleOnly?: boolean;
    /** If set to true the editor is permanently read-only. */
    readOnly?: boolean;
    /** When enabled the mini map is not displayed. */
    hideMinimap?: boolean;
    /** Defines minimum and maximum of the available zoom levels */
    zoomRange?: [number, number];
    /** After the initial fit to view, zoom to the specified Zoom level to avoid showing too small nodes. */
    initialFitToViewZoomLevel?: number;
    /** The ID of the instance. If multiple instances are used in parallel, they need to have unique IDs, else there can be interferences. */
    instanceId: string;
    /** Optional overlay content rendered inside the rule editor context tree, e.g. modals that should affect editor-local contexts. */
    overlayContent?: React.ReactNode;
}

export interface RuleEditorProps<RULE_TYPE, OPERATOR_TYPE> extends RuleEditorBaseProps {
    /** The task the rules are being edited of. */
    taskId: string;
    /** Function to fetch the actual task data to initialize the editor. */
    fetchRuleData: (projectId: string, taskId: string) => Promise<RULE_TYPE | undefined> | RULE_TYPE | undefined;
    /** Save rule. If true is returned saving was successful, else it failed. */
    saveRule: (
        ruleOperatorNodes: IRuleOperatorNode[],
        stickyNotes: StickyNote[],
        originalRuleData: RULE_TYPE,
    ) => Promise<RuleSaveResult> | RuleSaveResult;
    /** Fetch available rule operators. */
    fetchRuleOperators: () => Promise<OPERATOR_TYPE[] | undefined> | OPERATOR_TYPE[] | undefined;
    /** Converts the custom format to the internal rule operator format. */
    convertRuleOperator: (
        op: OPERATOR_TYPE,
        addAdditionParameterSpecifications: (
            pluginDetails: OPERATOR_TYPE,
        ) => [id: string, spec: IParameterSpecification][],
    ) => IRuleOperator;
    /** Converts the external rule representation into the internal rule representation. */
    convertToRuleOperatorNodes: (ruleData: RULE_TYPE, ruleOperator: RuleOperatorFetchFnType) => IRuleOperatorNode[];
    /** Additional rule operator plugins that are not returned via the fetchRuleOperators method. */
    additionalRuleOperators?: IRuleOperator[];
    /** Function to add additional parameter (specifications) to a rule operator based on the original operator. */
    addAdditionParameterSpecifications?: (operator: OPERATOR_TYPE) => [id: string, spec: IParameterSpecification][];
    /** Specifies the allowed connections. Only connections that return true are allowed. */
    validateConnection: (
        fromRuleOperatorNode: RuleEditorValidationNode,
        toRuleOperatorNode: RuleEditorValidationNode,
        targetPortIdx: number,
    ) => boolean;
    /** Tabs that allow to show different rule operators or only a subset. */
    tabs?: (IRuleSideBarFilterTabConfig | IRuleSidebarPreConfiguredOperatorsTabConfig)[];
    /** Additional components that will be placed in the tool bar left to the save button. */
    additionalToolBarComponents?: RuleEditorBaseProps["additionalToolBarComponents"];
    /** parent configuration to extract stickyNote from taskData*/
    getStickyNotes?: (taskData: RULE_TYPE | undefined) => StickyNote[];
    /** When enabled only the rule is shown without side- and toolbar and any other means to edit the rule. */
    showRuleOnly: boolean;
    /** Fetches dataset characteristics for all input datasets relevant in the rule editor. These are used for the 'PathInputOperator' type.
     * The key is the corresponding plugin ID. */
    fetchDatasetCharacteristics?: (
        taskData: RULE_TYPE | undefined,
    ) => Map<string, DatasetCharacteristics> | Promise<Map<string, DatasetCharacteristics>>;
    /** Optional hook to capture additional parent-owned state that should become part of the editor saved-state snapshot. */
    captureExternalSavedState?: () => unknown;
    /** Optional hook to restore additional parent-owned state from the latest saved-state snapshot. */
    restoreExternalSavedState?: (savedState: unknown) => void;
}

export interface RuleEditorExternalApi {
    /** Starts a new undo/redo transaction in the editor model. */
    startChangeTransaction(): void;
    /** Returns the current rule nodes of the editor model. */
    ruleOperatorNodes(): IRuleOperatorNode[];
    /** Deletes current canvas nodes and records the change in model history. */
    deleteNodes(nodeIds: string[]): void;
    /** Executes an external non-canvas change and registers it in the current undo/redo transaction. */
    executeExternalRuleModelChange(change: ExternalRuleModelChangeCallbacks): void;
    /** Updates current rule nodes' rendered metadata projection and records the change in model history. */
    updateRuleOperatorNodeMetaData(
        nodeIds: string[],
        patch: (node: IRuleOperatorNode) => RuleEditorPatchableNodeProjection,
    ): void;
    /** Adds a node by plugin type and ID through the current editor model, e.g. for parent-owned create-on-drop flows. */
    addNodeByPlugin(
        pluginType: string,
        pluginId: string,
        position: { x: number; y: number },
        overwriteParameterValues?: RuleOperatorNodeParameters,
        overwriteNodeMetaData?: RuleEditorPatchableNodeProjection,
        isCanvasPosition?: boolean,
    ): void;
}

const READ_ONLY_QUERY_PARAMETER = "readOnly";

/** Bridge the model-only context to the parent-facing RuleEditor ref.
 * Use this from a RuleEditor instance via `const ref = React.useRef<RuleEditorExternalApi>(null)` and
 * `<RuleEditor ref={ref} ... />`, e.g. when a parent needs to inspect current rule nodes or update their metadata. */
const RuleEditorExternalApiBridge = React.forwardRef<RuleEditorExternalApi>((_, ref) => {
    const ruleEditorModelContext = React.useContext(RuleEditorModelContext);

    React.useImperativeHandle(
        ref,
        () => ({
            startChangeTransaction: ruleEditorModelContext.executeModelEditOperation.startChangeTransaction,
            ruleOperatorNodes: ruleEditorModelContext.ruleOperatorNodes,
            deleteNodes: ruleEditorModelContext.executeModelEditOperation.deleteNodes,
            executeExternalRuleModelChange: ruleEditorModelContext.executeExternalRuleModelChange,
            updateRuleOperatorNodeMetaData: ruleEditorModelContext.updateRuleOperatorNodeMetaData,
            addNodeByPlugin: ruleEditorModelContext.executeModelEditOperation.addNodeByPlugin,
        }),
        [ruleEditorModelContext],
    );

    return null;
});

/**
 * Generic rule editor that can be used to build tree-line rule operator graphs.
 */
const RuleEditorInner = <TASK_TYPE extends object, OPERATOR_TYPE extends object>(
    {
        projectId,
        taskId,
        fetchRuleData,
        fetchRuleOperators,
        convertRuleOperator,
        convertToRuleOperatorNodes,
        saveRule,
        additionalRuleOperators,
        addAdditionParameterSpecifications,
        validateConnection,
        tabs,
        viewActions,
        additionalToolBarComponents,
        extraRuleNodeMenuItems,
        editorTitle,
        getStickyNotes = () => [],
        showRuleOnly,
        readOnly,
        hideMinimap,
        zoomRange,
        initialFitToViewZoomLevel,
        instanceId,
        overlayContent,
        fetchDatasetCharacteristics,
        captureExternalSavedState,
        restoreExternalSavedState,
        extendClipboardCopy,
        prepareClipboardPaste,
        handleSidebarDropRequest,
        onRuleOperatorNodesChange,
        pathMetaData,
        partialAutoCompletion,
        saveInitiallyEnabled,
        initialHighlighting,
    }: RuleEditorProps<TASK_TYPE, OPERATOR_TYPE>,
    ref: React.ForwardedRef<RuleEditorExternalApi>,
) => {
    // The task that contains the rule, e.g. transform or linking task
    const [taskData, setTaskData] = React.useState<TASK_TYPE | undefined>(undefined);
    // True while the task data is loaded
    const [taskDataLoading, setTaskDataLoading] = React.useState<boolean>(false);
    // The available operators for building the rule
    const [operators, setOperators] = React.useState<OPERATOR_TYPE[]>([]);
    // True while operators are loaded
    const [operatorsLoading, setOperatorsLoading] = React.useState<boolean>(false);
    // The internal rule operator node model
    const [initialRuleOperatorNodes, setInitialRuleOperatorNodes] = React.useState<IRuleOperatorNode[] | undefined>(
        undefined,
    );
    // The list of available operators that can be added to the canvas
    const [operatorList, setOperatorList] = React.useState<IRuleOperator[] | undefined>(undefined);
    /* A map that connects pluginId to all operators with that ID. In theory there could be plugins with the same ID in different plugin types,
       so we need to have an array. */
    const [operatorMap, setOperatorMap] = React.useState<Map<string, IRuleOperator[]> | undefined>(undefined);
    const [operatorSpec, setOperatorSpec] = React.useState<
        Map<string, Map<string, IParameterSpecification>> | undefined
    >(undefined);
    const readOnlyFromQuery =
        (new URLSearchParams(window.location.search).get(READ_ONLY_QUERY_PARAMETER) ?? "").toLowerCase() === "true";
    const readOnlyMode = !!readOnly || readOnlyFromQuery;
    const [lastSaveResult, setLastSaveResult] = React.useState<RuleSaveResult | undefined>(undefined);
    // Dataset characteristics used for the 'PathInputOperator' type. The key is the corresponding plugin ID.
    const [datasetCharacteristics, setDatasetCharacteristics] = React.useState<Map<string, DatasetCharacteristics>>(
        new Map(),
    );
    const [hotKeysDisabled, setHotKeysDisabled] = React.useState<boolean>(false);
    const fetchRuleDataRef = React.useRef(fetchRuleData);
    const fetchDatasetCharacteristicsRef = React.useRef(fetchDatasetCharacteristics);

    fetchRuleDataRef.current = fetchRuleData;
    fetchDatasetCharacteristicsRef.current = fetchDatasetCharacteristics;

    const disableHotKeys = React.useCallback((disabled: boolean) => {
        setHotKeysDisabled(disabled);
    }, []);

    /** This should be used instead of calling setLastSaveResult directly. */
    const updateLastSaveResult = (saveResult: RuleSaveResult | undefined) => {
        // This makes sure that the notifications are shown again
        setLastSaveResult(undefined);
        if (saveResult !== undefined) {
            setLastSaveResult(saveResult);
        }
    };

    // Convert task data to internal model
    React.useEffect(() => {
        if (taskData && operatorMap) {
            const getOperatorNode = (pluginId: string, pluginType?: string): IRuleOperator | undefined => {
                return utils.getOperatorNode(pluginId, operatorMap, pluginType);
            };
            const nodes = convertToRuleOperatorNodes(taskData, getOperatorNode);
            setInitialRuleOperatorNodes(nodes);
        }
    }, [taskData, operatorMap]);

    // Convert available operators
    React.useEffect(() => {
        if (operators.length > 0) {
            const ops: IRuleOperator[] = [];
            (additionalRuleOperators ?? []).forEach((additionalOp) => {
                ops.push(additionalOp);
            });
            const addAdditionalParams = addAdditionParameterSpecifications ?? (() => []);
            operators.forEach((op) => ops.push(convertRuleOperator(op, addAdditionalParams)));
            const operatorSpec = new Map(
                ops.map((op) => [op.pluginId, new Map(Object.entries(op.parameterSpecification))]),
            );

            const operatorMap = new Map<string, IRuleOperator[]>();
            ops.forEach((op) => operatorMap.set(op.pluginId, []));
            ops.forEach((op) => {
                operatorMap.get(op.pluginId)!!.push(op);
            });
            setOperatorSpec(operatorSpec);
            setOperatorList(ops);
            setOperatorMap(operatorMap);
        }
    }, [operators]);

    const fetchData = React.useCallback(async () => {
        setTaskDataLoading(true);
        try {
            const data = await fetchRuleDataRef.current(projectId, taskId);
            if (fetchDatasetCharacteristicsRef.current) {
                const datasetCharacteristics = await fetchDatasetCharacteristicsRef.current(data);
                setDatasetCharacteristics(datasetCharacteristics);
            }
            setTaskData(data);
            return data;
        } finally {
            setTaskDataLoading(false);
        }
    }, [projectId, taskId]);

    // Fetch the task data
    React.useEffect(() => {
        fetchData();
    }, [fetchData, projectId, taskId]);

    const saveRuleOperatorNodes = async (
        ruleNodeOperators: IRuleOperatorNode[],
        stickyNotes: StickyNote[] = [],
    ): Promise<RuleSaveResult> => {
        if (taskData) {
            const result = await saveRule(ruleNodeOperators, stickyNotes, taskData);
            if (result.success) {
                await fetchData();
            }
            updateLastSaveResult(result);
            viewActions?.onSave && viewActions.onSave();
            return result;
        } else {
            const error = {
                success: false,
                errorMessage: "No task data loaded, cannot save!",
                nodeErrors: [],
            };
            updateLastSaveResult(error);
            // unlikely to ever happen
            return error;
        }
    };

    // Fetch the operators
    React.useEffect(() => {
        fetchOperators();
    }, [projectId, taskId]);

    const fetchOperators = async () => {
        setOperatorsLoading(true);
        try {
            setOperators((await fetchRuleOperators()) ?? []);
        } finally {
            setOperatorsLoading(false);
        }
    };

    return (
        <RuleEditorContext.Provider
            value={{
                projectId,
                editedItemId: taskId,
                editedItem: taskData,
                operatorList,
                editedItemLoading: taskDataLoading,
                operatorListLoading: operatorsLoading,
                initialRuleOperatorNodes,
                saveRule: saveRuleOperatorNodes,
                convertRuleOperatorToRuleNode: utils.defaults.convertRuleOperatorToRuleNode,
                operatorSpec,
                validateConnection,
                tabs,
                viewActions,
                readOnlyMode,
                additionalToolBarComponents,
                extraRuleNodeMenuItems,
                lastSaveResult: lastSaveResult,
                editorTitle,
                stickyNotes: getStickyNotes(taskData),
                showRuleOnly,
                readOnly,
                hideMinimap,
                zoomRange,
                initialFitToViewZoomLevel,
                instanceId,
                datasetCharacteristics,
                captureExternalSavedState,
                restoreExternalSavedState,
                extendClipboardCopy,
                prepareClipboardPaste,
                handleSidebarDropRequest,
                onRuleOperatorNodesChange,
                pathMetaData,
                partialAutoCompletion,
                saveInitiallyEnabled,
                initialHighlighting,
            }}
        >
            <ReactFlowHotkeyContext.Provider
                value={{
                    disableHotKeys,
                    hotKeysDisabled,
                }}
            >
                <RuleEditorModel>
                    <>
                        {/* The external API must be created inside RuleEditorModel so it can delegate to the current
                            model context, while the parent still accesses it through the normal RuleEditor ref. */}
                        <RuleEditorExternalApiBridge ref={ref} />
                        <RuleEditorView
                            showRuleOnly={showRuleOnly}
                            hideMinimap={hideMinimap}
                            zoomRange={zoomRange}
                            readOnlyMode={readOnlyMode}
                            overlayContent={overlayContent}
                        />
                    </>
                </RuleEditorModel>
            </ReactFlowHotkeyContext.Provider>
        </RuleEditorContext.Provider>
    );
};

// Keep the imperative bridge opt-in per instance, e.g. `const ref = React.useRef<RuleEditorExternalApi>(null)`.
const RuleEditor = React.forwardRef(RuleEditorInner) as <TASK_TYPE extends object, OPERATOR_TYPE extends object>(
    props: RuleEditorProps<TASK_TYPE, OPERATOR_TYPE> & React.RefAttributes<RuleEditorExternalApi>,
) => React.ReactElement;

const Provider: React.FC<{ children: React.JSX.Element }> = ReactFlowProvider;
const WrappedRuleEditor = React.forwardRef(function WrappedRuleEditorInner<
    RULE_TYPE extends object,
    OPERATOR_TYPE extends object,
>(props: RuleEditorProps<RULE_TYPE, OPERATOR_TYPE>, ref: React.ForwardedRef<RuleEditorExternalApi>) {
    return (
        <ErrorBoundary>
            <Provider>
                <RuleEditor<RULE_TYPE, OPERATOR_TYPE> {...props} ref={ref} />
            </Provider>
        </ErrorBoundary>
    );
}) as <RULE_TYPE extends object, OPERATOR_TYPE extends object>(
    props: RuleEditorProps<RULE_TYPE, OPERATOR_TYPE> & React.RefAttributes<RuleEditorExternalApi>,
) => React.ReactElement;

export default WrappedRuleEditor;
