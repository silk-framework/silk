import React from "react";

interface GlobalMappingEditorContextProps {
    /** A mapping from value type id to label. */
    valueTypeLabels: Map<string, string>
}

export const GlobalMappingEditorContext = React.createContext<GlobalMappingEditorContextProps>({
    valueTypeLabels: new Map()
})
