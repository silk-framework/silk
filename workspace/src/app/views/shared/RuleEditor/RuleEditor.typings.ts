import { NodeContentProps } from "gui-elements/src/extensions/react-flow/nodes/NodeDefault";
import { PluginType, RuleOperatorType } from "@ducks/shared/typings";
import { ValidIconName } from "gui-elements/src/components/Icon/canonicalIconNames";
import { IPreConfiguredRuleOperator } from "./view/sidebar/RuleEditorOperatorSidebar.typings";

export type PathInputOperator = "PathInputOperator";

export type RuleOperatorPluginType = PathInputOperator | RuleOperatorType;

interface IRuleOperatorBase {
    /** Plugin type. */
    pluginType: RuleOperatorPluginType | PluginType | "unknown";
    /** The operator plugin ID. Taken from the list of available operators. */
    pluginId: string;
    /** The label that will be displayed in the node header. */
    label: string;
    /** Icon that should be displayed for the operator. */
    icon?: string;
    /** Specification of input ports of the operator node. */
    portSpecification: IPortSpecification;
}

/** The specification of the number of ports. */
export interface IPortSpecification {
    /** Minimal number of input ports. */
    minInputPorts: number;
    /** Max. number of input ports. If this is missing, then there is a unlimited number allowed. */
    maxInputPorts?: number;
}

/** Rule operator that can be added to a rule. Will be displayed in the sidebar. */
export interface IRuleOperator extends IRuleOperatorBase {
    /** Optional description that will be displayed in the side bar when search matches. */
    description?: string;
    /** Categories the rule operator is member of. Used for filtering by category. */
    categories?: string[];
    /** The specification of the supported parameters. */
    parameterSpecification: {
        [parameterKey: string]: IParameterSpecification;
    };
    /** Tags that will be displayed in the node operator. */
    tags: string[];
}

/** A single node in the rule operator tree. This is displayed in the editor canvas. */
export interface IRuleOperatorNode extends IRuleOperatorBase {
    /** Unique node ID. */
    nodeId: string;
    /** Parameter values. */
    parameters: RuleOperatorNodeParameters;
    /** The position on the canvas. */
    position?: NodePosition;
    /** The input node IDs. */
    inputs: (string | undefined)[];
    /** Tags that will be displayed inside the node. */
    tags?: string[];
}

export interface IParameterSpecification {
    /** Parameter label */
    label: string;
    /** Parameter description. */
    description?: string;
    /** The type of the parameter. */
    type: RuleParameterType;
    /** If the parameter can be left empty or is required. */
    required: boolean;
    /** If this parameter should only be shown in advanced mode. */
    advanced: boolean;
    /** The default value for the parameter. */
    defaultValue: string;
}

export type RuleParameterType =
    | "boolean"
    | "int"
    | "float"
    | "textField"
    | "code"
    | "password"
    | "resource"
    | "textArea"
    | "pathInput";

interface NodePosition {
    x: number;
    y: number;
}

export interface RuleOperatorNodeParameters {
    [parameterKey: string]: string | undefined;
}

/** Rule editor node with required business data. For convenience. */
export interface NodeContentPropsWithBusinessData<T> extends NodeContentProps<T> {
    businessData: T;
}

/** Business data for rule editor nodes. */
export interface IRuleNodeData {
    // If this is a node with dynamic port configuration
    dynamicPorts?: boolean;
    // The original rule operator node this node was created with.
    originalRuleOperatorNode: IRuleOperatorNode;
    // Update switch to force content updates
    updateSwitch?: boolean;
}

/** Sidebar tabs */
interface IRuleSideBarTabBaseConfig {
    // Unique ID of the tab
    id: string;
    // Optional icon that is displayed left to the label
    icon?: ValidIconName;
    // The tab label
    label?: string;
}

/** Filters (and sorts) the operator rule list and shows it in the tab. */
export interface IRuleSideBarFilterTabConfig extends IRuleSideBarTabBaseConfig {
    filterAndSort: (ruleOperators: IRuleOperator[]) => IRuleOperator[];
}

/** Allow to fetch and list pre-configured operators in a tab. This is used to have e.g. pre-configured path operators. */
export interface IRuleSidebarPreConfiguredOperatorsTabConfig<ListItem = any> extends IRuleSideBarTabBaseConfig {
    /** Fetches an array of items that can be transformed into rule operators. */
    fetchOperators: (langPref: string) => ListItem[] | undefined | Promise<ListItem[] | undefined>;
    /** Converts an operator into a rule operator. Only list items are converted that will currently be shown. */
    convertToOperator: (listItem: ListItem) => IPreConfiguredRuleOperator;
    /** Returns if the given item is a ListItem, else it is a IRuleOperator. */
    isOriginalOperator: (item: ListItem | IRuleOperator) => boolean;
    /** If the text query changes then the following search text of the ListItem is used.
     * This is an optimization, if we deal with a lot of elements, e.g. an array of strings, and don't want to convert them first into rule operators.
     **/
    itemSearchText: (listItem: ListItem) => string;
    /** Returns the label of the item. Also an optimization to not convert to IPreConfiguredRuleOperator first. */
    itemLabel: (listItem: ListItem) => string;
}
