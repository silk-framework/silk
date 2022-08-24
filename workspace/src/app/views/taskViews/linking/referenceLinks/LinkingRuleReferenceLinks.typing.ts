import { IEntity } from "../../shared/rules/rule.typings";
import { IEntityLink, IEvaluatedReferenceLinksScore } from "../linking.types";

/** The type of a reference link. */
export type LinkType = "positive" | "negative" | "unlabeled";

export interface ReferenceLinksOrdered {
    links: IEntityLink[];
    statistics?: ReferenceLinksStats;
    evaluationScore?: IEvaluatedReferenceLinksScore;
}

/** Stats of reference links. Also includes numbers of yet unsaved changes. */
export interface ReferenceLinksStats {
    existingLinks: number;
    addedLinks: number;
    removedLinks: number;
}

/** An entity pair that are part of the reference links or can be added to them. */
export interface EntityLink {
    /** Internal globally unique ID for managing purposes. */
    entityLinkId: string;
    /** The entity from the first data source. TODO: Think of different type. */
    source: IEntity;
    /** The entity from the second data source. */
    target: IEntity;
    /** The label of the entity link. */
    decision: LinkType;
}

/** The source and target values of a single property pair. */
export interface EntityLinkPropertyPairValues {
    sourceExamples: string[];
    targetExamples: string[];
}

/** The paths whose values should be displayed as entity labels. */
export interface LabelProperties {
    sourceProperties: string[];
    targetProperties: string[];
}
