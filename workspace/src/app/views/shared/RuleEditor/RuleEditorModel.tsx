import React from "react";
import { Elements } from "react-flow-renderer";
import { RuleEditorModelContext } from "./contexts/RuleEditorModelContext";

export interface RuleEditorModelProps {
    /** The children that work on this rule model. */
    children: JSX.Element | JSX.Element[];
}

/** The actual rule model, i.e. the model that is displayed in the editor. */
export const RuleEditorModel = ({ children }: RuleEditorModelProps) => {
    /** If set, then the model cannot be modified. */
    const [isReadOnly, setIsReadOnly] = React.useState<boolean>(false);
    /** The nodes and edges of the rule editor. */
    const [elements, setElements] = React.useState<Elements>([]);

    return (
        <RuleEditorModelContext.Provider
            value={{
                elements,
                isReadOnly,
                setIsReadOnly,
            }}
        >
            {children}
        </RuleEditorModelContext.Provider>
    );
};
