import { IMetadata } from "@ducks/shared/typings";
import { Keyword, Keywords } from "@ducks/workspace/typings";

export interface IMetadataExpanded extends IMetadata {
    tags: Keywords;
    lastModifiedByUser?: Keyword;
    createdByUser?: Keyword;
    modified?: string;
    created?: string;
}
