/** URI patterns found for requested target type URIs. */
export interface IUriPatternsResult {
    results: IUriPattern[]
}

/** An URI pattern for a specific type. */
export interface IUriPattern {
    // The target class URI this URI pattern was applied to
    targetClassUri: string
    // A simplified version of the pattern where the path expressions are reduced to the most relevant parts
    label: string
    // The full URI pattern
    value: string
}