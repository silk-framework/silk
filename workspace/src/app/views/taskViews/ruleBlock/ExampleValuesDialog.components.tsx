import React from "react";
import {
    Button,
    Checkbox,
    ClassNames,
    CodeEditor,
    FieldItem,
    FieldSet,
    IconButton,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemList,
    OverflowText,
    PropertyName,
    PropertyValue,
    PropertyValueList,
    PropertyValuePair,
    SearchField,
    Spacing,
    Tag,
    TagList,
    TextField,
    Toolbar,
    ToolbarSection,
    Tooltip,
    ApplicationViewability,
    TitleSubsection,
    FlexibleLayoutContainer,
    FlexibleLayoutItem,
    Label,
} from "@eccenca/gui-elements";
import { TFunction } from "react-i18next";
import { ActiveValueSelection } from "./ExampleValuesDialog.state";
import { IRuleBlockInputExample, RuleBlockPort } from "./ruleBlock.types";

const EDITOR_HEIGHT = 220;
const EDITOR_HEADER_HEIGHT = 56;

export const exampleTitle = (index: number, t: TFunction): string =>
    t("taskViews.ruleBlock.examples.dialog.exampleTitle", {
        defaultValue: "Example {{index}}",
        index: index + 1,
    });

export const exampleDisplayTitle = (example: IRuleBlockInputExample, index: number, t: TFunction): string => {
    const trimmedLabel = example.label?.trim();
    return trimmedLabel ? trimmedLabel : exampleTitle(index, t);
};

const inputCount = (example: IRuleBlockInputExample): number =>
    Object.values(example.inputs ?? {}).filter((values) => values.length > 0).length;

const valuePreview = (value: string): string => {
    const normalizedValue = value.replace(/\s+/g, " ").trim();
    if (!normalizedValue) {
        return "…";
    }
    return normalizedValue.length > 40 ? `${normalizedValue.slice(0, 37)}…` : normalizedValue;
};

/** Props for a single example entry shown in the example list. */
interface ExampleListItemProps {
    /** Example rendered by the list item. */
    example: IRuleBlockInputExample;
    /** Zero-based position of the example in the full draft list. */
    exampleIndex: number;
    /** Whether the example is currently selected in the dialog. */
    isSelected: boolean;
    /** Whether the example is selected for evaluation. */
    isSelectedForEvaluation: boolean;
    /** Translation function used for labels. */
    t: TFunction;
    /** Selects the example when the item is clicked. */
    onSelectExample: (exampleId: string) => void;
    /** Toggles whether the example is included in evaluation. */
    onToggleExampleSelectionForEvaluation: (exampleId: string, checked: boolean) => void;
    /** Duplicates the example from the item action button. */
    onDuplicateExample: (example: IRuleBlockInputExample) => void;
    /** Deletes the example from the item action button. */
    onDeleteExample: (exampleId: string) => void;
}

const ExampleListItem = ({
    example,
    exampleIndex,
    isSelected,
    isSelectedForEvaluation,
    t,
    onSelectExample,
    onToggleExampleSelectionForEvaluation,
    onDuplicateExample,
    onDeleteExample,
}: ExampleListItemProps) => {
    const handleDuplicateClick = React.useCallback(
        (event: React.MouseEvent) => {
            event.stopPropagation();
            onDuplicateExample(example);
        },
        [example, onDuplicateExample],
    );

    const handleDeleteClick = React.useCallback(
        (event: React.MouseEvent) => {
            event.stopPropagation();
            onDeleteExample(example.id);
        },
        [example.id, onDeleteExample],
    );

    const handleSelectionChange = React.useCallback(
        (event: React.ChangeEvent<HTMLInputElement>) => {
            onToggleExampleSelectionForEvaluation(example.id, event.target.checked);
        },
        [example.id, onToggleExampleSelectionForEvaluation],
    );

    return (
        <OverviewItem
            className={`ecc-silk-rule-block-example-values-dialog__example-item ${isSelected ? ClassNames.Intent.ACCENT : ""}`}
            onClick={() => onSelectExample(example.id)}
            hasSpacing
        >
            <OverviewItemActions
                className="ecc-silk-rule-block-example-values-dialog__example-selection"
                onClick={(event) => event.stopPropagation()}
            >
                <Checkbox
                    checked={isSelectedForEvaluation}
                    onChange={handleSelectionChange}
                    labelElement={
                        <ApplicationViewability hide={"screen"}>
                            <span>
                                {t("common.action.select")}: {exampleDisplayTitle(example, exampleIndex, t)}
                            </span>
                        </ApplicationViewability>
                    }
                    style={{ marginBottom: 0 }}
                />
            </OverviewItemActions>
            <OverviewItemDescription>
                <OverviewItemLine>{exampleDisplayTitle(example, exampleIndex, t)}</OverviewItemLine>
                <OverviewItemLine small>
                    {t("taskViews.ruleBlock.examples.dialog.inputsCount", {
                        defaultValue: "{{count}} inputs",
                        count: inputCount(example),
                    })}
                </OverviewItemLine>
            </OverviewItemDescription>
            <OverviewItemActions>
                <IconButton
                    name="item-clone"
                    text={t("taskViews.ruleBlock.examples.dialog.duplicateExample")}
                    onClick={handleDuplicateClick}
                />
                <IconButton
                    name="item-remove"
                    text={t("taskViews.ruleBlock.examples.dialog.deleteExample")}
                    intent="danger"
                    onClick={handleDeleteClick}
                />
            </OverviewItemActions>
        </OverviewItem>
    );
};

