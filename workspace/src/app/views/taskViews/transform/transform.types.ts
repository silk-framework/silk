import { IInputSource, IStickyNote } from "../shared/task.typings";
import { IMetadata } from "@ducks/shared/typings";
import { IValueInput, RuleLayout } from "../shared/rules/rule.typings";
import { ILinkingRule, IResourceLink, OptionallyLabelledParameter } from "../linking/linking.types";
import {Stacktrace} from "../../shared/SampleError/SampleError";

/** Parameters of a transform task. */
export interface ITransformRule {
    /** The ID of the transform rule. */
    id: string;
    /** The mapping editor can handle these types of mapping rules. */
    type: "direct" | "complexUri" | "complex" | "uri";
    /** Meta data of the mapping rule, e.g. label and description. */
    metadata: IMetadata;
}

/** A simple 1 to 1 mapping rule. */
export interface IDirectTransformRule extends ITransformRule {
    type: "direct";
    /** The source path expression. Silk path syntax. */
    sourcePath: string;
    mappingTarget: IMappingTarget;
}

export interface IComplexMappingRule extends ITransformRule {
    type: "complex";
    /** All used input paths. Silk path syntax. */
    sourcePaths: string[];
    /** The rule operator tree. */
    operator: IValueInput;
    /** Rule operator layout information. */
    layout: RuleLayout;
    uiAnnotations: {
        stickyNotes: IStickyNote[];
    };
}

export interface IComplexUriRule extends ITransformRule {
    type: "complexUri";
    /**  */
    operator: IValueInput;
}

/** A constant URI rule. */
export interface IUriRule extends ITransformRule {}

/** The target specification of the mapping rule. */
export interface IMappingTarget {
    /** The target property URI of the mapping rule. */
    uri: string;
    /** The data type of the value, e.g. string, int, URI etc. */
    valueType: IValueType;
    /** If this relationship is inverted, i.e. child points to the parent. Only possible if the value is a
     * resource/object and only applicable to datasets that support backward properties like graph-based datasets. */
    isBackwardProperty: boolean;
    /** If true then this should become an attribute. Only applicable to datasets that define attributes like XML. */
    isAttribute: boolean;
}

export interface IValueType {
    /** The type of the target value, e.g. 'StringValueType' */
    nodeType: string;
}

export interface EvaluatedTransformEntity {
    operatorId: string;
    values: string[];
    error: string | null;
    stacktrace?: Stacktrace
    children: EvaluatedTransformEntity[];
}

export interface ITransformTaskParameters {
    /** The mapping rule*/
    mappingRule: any;
    abortIfErrorsOccur: string | boolean;
    /** Error output task ID */
    errorOutput: string;
    /** Selected target vocabularies. All, none or a list of vocabulary URIs. */
    targetVocabularies: "all installed vocabularies" | "no vocabularies" | string;
    /** The input task data. */
    selection: IInputSource;
    /** The output task ID */
    output: string;
}
