/** Common properties of all operator nodes. */
export interface IOperatorNode {
    /** The ID of the operator node. */
    id: string;
}

/** A value input that can either be plugged into a comparison or transform operator. */
export interface IValueInput extends IOperatorNode {
    /** A value input can either be a path input or a transform operator. */
    type: "pathInput" | "transformInput";
}

/** Input operator that returns inputs for a specific path expression of a resource. */
export interface IPathInput extends IValueInput {
    type: "pathInput";
    /** The input path expression. Silk path expression syntax.*/
    path: string;
}

/** Input operator that returns transformed values. */
export interface ITransformOperator extends IValueInput {
    type: "transformInput";
    /** The transform operator plugin ID. */
    function: string;
    /** Inputs of the transformation. */
    inputs: IValueInput[];
    /** Parameter values to configure the transform operator. */
    parameters: IOperatorNodeParameters;
}

/** Parameters of a rule operator. */
export interface IOperatorNodeParameters {
    [key: string]: IOperatorParameterValue;
}

/** Values of a parameterized rule operator. */
export interface IOperatorParameterValue {
    defaultValue?: string;
}