/** Props for the list pane containing search and the example overview list. */
interface ExampleListPaneProps {
    /** Full editable example list used to resolve stable display indices. */
    allExamples: IRuleBlockInputExample[];
    /** Filtered example list currently visible in the pane. */
    filteredExamples: IRuleBlockInputExample[];
    /** Currently selected example identifier. */
    selectedExampleId?: string;
    /** Current search field content. */
    searchText: string;
    /** Example IDs selected for evaluation. Empty means evaluate all examples. */
    selectedExampleIdsForEvaluation: string[];
    /** Translation function used for labels. */
    t: TFunction;
    /** Creates a new example. */
    onCreateExample: () => void;
    /** Updates the search text as the user types. */
    onSearchTextChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    /** Clears the current search text. */
    onClearSearch: () => void;
    /** Clears the current evaluation selection. */
    onClearSelectedExamplesForEvaluation: () => void;
    /** Selects an example from the list. */
    onSelectExample: (exampleId: string) => void;
    /** Toggles whether an example is included in evaluation. */
    onToggleExampleSelectionForEvaluation: (exampleId: string, checked: boolean) => void;
    /** Duplicates an example from the list. */
    onDuplicateExample: (example: IRuleBlockInputExample) => void;
    /** Deletes an example from the list. */
    onDeleteExample: (exampleId: string) => void;
}

export const ExampleListPane = ({
    allExamples,
    filteredExamples,
    selectedExampleId,
    searchText,
    selectedExampleIdsForEvaluation,
    t,
    onCreateExample,
    onSearchTextChange,
    onClearSearch,
    onClearSelectedExamplesForEvaluation,
    onSelectExample,
    onToggleExampleSelectionForEvaluation,
    onDuplicateExample,
    onDeleteExample,
}: ExampleListPaneProps) => (
    <FlexibleLayoutContainer
        vertical
        noEqualItemSpace
        useAbsoluteSpace
        gapSize={"small"}
        style={{
            padding: "calc(0.5 * var(--eccgui-size-block-whitespace))",
            paddingLeft: "var(--eccgui-size-block-whitespace)",
            borderRight: "1px solid var(--eccgui-color-palette-layout-grey-300)",
        }}
    >
        <FlexibleLayoutItem growFactor={0} shrinkFactor={0}>
            <Toolbar className="ecc-silk-rule-block-example-values-dialog__header" noWrap>
                <ToolbarSection canGrow>
                    <TitleSubsection>{t("taskViews.ruleBlock.examples.dialog.examples")}</TitleSubsection>
                </ToolbarSection>
                <ToolbarSection>
                    <Button
                        text={t("taskViews.ruleBlock.examples.dialog.newExample")}
                        intent={"accent"}
                        variant={"outlined"}
                        rightIcon={"item-add-artefact"}
                        onClick={onCreateExample}
                        data-test-id={"example-values-new-example"}
                    />
                </ToolbarSection>
            </Toolbar>
        </FlexibleLayoutItem>
        <FlexibleLayoutItem growFactor={0} shrinkFactor={0}>
            <SearchField
                value={searchText}
                onChange={onSearchTextChange}
                onClearanceHandler={onClearSearch}
                emptySearchInputMessage={t("taskViews.ruleBlock.examples.dialog.searchExamples")}
            />
        </FlexibleLayoutItem>
        <FlexibleLayoutItem growFactor={0} shrinkFactor={0}>
            {selectedExampleIdsForEvaluation.length > 0 ? (
                <TagList>
                    <Tag
                        onRemove={onClearSelectedExamplesForEvaluation}
                        data-test-id={"example-values-clear-selection"}
                    >
                        {t("taskViews.ruleBlock.examples.dialog.selectionInfo", {
                            defaultValue: "{{count}} selected for evaluation",
                            count: selectedExampleIdsForEvaluation.length,
                        })}
                    </Tag>
                </TagList>
            ) : null}
        </FlexibleLayoutItem>
        <FlexibleLayoutItem style={{ overflow: "auto" }}>
            <div className="ecc-silk-rule-block-example-values-dialog__example-list-scroll">
                <OverviewItemList
                    className="ecc-silk-rule-block-example-values-dialog__example-list"
                    hasDivider
                    columns={1}
                >
                    {filteredExamples.map((example) => (
                        <ExampleListItem
                            key={example.id}
                            example={example}
                            exampleIndex={allExamples.findIndex((draftExample) => draftExample.id === example.id)}
                            isSelected={example.id === selectedExampleId}
                            isSelectedForEvaluation={selectedExampleIdsForEvaluation.includes(example.id)}
                            t={t}
                            onSelectExample={onSelectExample}
                            onToggleExampleSelectionForEvaluation={onToggleExampleSelectionForEvaluation}
                            onDuplicateExample={onDuplicateExample}
                            onDeleteExample={onDeleteExample}
                        />
                    ))}
                </OverviewItemList>
            </div>
        </FlexibleLayoutItem>
    </FlexibleLayoutContainer>
);

