import { IRuleBlockInputExample } from "./ruleBlock.types";
import ruleBlockUtils from "./ruleBlock.utils";

/** Identifies the currently edited input value inside the selected example. */
export interface ActiveValueSelection {
    /** Rule block input port that owns the selected value. */
    portId: string;
    /** Zero-based index of the selected value inside the port's value list. */
    valueIndex: number;
}

/** Mutable draft state managed by the example values dialog. */
export interface DialogState {
    /** Editable copy of the persisted examples shown in the dialog. */
    draftExamples: IRuleBlockInputExample[];
    /** Identifier of the example currently shown in the detail pane. */
    selectedExampleId?: string;
    /** Currently opened editor target, if the value editor is visible. */
    selectedValue?: ActiveValueSelection;
    /** Free-text filter applied to the example list. */
    searchText: string;
}

const exampleAfterDeletion = (
    examples: IRuleBlockInputExample[],
    deletedExampleId: string,
): IRuleBlockInputExample | undefined => {
    const deletedIndex = examples.findIndex((example) => example.id === deletedExampleId);
    const remainingExamples = examples.filter((example) => example.id !== deletedExampleId);
    if (remainingExamples.length === 0) {
        return undefined;
    }
    if (deletedIndex < 0) {
        return remainingExamples[0];
    }
    return remainingExamples[Math.min(deletedIndex, remainingExamples.length - 1)];
};

/** Builds the initial dialog state from the persisted examples received via props. */
const initializeDialogState = (inputExamples: IRuleBlockInputExample[]): DialogState => {
    const draftExamples = ruleBlockUtils.cloneInputExamples(inputExamples);
    return {
        draftExamples,
        selectedExampleId: draftExamples[0]?.id,
        selectedValue: undefined,
        searchText: "",
    };
};

const selectedExample = (state: DialogState): IRuleBlockInputExample | undefined =>
    state.draftExamples.find((example) => example.id === state.selectedExampleId) ?? state.draftExamples[0];

const selectedExampleIndex = (state: DialogState): number =>
    Math.max(
        0,
        state.draftExamples.findIndex((example) => example.id === state.selectedExampleId),
    );

const setSearchText = (state: DialogState, value: string): DialogState => ({
    ...state,
    searchText: value,
});

const selectExample = (state: DialogState, exampleId: string): DialogState => ({
    ...state,
    selectedExampleId: exampleId,
    selectedValue: undefined,
});

const createExample = (state: DialogState, example: IRuleBlockInputExample): DialogState => ({
    ...state,
    draftExamples: [...state.draftExamples, example],
    selectedExampleId: example.id,
    selectedValue: undefined,
});

/** Inserts a deep copy of the source example directly after the original and selects the duplicate. */
const duplicateExample = (state: DialogState, exampleId: string, duplicateId: string): DialogState => {
    const sourceExampleIndex = state.draftExamples.findIndex((example) => example.id === exampleId);
    const sourceExample = state.draftExamples[sourceExampleIndex];
    if (!sourceExample) {
        return state;
    }
    const duplicate: IRuleBlockInputExample = {
        id: duplicateId,
        label: sourceExample.label ?? "",
        inputs: Object.fromEntries(
            Object.entries(sourceExample.inputs).map(([portId, values]) => [portId, [...values]] as const),
        ),
    };
    return {
        ...state,
        draftExamples: [
            ...state.draftExamples.slice(0, sourceExampleIndex + 1),
            duplicate,
            ...state.draftExamples.slice(sourceExampleIndex + 1),
        ],
        selectedExampleId: duplicate.id,
        selectedValue: undefined,
    };
};

/** Removes an example and keeps the detail pane focused on the nearest remaining example, if any. */
const deleteExample = (state: DialogState, exampleId: string): DialogState => {
    const nextSelectedExample = exampleAfterDeletion(state.draftExamples, exampleId);
    return {
        ...state,
        draftExamples: state.draftExamples.filter((example) => example.id !== exampleId),
        selectedExampleId: nextSelectedExample?.id,
        selectedValue: undefined,
    };
};

