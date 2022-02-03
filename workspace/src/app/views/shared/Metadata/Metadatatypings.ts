import { IMetadata } from "@ducks/shared/typings";

export interface Tag {
    uri: string;
    label: string;
}

export interface IMetadataExpanded extends IMetadata {
    tags?: Array<Tag>;
    lastModifiedByUser?: Tag;
    createdByUser?: Tag;
}