/** Props for the port/value list shown inside the example detail pane. */
interface ExamplePortValuesListProps {
    /** Sorted ports rendered as rows. */
    ports: RuleBlockPort[];
    /** Example currently shown in the detail pane. */
    example: IRuleBlockInputExample;
    /** Optional highlighted port identifier. */
    highlightedPortId?: string;
    /** Current active value selection, if any. */
    selectedValue?: ActiveValueSelection;
    /** Translation function used for labels. */
    t: TFunction;
    /** Selects a value chip for editing. */
    onSelectValue: (portId: string, valueIndex: number) => void;
    /** Removes a value chip. */
    onDeleteValue: (portId: string, valueIndex: number) => void;
    /** Appends a new value for a port. */
    onAddValue: (portId: string) => void;
}

export const ExamplePortValuesList = ({
    ports,
    example,
    highlightedPortId,
    selectedValue,
    t,
    onSelectValue,
    onDeleteValue,
    onAddValue,
}: ExamplePortValuesListProps) => (
    <div className="ecc-silk-rule-block-example-values-dialog__port-list-scroll">
        <PropertyValueList className="ecc-silk-rule-block-example-values-dialog__port-list">
            {ports.map((port) => {
                const values = example.inputs[port.id] ?? [];
                return (
                    <PropertyValuePair
                        key={port.id}
                        hasDivider
                        hasSpacing
                        className={
                            port.id === highlightedPortId
                                ? "ecc-silk-rule-block-example-values-dialog__port-pair ecc-silk-rule-block-example-values-dialog__port-pair--highlighted"
                                : "ecc-silk-rule-block-example-values-dialog__port-pair"
                        }
                        data-test-id={`example-values-row-${port.id}`}
                    >
                        <PropertyName
                            size="medium"
                            className={
                                port.id === highlightedPortId
                                    ? "ecc-silk-rule-block-example-values-dialog__port-name ecc-silk-rule-block-example-values-dialog__port-name--highlighted"
                                    : "ecc-silk-rule-block-example-values-dialog__port-name"
                            }
                        >
                            <Tooltip content={port.label} targetProps={{ style: { maxWidth: "100%" } }}>
                                <Label
                                    text={<OverflowText inline>{port.label}</OverflowText>}
                                    isLayoutForElement="span"
                                    emphasis={"strong"}
                                />
                            </Tooltip>
                        </PropertyName>
                        <PropertyValue>
                            <Toolbar className="ecc-silk-rule-block-example-values-dialog__port-value-content" noWrap>
                                <ToolbarSection
                                    canGrow
                                    canShrink
                                    className="ecc-silk-rule-block-example-values-dialog__port-tags"
                                >
                                    <TagList>
                                        {values.map((value, valueIndex) => {
                                            const isActive =
                                                selectedValue?.portId === port.id &&
                                                selectedValue.valueIndex === valueIndex;
                                            return (
                                                <Tooltip
                                                    key={`${port.id}-${valueIndex}`}
                                                    content={
                                                        <div
                                                            className="ecc-silk-rule-block-example-values-dialog__tooltip-value"
                                                            style={{ whiteSpace: "pre-wrap" }}
                                                        >
                                                            {value || " "}
                                                        </div>
                                                    }
                                                >
                                                    <Tag
                                                        intent={isActive ? "accent" : undefined}
                                                        onClick={() => onSelectValue(port.id, valueIndex)}
                                                        onRemove={(event) => {
                                                            event.stopPropagation();
                                                            onDeleteValue(port.id, valueIndex);
                                                        }}
                                                        minimal={!isActive}
                                                    >
                                                        {valuePreview(value)}
                                                    </Tag>
                                                </Tooltip>
                                            );
                                        })}
                                    </TagList>
                                </ToolbarSection>
                                <ToolbarSection className="ecc-silk-rule-block-example-values-dialog__port-action">
                                    <IconButton
                                        onClick={() => onAddValue(port.id)}
                                        data-test-id={`example-values-add-${port.id}`}
                                        name="item-add-artefact"
                                        variant={"outlined"}
                                        text={t("taskViews.ruleBlock.examples.dialog.addValue")}
                                    />
                                </ToolbarSection>
                            </Toolbar>
                        </PropertyValue>
                    </PropertyValuePair>
                );
            })}
        </PropertyValueList>
    </div>
);

