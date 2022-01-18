import React from "react";

/**
 * The rule editor context that contains objects and methods related to the original objects that are being edited and
 * the operators that can are available.
 *
 * @param ITEM_TYPE The interface of the rule based item that is being edited.
 * @param OPERATOR_TYPE The interface of the operators that can be placed in the editor.
 */
export interface RuleEditorContextProps<ITEM_TYPE, OPERATOR_TYPE> {
    /** The item whose rules are being edited, e.g. linking or transformation. */
    editedItem?: ITEM_TYPE;
    /** The operators that can be dragged and dropped onto the rule editor. */
    operatorList: OPERATOR_TYPE[];
    /** Loading states. */
    editedItemLoading: boolean;
    operatorListLoading: boolean;
}

/** Creates a rule editor model context that contains the actual rule model and low-level update functions. */
export function createRuleEditorContext<ITEM_TYPE, OPERATOR_TYPE>() {
    return React.createContext<RuleEditorContextProps<ITEM_TYPE, OPERATOR_TYPE>>({
        operatorList: [],
        editedItemLoading: false,
        operatorListLoading: false,
    });
}
