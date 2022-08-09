import { IEntity } from "../../shared/rules/rule.typings";

/** The type of a reference link. */
export type LinkType = "positive" | "negative" | "unlabeled";

/** A entity pair that are part of the reference links or can be added to them. */
export interface EntityLink {
    /** Internal globally unique ID for managing purposes. */
    entityLinkId: string;
    /** The entity from the first data source. TODO: Think of different type. */
    source: IEntity;
    /** The entity from the second data source. */
    target: IEntity;
    /** The label given to the entity link. */
    label: LinkType;
}

/** The source and target values of a single property pair. */
export interface EntityLinkPropertyPairValues {
    sourceValues: string[];
    targetValues: string[];
}

/** The paths whose values should be displayed as entity labels. */
export interface LabelProperties {
    sourceProperties: string[];
    targetProperties: string[];
}
