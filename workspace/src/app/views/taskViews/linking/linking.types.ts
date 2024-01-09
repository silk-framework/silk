import { IInputSource, IUiAnnotations } from "../shared/task.typings";
import { IEntity, IOperatorNode, IOperatorNodeParameters, IValueInput, RuleLayout } from "../shared/rules/rule.typings";
import {SampleError, Stacktrace} from "../../shared/SampleError/SampleError";

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
    /** The property URI of the link, e.g. owl:sameAs, that will connect the source entity to the target entity. */
    linkType: string;
    /** The optional inverse property URI of the link that would connect the target entity to the source entity. */
    inverseLinkType?: string | null;
    /** If enabled, links will not be generated from a resource to itself. */
    excludeSelfReferences: boolean;
    /** Layout information of the link rule operators. */
    layout: RuleLayout;
    /** Visual annotations to be displayed in the editor. */
    uiAnnotations: IUiAnnotations;
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

export type OptionallyLabelledParameter<ACTUAL_TYPE> = ACTUAL_TYPE | LabelledParameterValue<ACTUAL_TYPE>;
export type LabelledParameterValue<ACTUAL_TYPE> = { value: ACTUAL_TYPE; label?: string };

/** Get the value of an optionally labelled parameter value. */
export function optionallyLabelledParameterToValue<T>(optionallyLabelledValue: OptionallyLabelledParameter<T>): T {
    if (optionallyLabelledValue == null) {
        return optionallyLabelledValue;
    }
    return (optionallyLabelledValue as LabelledParameterValue<T>).value !== undefined
        ? (optionallyLabelledValue as LabelledParameterValue<T>).value
        : (optionallyLabelledValue as T);
}

export function optionallyLabelledParameterToLabel(
    optionallyLabelledValue: OptionallyLabelledParameter<any>
): string | undefined {
    if (optionallyLabelledValue != null) {
        return (optionallyLabelledValue as LabelledParameterValue<any>).label
            ? (optionallyLabelledValue as LabelledParameterValue<any>).label
            : undefined;
    }
}

/** Parameters of a linking task.
 * Either all or zero parameters have the value/label structure.
 */
export interface ILinkingTaskParameters {
    /** First input source of the linking task. */
    source: OptionallyLabelledParameter<IInputSource>;
    /** Second input source of the linking task. */
    target: OptionallyLabelledParameter<IInputSource>;
    /** The linking rule. */
    rule: OptionallyLabelledParameter<ILinkingRule>;
    /** ID of the output task*/
    output: OptionallyLabelledParameter<string>;
    /** Positive, negative and unlabeled reference links between resources. Linking rules are evaluated against positive and negative reference links. */
    referenceLinks: OptionallyLabelledParameter<{
        positive: IResourceLink;
        negative: IResourceLink;
        unlabeled: IResourceLink;
    }>;
    /** The max. number of overall links that will be generated. */
    linkLimit: OptionallyLabelledParameter<number>;
    /** Execution timeout of the matching phase in seconds. */
    matchingExecutionTimeout: OptionallyLabelledParameter<number>;
}

/** Link evaluation */

/** Reference links of a linking rule. */
export interface ReferenceLinks {
    negative: IEntityLink[];
    positive: IEntityLink[];
}

/** The result when evaluating a linkage rule against the reference links. */
export interface IEvaluatedReferenceLinks extends ReferenceLinks {
    evaluationScore: IEvaluatedReferenceLinksScore;
}

/** The metrics of a rule evaluation over the reference elements. */
export interface IEvaluatedReferenceLinksScore {
    /** F1 measure */
    fMeasure: string;
    precision: string;
    recall: string;
    falseNegatives: number;
    falsePositives: number;
    trueNegatives: number;
    truePositives: number;
}

export interface IEntityLink {
    /** The source entity URI. */
    source: string;
    /** The target entity URI. */
    target: string;
    /** The confidence regarding to the linkage rule. Range: -1.0 to 1.0 */
    confidence?: number;
    /** The label of the entity link. */
    decision: "positive" | "negative" | "unlabeled" | undefined;
    /** The rule evaluation tree. For empty rules it is just a score. */
    ruleValues?: IEvaluationNode | { score: number };
    entities?: {
        source: IEntity;
        target: IEntity;
    };
}

/** The evaluation scores and values in the aggregator/comparator tree. */
export interface IEvaluationNode {
    /** The ID of the operator. */
    operatorId: string;
    /** Optional score. This is only set on aggregation or comparison nodes, but there it is still optional. */
    score?: number;
}

/** Evaluation result of an aggregation operator. */
export interface AggregationConfidence extends IEvaluationNode {
    /** The (score producing) children of the aggregation operator. */
    children: IEvaluationNode[];
}

/** Evaluation result of a comparison operator. */
export interface ComparisonConfidence extends IEvaluationNode {
    /** Source input value tree. */
    sourceValue: IEvaluationValue;
    /** Target input value tree. */
    targetValue: IEvaluationValue;
}

/** Values produced by a specific value generating operator, i.e. path input or transform operator. */
export interface IEvaluationValue extends IEvaluationNode {
    /** The generated values of this operator. */
    values: string[];
    /** Optional validation error. */
    error?: string;
    /** In case of transform operators, they can have value inputs. */
    children?: IEvaluationValue[];
    /** Optional stacktrace if error is set. */
    stacktrace?: Stacktrace
}
