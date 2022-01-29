export interface Tag {
    uri: string;
    label: string;
}

export interface MetadataExpandedResponse {
    label: string;
    description: string;
    modified: string;
    created: string;
    createdByUser: string;
    lastModifiedByUser: string;
    tags: Array<Tag>;
}
