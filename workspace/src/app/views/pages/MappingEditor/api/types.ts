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

/** Target property auto-completion */
export interface TargetPropertyAutoCompletion {
    /** The URI */
    value: string,
    /** label of the property */
    label?: string,
    /** description of the property */
    description?: string,
    category?: string,
    isCompletion?: boolean,
    extra: {
        /** Object or data type property */
        type: "object" | "value",
        graph?: string
    }
}

export interface GenericInfo {
    uri: string
    label?: string
    description?: string
    altLabels?: string[]
}

export interface TargetClassAutoCompletion {
    /** The URI */
    value: string,
    /** label of the property */
    label?: string,
    /** description of the property */
    description?: string
}

export interface PropertyByDomainAutoCompletion {
    domain: string
    genericInfo: GenericInfo
    propertyType: "ObjectProperty" | "DatatypeProperty"
    range: string
}