/** Props for the right-hand example detail pane. */
interface ExampleDetailPaneProps {
    /** Example currently shown in the detail pane. */
    example?: IRuleBlockInputExample;
    /** Zero-based index of the current example in the full draft list. */
    exampleIndex: number;
    /** Sorted ports rendered in the port/value list and editor header. */
    ports: RuleBlockPort[];
    /** Optional highlighted port identifier. */
    highlightedPortId?: string;
    /** Current active value selection, if any. */
    selectedValue?: ActiveValueSelection;
    /** Current value content shown in the editor. */
    activeValue?: string;
    /** Stable editor key used to reset the code editor when the selection changes. */
    editorKey: string;
    /** Whether the editor section should be visible. */
    isEditorVisible: boolean;
    /** Translation function used for labels. */
    t: TFunction;
    /** Duplicates the current example. */
    onDuplicateExample: () => void;
    /** Deletes the current example. */
    onDeleteExample: (exampleId: string) => void;
    /** Updates the current example label. */
    onLabelChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    /** Selects a value chip for editing. */
    onSelectValue: (portId: string, valueIndex: number) => void;
    /** Removes a value chip. */
    onDeleteValue: (portId: string, valueIndex: number) => void;
    /** Appends a new value for a port. */
    onAddValue: (portId: string) => void;
    /** Closes the editor section. */
    onCloseEditor: () => void;
    /** Updates the active value content. */
    onValueChange: (nextValue: string) => void;
}