const updateExampleLabel = (state: DialogState, value: string): DialogState => {
    const currentExample = selectedExample(state);
    if (!currentExample) {
        return state;
    }
    return {
        ...state,
        draftExamples: state.draftExamples.map((example) =>
            example.id === currentExample.id
                ? {
                      ...example,
                      label: value,
                  }
                : example,
        ),
    };
};

const selectValue = (state: DialogState, portId: string, valueIndex: number): DialogState => ({
    ...state,
    selectedValue: {
        portId,
        valueIndex,
    },
});

const closeEditor = (state: DialogState): DialogState => ({
    ...state,
    selectedValue: undefined,
});

/** Appends an empty value to the selected example and opens the editor for the new value. */
const addValue = (state: DialogState, portId: string): DialogState => {
    const currentExample = selectedExample(state);
    if (!currentExample) {
        return state;
    }
    const nextValueIndex = (currentExample.inputs[portId] ?? []).length;
    return {
        ...state,
        draftExamples: state.draftExamples.map((example) =>
            example.id === currentExample.id
                ? {
                      ...example,
                      inputs: {
                          ...example.inputs,
                          [portId]: [...(example.inputs[portId] ?? []), ""],
                      },
                  }
                : example,
        ),
        selectedValue: {
            portId,
            valueIndex: nextValueIndex,
        },
    };
};

const updateValue = (state: DialogState, value: string): DialogState => {
    const currentExample = selectedExample(state);
    const currentSelection = state.selectedValue;
    if (!currentExample || !currentSelection) {
        return state;
    }
    return {
        ...state,
        draftExamples: state.draftExamples.map((example) =>
            example.id === currentExample.id
                ? {
                      ...example,
                      inputs: {
                          ...example.inputs,
                          [currentSelection.portId]: (example.inputs[currentSelection.portId] ?? []).map(
                              (currentValue, index) => (index === currentSelection.valueIndex ? value : currentValue),
                          ),
                      },
                  }
                : example,
        ),
    };
};

/**
 * Removes a value from the selected example. Deleting the currently edited value closes the editor instead of
 * automatically switching to a different value.
 */
const deleteValue = (state: DialogState, portId: string, valueIndex: number): DialogState => {
    const currentExample = selectedExample(state);
    if (!currentExample) {
        return state;
    }
    const currentSelection = state.selectedValue;
    const nextDraftExamples = state.draftExamples.map((example) => {
        if (example.id !== currentExample.id) {
            return example;
        }
        const nextValues = (example.inputs[portId] ?? []).filter((_, index) => index !== valueIndex);
        const nextInputs = { ...example.inputs };
        if (nextValues.length > 0) {
            nextInputs[portId] = nextValues;
        } else {
            delete nextInputs[portId];
        }
        return {
            ...example,
            inputs: nextInputs,
        };
    });
    let nextSelectedValue = currentSelection;
    if (currentSelection?.portId === portId) {
        if (currentSelection.valueIndex === valueIndex) {
            nextSelectedValue = undefined;
        } else if (valueIndex < currentSelection.valueIndex) {
            nextSelectedValue = {
                portId: currentSelection.portId,
                valueIndex: currentSelection.valueIndex - 1,
            };
        }
    }
    return {
        ...state,
        draftExamples: nextDraftExamples,
        selectedValue: nextSelectedValue,
    };
};

const exampleValuesDialogState = {
    initializeDialogState,
    selectedExample,
    selectedExampleIndex,
    setSearchText,
    selectExample,
    createExample,
    duplicateExample,
    deleteExample,
    updateExampleLabel,
    selectValue,
    closeEditor,
    addValue,
    updateValue,
    deleteValue,
};

export default exampleValuesDialogState;
