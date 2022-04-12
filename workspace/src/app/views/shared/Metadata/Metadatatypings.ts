import { IMetadata } from "@ducks/shared/typings";
import { KeywordProp, KeywordProps } from "@ducks/workspace/typings";

export interface IMetadataExpanded extends IMetadata {
    tags: KeywordProps;
    lastModifiedByUser?: KeywordProp;
    createdByUser?: KeywordProp;
    modified: string;
    created: string;
}
