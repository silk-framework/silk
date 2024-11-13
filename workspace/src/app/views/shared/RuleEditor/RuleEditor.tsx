import { RuleEditorModel } from "./model/RuleEditorModel";
import React from "react";
import { RuleEditorView } from "./view/RuleEditorView";
import { RuleEditorContext } from "./contexts/RuleEditorContext";
import { IViewActions } from "../../plugins/PluginRegistry";
import {
    IParameterSpecification,
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSideBarFilterTabConfig,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
    RuleEditorValidationNode,
    RuleOperatorPluginType,
    RuleSaveResult,
} from "./RuleEditor.typings";
import ErrorBoundary from "../../../ErrorBoundary";
import { ReactFlowProvider } from "react-flow-renderer";
import utils from "./RuleEditor.utils";
import { IStickyNote } from "views/taskViews/shared/task.typings";
import { DatasetCharacteristics } from "../typings";
import { ReactFlowHotkeyContext } from "@eccenca/gui-elements/src/cmem/react-flow/extensions/ReactFlowHotkeyContext";
import { Notification } from "@eccenca/gui-elements";
import { diErrorMessage } from "@ducks/error/typings";

/** Function to fetch the rule operator spec. */
export type RuleOperatorFetchFnType = (
    pluginId: string,
    pluginType?: RuleOperatorPluginType
) => IRuleOperator | undefined;

export interface RuleEditorProps<RULE_TYPE, OPERATOR_TYPE> {
    /** The ID of the instance. If multiple instances are used in parallel, they need to have unique IDs, else there can be interferences. */
    instanceId: string;
    /** Optional title that is shown above the toolbar. */
    editorTitle?: string;
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    taskId: string;
    /** Function to fetch the actual task data to initialize the editor. */
    fetchRuleData: (projectId: string, taskId: string) => Promise<RULE_TYPE | undefined> | RULE_TYPE | undefined;
    /** Save rule. If true is returned saving was successful, else it failed. */
    saveRule: (
        ruleOperatorNodes: IRuleOperatorNode[],
        stickyNotes: IStickyNote[],
        originalRuleData: RULE_TYPE
    ) => Promise<RuleSaveResult> | RuleSaveResult;
    /** Fetch available rule operators. */
    fetchRuleOperators: () => Promise<OPERATOR_TYPE[] | undefined> | OPERATOR_TYPE[] | undefined;
    /** Converts the custom format to the internal rule operator format. */
    convertRuleOperator: (
        op: OPERATOR_TYPE,
        addAdditionParameterSpecifications: (
            pluginDetails: OPERATOR_TYPE
        ) => [id: string, spec: IParameterSpecification][]
    ) => IRuleOperator;
    /** Converts the external rule representation into the internal rule representation. */
    convertToRuleOperatorNodes: (ruleData: RULE_TYPE, ruleOperator: RuleOperatorFetchFnType) => IRuleOperatorNode[];
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
    /** Additional rule operator plugins that are not returned via the fetchRuleOperators method. */
    additionalRuleOperators?: IRuleOperator[];
    /** Function to add additional parameter (specifications) to a rule operator based on the original operator. */
    addAdditionParameterSpecifications?: (operator: OPERATOR_TYPE) => [id: string, spec: IParameterSpecification][];
    /** Specifies the allowed connections. Only connections that return true are allowed. */
    validateConnection: (
        fromRuleOperatorNode: RuleEditorValidationNode,
        toRuleOperatorNode: RuleEditorValidationNode,
        targetPortIdx: number
    ) => boolean;
    /** Tabs that allow to show different rule operators or only a subset. */
    tabs?: (IRuleSideBarFilterTabConfig | IRuleSidebarPreConfiguredOperatorsTabConfig)[];
    /** Additional components that will be placed in the tool bar left to the save button. */
    additionalToolBarComponents?: () => JSX.Element | JSX.Element[];
    /** parent configuration to extract stickyNote from taskData*/
    getStickyNotes?: (taskData: RULE_TYPE | undefined) => IStickyNote[];
    /** When enabled only the rule is shown without side- and toolbar and any other means to edit the rule. */
    showRuleOnly: boolean;
    /** When enabled the mini map is not displayed. */
    hideMinimap?: boolean;
    /** Defines minimun and maximum of the available zoom levels */
    zoomRange?: [number, number];
    /** After the initial fit to view, zoom to the specified Zoom level to avoid showing too small nodes. */
    initialFitToViewZoomLevel?: number;
    /** Fetches dataset characteristics for all input datasets relevant in the rule editor. These are used for the 'PathInputOperator' type.
     * The key is the corresponding plugin ID. */
    fetchDatasetCharacteristics?: (
        taskData: RULE_TYPE | undefined
    ) => Map<string, DatasetCharacteristics> | Promise<Map<string, DatasetCharacteristics>>;
    /** Returns for a path input plugin and a path the type of the given path. Returns undefined if either the plugin does not exist or the path data is unknown. */
    inputPathPluginPathType?: (inputPathPluginId: string, path: string) => string | undefined;
    /** allow the width and height of nodes to be adjustable */
    allowFlexibleSize?: boolean;
}

