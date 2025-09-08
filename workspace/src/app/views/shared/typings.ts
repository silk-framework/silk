export type ProjectTaskParams = {
    projectId: string;
    taskId: string;
};

/** Characteristics of a dataset task. */
export interface DatasetCharacteristics {
    /** The path operators and characteristics the dataset supports. */
    supportedPathExpressions: SupportedPathExpressions;
    /** If true, the dataset supports reading and writing multiple tables, which includes hierarchical datasets (XML, JSON, etc.).
     * If false, the dataset only supports a single table (e.g., CSV). */
    supportsMultipleTables: boolean;
}

interface SupportedPathExpressions {
    /** Support for paths with multiple chained forward and/or backward operators. */
    multiHopPaths: boolean;
    /** Support for backward paths, e.g. pointing to the parent in a hierarchical file format like XML/JSON. */
    backwardPaths: boolean;
    /** Support for language filters for data formats that allow to annotate values with language tags, e.g. RDF. */
    languageFilter: boolean;
    /** Support for property filters that restrict the number of values the expression returns. */
    propertyFilter: boolean;
    /** Special paths that the dataset supports, e.g. #idx to get the order index of an entity. */
    specialPaths: SpecialPathInfo[];
}

interface SpecialPathInfo {
    value: string;
    description?: string;
    suggestedFor: "All" | "ValuePathOnly" | "ObjectPathOnly";
}
