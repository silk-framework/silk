import React from "react";
import { Button, Grid, GridColumn, GridRow, SimpleDialog } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { exampleDisplayTitle, ExampleDetailPane, ExampleListPane } from "./ExampleValuesDialog.components";
import { DialogState, default as exampleValuesDialogState } from "./ExampleValuesDialog.state";
import { IRuleBlockInputExample, RuleBlockPort } from "./ruleBlock.types";
import ruleBlockUtils from "./ruleBlock.utils";
import "./ExampleValuesDialog.scss";

/** Props for the example values dialog used to edit reusable rule block examples. */
interface ExampleValuesDialogProps {
    /** Input ports shown in the detail view. */
    ports: RuleBlockPort[];
    /** Persisted examples copied into the dialog draft state. */
    inputExamples: IRuleBlockInputExample[];
    /** Optional port to visually highlight in the detail view. */
    highlightedPortId?: string;
    /** Ephemeral selection of example cases that should be used for evaluation. Empty means evaluate all examples. */
    selectedExampleIdsForEvaluation: string[];
    /** Called when the dialog should close without applying changes. */
    onClose: () => void;
    /** Receives the updated non-persistent evaluation selection when the dialog is closed or applied. */
    onSelectedExampleIdsForEvaluationChange: (selectedExampleIdsForEvaluation: string[]) => void;
    /** Receives the edited examples when the user applies the dialog. */
    onApply: (inputExamples: IRuleBlockInputExample[]) => void;
}

