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

/** Labelled value. In order to show human-readable versions of values, e.g. in auto-completion. */
export interface IOperatorNodeParameterValueWithLabel {
    value: string;
    label?: string;
}

/** Parameters of a rule operator. */
export interface IOperatorNodeParameters {
    [key: string]: string | IOperatorNodeParameterValueWithLabel;
}

/** Rule layout information. */
export interface RuleLayout {
    nodePositions: {
        [nodeId: string]: {
            x: number;
            y: number;
            width: number | null;
        };
    };
}

/** A data source path with meta data. */
export interface PathWithMetaData {
    /** The actual value of the path, i.e. Silk path expression. */
    value: string;
    /** An optional human-readable label. */
    label?: string;
    /** A human-readable type of the value of the path. */
    valueType: string;
    /** Optional prefix to generate unique IDs for lists. */
    _idPrefix?: string;
}

/** An entity. The basic item that is being processed and produced by most tasks. */
export interface IEntity {
    uri: string;
    values: string[][];
    schema?: IEntitySchema;
}

/** The entity schema of an entity. */
export interface IEntitySchema {
    /** The type (URI) of this entity. */
    typeUri: string;
    /** The paths of the entity as normalized strings. */
    paths: string[];
    /** The filter expression for this entity. */
    filter: string;
    /** The sub path leading to this entity, e.g. if the entity is inside a nested data structure (JSON, XML etc.). */
    subPath: string;
}
