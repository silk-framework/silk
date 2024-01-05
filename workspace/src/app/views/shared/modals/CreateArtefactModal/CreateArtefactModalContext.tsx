import React from "react";

export interface CreateArtefactModalContextProps {
    /** Displays an (unexpected) error in the modal error widget. */
    registerModalError?: (errorId: string, errorMessage: string, error: any) => any;
}

export const CreateArtefactModalContext = React.createContext<CreateArtefactModalContextProps>({});