/** Dialog for editing stored example cases of a reusable rule block. */
export const ExampleValuesDialog = ({
    ports,
    inputExamples,
    highlightedPortId,
    selectedExampleIdsForEvaluation,
    onClose,
    onSelectedExampleIdsForEvaluationChange,
    onApply,
}: ExampleValuesDialogProps) => {
    const [t] = useTranslation();
    const sortedPorts = React.useMemo(() => ruleBlockUtils.sortRuleBlockPorts(ports), [ports]);
    const [state, setState] = React.useState<DialogState>(() =>
        exampleValuesDialogState.initializeDialogState(inputExamples, selectedExampleIdsForEvaluation),
    );

    React.useEffect(() => {
        setState(exampleValuesDialogState.initializeDialogState(inputExamples, selectedExampleIdsForEvaluation));
    }, [inputExamples, selectedExampleIdsForEvaluation]);

    const filteredExamples = React.useMemo(() => {
        const normalizedSearchText = state.searchText.trim().toLowerCase();
        if (!normalizedSearchText) {
            return state.draftExamples;
        }
        return state.draftExamples.filter((example, index) => {
            const searchableText = [exampleDisplayTitle(example, index, t), ...Object.values(example.inputs).flat()]
                .join(" ")
                .toLowerCase();
            return searchableText.includes(normalizedSearchText);
        });
    }, [state.draftExamples, state.searchText, t]);

    const currentExample = exampleValuesDialogState.selectedExample(state);
    const currentExampleIndex = exampleValuesDialogState.selectedExampleIndex(state);
    const activeValue =
        currentExample && state.selectedValue
            ? (currentExample.inputs[state.selectedValue.portId] ?? [])[state.selectedValue.valueIndex]
            : undefined;
    const isEditorVisible = state.selectedValue !== undefined;

    const handleCreateNewExample = React.useCallback(() => {
        setState((currentState) =>
            exampleValuesDialogState.createExample(currentState, {
                id: ruleBlockUtils.generateInputExampleId(),
                label: "",
                inputs: {},
            }),
        );
    }, []);

    const handleSelectExample = React.useCallback((exampleId: string) => {
        setState((currentState) => exampleValuesDialogState.selectExample(currentState, exampleId));
    }, []);

    const handleDuplicateExample = React.useCallback((exampleToDuplicate: IRuleBlockInputExample) => {
        setState((currentState) =>
            exampleValuesDialogState.duplicateExample(
                currentState,
                exampleToDuplicate.id,
                ruleBlockUtils.generateInputExampleId(),
            ),
        );
    }, []);

    const handleDeleteExample = React.useCallback((exampleId: string) => {
        setState((currentState) => exampleValuesDialogState.deleteExample(currentState, exampleId));
    }, []);

    const handleDuplicateSelectedExample = React.useCallback(() => {
        if (!currentExample) {
            return;
        }
        handleDuplicateExample(currentExample);
    }, [currentExample, handleDuplicateExample]);

    const handleAddValue = React.useCallback((portId: string) => {
        setState((currentState) => exampleValuesDialogState.addValue(currentState, portId));
    }, []);

    const handleDeleteValue = React.useCallback(
        (portId: string, valueIndex: number) => {
            setState((currentState) => exampleValuesDialogState.deleteValue(currentState, portId, valueIndex));
        },
        [],
    );

    const handleUpdateActiveValue = React.useCallback((nextValue: string) => {
        setState((currentState) => exampleValuesDialogState.updateValue(currentState, nextValue));
    }, []);

    const handleUpdateSelectedExampleLabel = React.useCallback((nextLabel: string) => {
        setState((currentState) => exampleValuesDialogState.updateExampleLabel(currentState, nextLabel));
    }, []);

    const handleSearchTextChange = React.useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
        setState((currentState) => exampleValuesDialogState.setSearchText(currentState, event.target.value));
    }, []);

    const handleClearSearch = React.useCallback(() => {
        setState((currentState) => exampleValuesDialogState.setSearchText(currentState, ""));
    }, []);

    const handleSelectedExampleLabelChange = React.useCallback(
        (event: React.ChangeEvent<HTMLInputElement>) => {
            handleUpdateSelectedExampleLabel(event.target.value);
        },
        [handleUpdateSelectedExampleLabel],
    );

    const handleSelectValue = React.useCallback((portId: string, valueIndex: number) => {
        setState((currentState) => exampleValuesDialogState.selectValue(currentState, portId, valueIndex));
    }, []);

    const handleCloseEditor = React.useCallback(() => {
        setState((currentState) => exampleValuesDialogState.closeEditor(currentState));
    }, []);

    const handleToggleExampleSelectionForEvaluation = React.useCallback((exampleId: string, checked: boolean) => {
        setState((currentState) =>
            exampleValuesDialogState.toggleExampleSelectionForEvaluation(currentState, exampleId, checked),
        );
    }, []);

    const handleClearSelectedExamplesForEvaluation = React.useCallback(() => {
        setState((currentState) => exampleValuesDialogState.clearSelectedExamplesForEvaluation(currentState));
    }, []);

    const editorKey = state.selectedValue
        ? `${currentExample?.id ?? "unknown"}:${state.selectedValue.portId}:${state.selectedValue.valueIndex}`
        : "no-selection";

    const selectedExampleIdsForAvailableExamples = React.useCallback(
        (availableExamples: IRuleBlockInputExample[]) =>
            state.selectedExampleIdsForEvaluation.filter((selectedExampleId) =>
                availableExamples.some((example) => example.id === selectedExampleId),
            ),
        [state.selectedExampleIdsForEvaluation],
    );

    const handleCloseDialog = React.useCallback(() => {
        onSelectedExampleIdsForEvaluationChange(selectedExampleIdsForAvailableExamples(inputExamples));
        onClose();
    }, [inputExamples, onClose, onSelectedExampleIdsForEvaluationChange, selectedExampleIdsForAvailableExamples]);

    const handleApplyAndClose = React.useCallback(() => {
        onSelectedExampleIdsForEvaluationChange(selectedExampleIdsForAvailableExamples(state.draftExamples));
        onApply(ruleBlockUtils.cloneInputExamples(state.draftExamples));
    }, [onApply, onSelectedExampleIdsForEvaluationChange, selectedExampleIdsForAvailableExamples, state.draftExamples]);

    return (
        <SimpleDialog
            isOpen={true}
            size="xlarge"
            title={t("taskViews.ruleBlock.examples.dialog.title", "Example editor")}
            onClose={handleCloseDialog}
            actions={[
                <Button key="apply" affirmative onClick={handleApplyAndClose}>
                    {t("taskViews.ruleBlock.examples.dialog.applyAndClose", "Apply and close")}
                </Button>,
                <Button key="close" onClick={handleCloseDialog}>
                    {t("common.action.close")}
                </Button>,
            ]}
            hasBorder
            showFullScreenToggler
        >
            <div className="ecc-silk-rule-block-example-values-dialog">
                <Grid verticalStretchable={true} useAbsoluteSpace={true}>
                    <GridRow verticalStretched={true}>
                        <GridColumn medium className="ecc-silk-rule-block-example-values-dialog__left-column">
                            <ExampleListPane
                                allExamples={state.draftExamples}
                                filteredExamples={filteredExamples}
                                selectedExampleId={currentExample?.id}
                                searchText={state.searchText}
                                selectedExampleIdsForEvaluation={state.selectedExampleIdsForEvaluation}
                                t={t}
                                onCreateExample={handleCreateNewExample}
                                onSearchTextChange={handleSearchTextChange}
                                onClearSearch={handleClearSearch}
                                onClearSelectedExamplesForEvaluation={handleClearSelectedExamplesForEvaluation}
                                onSelectExample={handleSelectExample}
                                onToggleExampleSelectionForEvaluation={handleToggleExampleSelectionForEvaluation}
                                onDuplicateExample={handleDuplicateExample}
                                onDeleteExample={handleDeleteExample}
                            />
                        </GridColumn>
                        <GridColumn className="ecc-silk-rule-block-example-values-dialog__right-column">
                            <ExampleDetailPane
                                example={currentExample}
                                exampleIndex={currentExampleIndex}
                                ports={sortedPorts}
                                highlightedPortId={highlightedPortId}
                                selectedValue={state.selectedValue}
                                activeValue={activeValue}
                                editorKey={editorKey}
                                isEditorVisible={isEditorVisible}
                                t={t}
                                onDuplicateExample={handleDuplicateSelectedExample}
                                onDeleteExample={handleDeleteExample}
                                onLabelChange={handleSelectedExampleLabelChange}
                                onSelectValue={handleSelectValue}
                                onDeleteValue={handleDeleteValue}
                                onAddValue={handleAddValue}
                                onCloseEditor={handleCloseEditor}
                                onValueChange={handleUpdateActiveValue}
                            />
                        </GridColumn>
                    </GridRow>
                </Grid>
            </div>
        </SimpleDialog>
    );
};

export default ExampleValuesDialog;
