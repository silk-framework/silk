import { IInputSource } from "../task.typings";
import { IOperatorNode, IOperatorNodeParameters, IValueInput } from "../rule.typings";

/** A linking rule. */
export interface ILinkingRule {
    /** The operator rule tree that specifies how to compare pairs of source/target resources to decide if a link between should be generated. */
    operator?: ISimilarityOperator;
    /** Filter options. */
    filter: {
        /** Number of links generated per source resource. If set only the <limit> best links are taken. */
        limit?: number;
        unambiguous?: boolean;
    };
    /** The property URI of the link, e.g. owl:sameAs. */
    linkType: string;
}

export interface ISimilarityOperator extends IOperatorNode {
    /** The weight of this operator when a weighted average of operator scores is calculated, the higher the higher the influence. */
    weight: number;
    /** If the operator is used to generate the index to improve performance of the matching step. */
    indexing?: boolean;
    /** A similarity operator is either a comparison or aggregation. */
    type: "Comparison" | "Aggregation";
    /** The parameter values of the operator. */
    parameters: IOperatorNodeParameters;
}

/** Aggregation operator. Aggregates multiple scores to a single value. */
export interface IAggregationOperator extends ISimilarityOperator {
    /** The ID of the aggregator plugin. */
    aggregator: string;
    /** The inputs to the aggregation. */
    inputs: ISimilarityOperator[];
    type: "Aggregation";
}

/** Comparison operator. Compares 2 resources and outputs a score. */
export interface IComparisonOperator extends ISimilarityOperator {
    type: "Comparison";
    /** The threshold that decides if a distance measure is converted to a positive or negative score. */
    threshold: number;
    /** The comparison plugin ID used for comparing the input values, e.q. 'equality'. */
    metric: string;
    /** The first input for the comparison. Links are generated from the source. */
    sourceInput: IValueInput;
    /** The second input for the comparison. Links are generated to the target. */
    targetInput: IValueInput;
}

/** A link between two resources. */
export interface IResourceLink {
    /** The resource from the first input source. */
    source: string;
    /** The resource from the second input source. */
    target: string;
}

/** Parameters of a linking task. */
export interface ILinkingTaskParameters {
    /** First input source of the linking task. */
    source: IInputSource;
    /** Second input source of the linking task. */
    target: IInputSource;
    /** The linking rule. */
    rule: ILinkingRule;
    /** ID of the output task*/
    output: string;
    /** Positive, negative and unlabeled reference links between resources. Linking rules are evaluated against positive and negative reference links. */
    referenceLinks: {
        positive: IResourceLink;
        negative: IResourceLink;
        unlabeled: IResourceLink;
    };
    /** The max. number of overall links that will be generated. */
    linkLimit: number;
    /** Execution timeout of the matching phase in seconds. */
    matchingExecutionTimeout: number;
}
