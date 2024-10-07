import React from "react";

interface ProjectTaskTabViewContextProps {
    fullScreen: boolean
}

export const ProjectTaskTabViewContext = React.createContext<ProjectTaskTabViewContextProps>({
    fullScreen: false
})
