import { NodeContentProps } from "gui-elements/src/extensions/react-flow/nodes/NodeDefault";
import { RuleOperatorType } from "@ducks/shared/typings";

export type PathInputOperator = "PathInputOperator";

interface IRuleOperatorBase {
    /** Plugin type. */
    pluginType: string | PathInputOperator | RuleOperatorType;
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
    | "textArea";

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