export const ExampleDetailPane = ({
    example,
    exampleIndex,
    ports,
    highlightedPortId,
    selectedValue,
    activeValue,
    editorKey,
    isEditorVisible,
    t,
    onDuplicateExample,
    onDeleteExample,
    onLabelChange,
    onSelectValue,
    onDeleteValue,
    onAddValue,
    onCloseEditor,
    onValueChange,
}: ExampleDetailPaneProps) => {
    if (!example) {
        return (
            <div className="ecc-silk-rule-block-example-values-dialog__empty-state">
                {t("taskViews.ruleBlock.examples.dialog.noExamples")}
            </div>
        );
    }

    const editorTitle =
        selectedValue && activeValue != null
            ? t("taskViews.ruleBlock.examples.dialog.editingValue", {
                  defaultValue: "Editing {{portLabel}}",
                  portLabel: ports.find((port) => port.id === selectedValue.portId)?.label ?? selectedValue.portId,
              })
            : t("taskViews.ruleBlock.examples.dialog.selectValueHint");

    return (
        <FlexibleLayoutContainer
            vertical
            useAbsoluteSpace
            noEqualItemSpace
            gapSize={"small"}
            className="ecc-silk-rule-block-example-values-dialog__detail-column"
            style={{ padding: "calc(0.5 * var(--eccgui-size-block-whitespace)) var(--eccgui-size-block-whitespace)" }}
        >
            <FlexibleLayoutItem growFactor={0} shrinkFactor={0}>
                <Toolbar className="ecc-silk-rule-block-example-values-dialog__header" noWrap>
                    <ToolbarSection canGrow canShrink hideOverflow>
                        <ApplicationViewability hide={"screen"}>
                            <TitleSubsection className="ecc-silk-rule-block-example-values-dialog__title">
                                {exampleDisplayTitle(example, exampleIndex, t)}
                            </TitleSubsection>
                        </ApplicationViewability>
                    </ToolbarSection>
                    <ToolbarSection className="ecc-silk-rule-block-example-values-dialog__header-actions">
                        <Button onClick={onDuplicateExample} rightIcon={"item-clone"} variant={"outlined"}>
                            {t("taskViews.ruleBlock.examples.dialog.duplicateExample")}
                        </Button>
                        <Spacing size={"small"} vertical />
                        <Button
                            disruptive
                            onClick={() => onDeleteExample(example.id)}
                            rightIcon={"item-remove"}
                            variant={"outlined"}
                        >
                            {t("taskViews.ruleBlock.examples.dialog.deleteExample")}
                        </Button>
                    </ToolbarSection>
                </Toolbar>
            </FlexibleLayoutItem>
            <FlexibleLayoutItem growFactor={0} shrinkFactor={0}>
                <FieldItem
                    labelProps={{ text: t("taskViews.ruleBlock.examples.dialog.label", "Label") }}
                    helperText={t(
                        "taskViews.ruleBlock.examples.dialog.labelHint",
                        "Optional. If empty, the generated example title is shown.",
                    )}
                >
                    <TextField
                        value={example.label ?? ""}
                        placeholder={exampleTitle(exampleIndex, t)}
                        onChange={onLabelChange}
                        data-test-id="example-values-label"
                    />
                </FieldItem>
            </FlexibleLayoutItem>
            <Spacing size="small" hasDivider />
            <FlexibleLayoutItem style={{ overflow: "auto" }}>
                <ExamplePortValuesList
                    ports={ports}
                    example={example}
                    highlightedPortId={highlightedPortId}
                    selectedValue={selectedValue}
                    t={t}
                    onSelectValue={onSelectValue}
                    onDeleteValue={onDeleteValue}
                    onAddValue={onAddValue}
                />
            </FlexibleLayoutItem>
            {isEditorVisible ? <Spacing size="small" hasDivider /> : null}
            {isEditorVisible ? (
                <FlexibleLayoutItem growFactor={0} shrinkFactor={0}>
                    <div className="ecc-silk-rule-block-example-values-dialog__editor-section">
                        <FieldSet
                            boxed
                            className="ecc-silk-rule-block-example-values-dialog__editor-pane"
                            title={
                                <Toolbar className="ecc-silk-rule-block-example-values-dialog__editor-header" noWrap>
                                    <ToolbarSection canGrow canShrink hideOverflow>
                                        <OverflowText inline>{editorTitle}</OverflowText>
                                    </ToolbarSection>
                                    <ToolbarSection>
                                        <IconButton
                                            name="navigation-close"
                                            text={t("common.action.close")}
                                            onClick={onCloseEditor}
                                            data-test-id="example-values-editor-close"
                                        />
                                    </ToolbarSection>
                                </Toolbar>
                            }
                        >
                            {selectedValue && activeValue != null ? (
                                <FieldItem labelProps={{ text: t("taskViews.ruleBlock.examples.dialog.editValue") }}>
                                    <CodeEditor
                                        key={editorKey}
                                        id="rule-block-example-value-editor"
                                        name="rule-block-example-value-editor"
                                        mode="markdown"
                                        defaultValue={activeValue}
                                        onChange={onValueChange}
                                        height={EDITOR_HEIGHT - EDITOR_HEADER_HEIGHT}
                                        data-test-id="example-values-editor"
                                    />
                                </FieldItem>
                            ) : (
                                <div className="ecc-silk-rule-block-example-values-dialog__empty-editor">
                                    {t("taskViews.ruleBlock.examples.dialog.selectValueHint")}
                                </div>
                            )}
                        </FieldSet>
                    </div>
                </FlexibleLayoutItem>
            ) : null}
        </FlexibleLayoutContainer>
    );
};