const READ_ONLY_QUERY_PARAMETER = "readOnly";
/**
 * Generic rule editor that can be used to build tree-line rule operator graphs.
 */
const RuleEditor = <TASK_TYPE extends object, OPERATOR_TYPE extends object>({
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
    editorTitle,
    getStickyNotes = () => [],
    showRuleOnly,
    hideMinimap,
    zoomRange,
    initialFitToViewZoomLevel,
    instanceId,
    fetchDatasetCharacteristics,
    inputPathPluginPathType,
    allowFlexibleSize,
}: RuleEditorProps<TASK_TYPE, OPERATOR_TYPE>) => {
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
        undefined
    );
    // The list of available operators that can be added to the canvas
    const [operatorList, setOperatorList] = React.useState<IRuleOperator[] | undefined>(undefined);
    /* A map that connects pluginId to all operators with that ID. In theory there could be plugins with the same ID in different plugin types,
       so we need to have an array. */
    const [operatorMap, setOperatorMap] = React.useState<Map<string, IRuleOperator[]> | undefined>(undefined);
    const [operatorSpec, setOperatorSpec] = React.useState<
        Map<string, Map<string, IParameterSpecification>> | undefined
    >(undefined);
    const readOnlyMode =
        (new URLSearchParams(window.location.search).get(READ_ONLY_QUERY_PARAMETER) ?? "").toLowerCase() === "true";
    const [lastSaveResult, setLastSaveResult] = React.useState<RuleSaveResult | undefined>(undefined);
    // Dataset characteristics used for the 'PathInputOperator' type. The key is the corresponding plugin ID.
    const [datasetCharacteristics, setDatasetCharacteristics] = React.useState<Map<string, DatasetCharacteristics>>(
        new Map()
    );
    const [hotKeysDisabled, setHotKeysDisabled] = React.useState<boolean>(false);

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

    // Fetch the task data
    React.useEffect(() => {
        fetchData();
    }, [projectId, taskId]);

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
                ops.map((op) => [op.pluginId, new Map(Object.entries(op.parameterSpecification))])
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

    const fetchData = async () => {
        setTaskDataLoading(true);
        try {
            const data = await fetchRuleData(projectId, taskId);
            if (fetchDatasetCharacteristics) {
                const datasetCharacteristics = await fetchDatasetCharacteristics(data);
                setDatasetCharacteristics(datasetCharacteristics);
            }
            setTaskData(data);
        } finally {
            setTaskDataLoading(false);
        }
    };

    const saveRuleOperatorNodes = async (
        ruleNodeOperators: IRuleOperatorNode[],
        stickyNotes: IStickyNote[] = []
    ): Promise<RuleSaveResult> => {
        if (taskData) {
            const result = await saveRule(ruleNodeOperators, stickyNotes, taskData);
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
                readOnlyMode: showRuleOnly || readOnlyMode,
                additionalToolBarComponents,
                lastSaveResult: lastSaveResult,
                editorTitle,
                stickyNotes: getStickyNotes(taskData),
                showRuleOnly,
                hideMinimap,
                zoomRange,
                initialFitToViewZoomLevel,
                instanceId,
                datasetCharacteristics,
                inputPathPluginPathType,
                allowFlexibleSize,
            }}
        >
            <ReactFlowHotkeyContext.Provider
                value={{
                    disableHotKeys,
                    hotKeysDisabled,
                }}
            >
                <RuleEditorModel>
                    <RuleEditorView
                        showRuleOnly={showRuleOnly}
                        hideMinimap={hideMinimap}
                        zoomRange={zoomRange}
                        readOnlyMode={readOnlyMode}
                    />
                </RuleEditorModel>
            </ReactFlowHotkeyContext.Provider>
        </RuleEditorContext.Provider>
    );
};

const WrappedRuleEditor = <RULE_TYPE extends object, OPERATOR_TYPE extends object>(
    props: RuleEditorProps<RULE_TYPE, OPERATOR_TYPE>
) => (
    <ErrorBoundary>
        <ReactFlowProvider>
            <RuleEditor<RULE_TYPE, OPERATOR_TYPE> {...props} />
        </ReactFlowProvider>
    </ErrorBoundary>
);

export default WrappedRuleEditor;
