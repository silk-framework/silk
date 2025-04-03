import React from "react";
import { TaskContext } from "../../../shared/projectTaskTabView/projectTaskTabView.typing";

interface GlobalMappingEditorContextProps {
    /** The ID of the project. */
    projectId: string;
    /** The ID of the transform task. */
    transformTaskId: string;
    /** A mapping from value type id to label. */
    valueTypeLabels: Map<string, string>;
    /** Optional transform task context. */
    taskContext?: TaskContext;
}

/** Global properties of a specific mapping editor instance. */
export const GlobalMappingEditorContext = React.createContext<GlobalMappingEditorContextProps>({
    valueTypeLabels: new Map(),
    projectId: "",
    transformTaskId: "",
});
