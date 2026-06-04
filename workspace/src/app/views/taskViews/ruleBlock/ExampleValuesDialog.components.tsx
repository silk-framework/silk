import React from "react";
import {
    Button,
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
    /** Translation function used for labels. */
    t: TFunction;
    /** Selects the example when the item is clicked. */
    onSelectExample: (exampleId: string) => void;
    /** Duplicates the example from the item action button. */
    onDuplicateExample: (example: IRuleBlockInputExample) => void;
    /** Deletes the example from the item action button. */
    onDeleteExample: (exampleId: string) => void;
}

const ExampleListItem = ({
    example,
    exampleIndex,
    isSelected,
    t,
    onSelectExample,
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

    return (
        <OverviewItem
            className="ecc-silk-rule-block-example-values-dialog__example-item"
            hasCardWrapper
            cardProps={{
                className: isSelected
                    ? `ecc-silk-rule-block-example-values-dialog__example-item-card ${ClassNames.Intent.ACCENT}`
                    : "ecc-silk-rule-block-example-values-dialog__example-item-card",
            }}
            onClick={() => onSelectExample(example.id)}
        >
            <OverviewItemDescription>
                <OverviewItemLine>
                    <strong>{exampleDisplayTitle(example, exampleIndex, t)}</strong>
                </OverviewItemLine>
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
    /** Translation function used for labels. */
    t: TFunction;
    /** Creates a new example. */
    onCreateExample: () => void;
    /** Updates the search text as the user types. */
    onSearchTextChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    /** Clears the current search text. */
    onClearSearch: () => void;
    /** Selects an example from the list. */
    onSelectExample: (exampleId: string) => void;
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
    t,
    onCreateExample,
    onSearchTextChange,
    onClearSearch,
    onSelectExample,
    onDuplicateExample,
    onDeleteExample,
}: ExampleListPaneProps) => (
    <>
        <Toolbar className="ecc-silk-rule-block-example-values-dialog__header" noWrap>
            <ToolbarSection canGrow>
                <strong>{t("taskViews.ruleBlock.examples.dialog.examples")}</strong>
            </ToolbarSection>
            <ToolbarSection>
                <Button
                    text={t("taskViews.ruleBlock.examples.dialog.newExample")}
                    intent={"accent"}
                    rightIcon={"item-add-artefact"}
                    onClick={onCreateExample}
                    data-test-id={"example-values-new-example"}
                />
            </ToolbarSection>
        </Toolbar>
        <Spacing size="small" />
        <SearchField
            value={searchText}
            onChange={onSearchTextChange}
            onClearanceHandler={onClearSearch}
            emptySearchInputMessage={t("taskViews.ruleBlock.examples.dialog.searchExamples")}
        />
        <Spacing size="small" />
        <div className="ecc-silk-rule-block-example-values-dialog__example-list-scroll">
            <OverviewItemList
                className="ecc-silk-rule-block-example-values-dialog__example-list"
                hasSpacing
                hasDivider
                columns={1}
            >
                {filteredExamples.map((example) => (
                    <ExampleListItem
                        key={example.id}
                        example={example}
                        exampleIndex={allExamples.findIndex((draftExample) => draftExample.id === example.id)}
                        isSelected={example.id === selectedExampleId}
                        t={t}
                        onSelectExample={onSelectExample}
                        onDuplicateExample={onDuplicateExample}
                        onDeleteExample={onDeleteExample}
                    />
                ))}
            </OverviewItemList>
        </div>
    </>
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
                            <Tooltip content={port.label}>
                                <OverflowText>{port.label}</OverflowText>
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
                                                        <span className="ecc-silk-rule-block-example-values-dialog__tooltip-value">
                                                            {value || " "}
                                                        </span>
                                                    }
                                                >
                                                    <Tag
                                                        interactive
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
                                    <Button
                                        onClick={() => onAddValue(port.id)}
                                        data-test-id={`example-values-add-${port.id}`}
                                    >
                                        {t("taskViews.ruleBlock.examples.dialog.addValue")}
                                    </Button>
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
        <>
            <Toolbar className="ecc-silk-rule-block-example-values-dialog__header" noWrap>
                <ToolbarSection canGrow canShrink hideOverflow>
                    <h3 className="ecc-silk-rule-block-example-values-dialog__title">
                        {exampleDisplayTitle(example, exampleIndex, t)}
                    </h3>
                </ToolbarSection>
                <ToolbarSection className="ecc-silk-rule-block-example-values-dialog__header-actions">
                    <Button onClick={onDuplicateExample} rightIcon={"item-clone"}>
                        {t("taskViews.ruleBlock.examples.dialog.duplicateExample")}
                    </Button>
                    <Button disruptive onClick={() => onDeleteExample(example.id)} rightIcon={"item-remove"}>
                        {t("taskViews.ruleBlock.examples.dialog.deleteExample")}
                    </Button>
                </ToolbarSection>
            </Toolbar>
            <Spacing size="small" />
            <div className="ecc-silk-rule-block-example-values-dialog__detail-column">
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
                <Spacing size="small" />
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
                {isEditorVisible ? (
                    <div className="ecc-silk-rule-block-example-values-dialog__editor-section">
                        <Spacing size="small" hasDivider />
                        <FieldSet boxed className="ecc-silk-rule-block-example-values-dialog__editor-pane">
                            <Toolbar className="ecc-silk-rule-block-example-values-dialog__editor-header" noWrap>
                                <ToolbarSection canGrow canShrink hideOverflow>
                                    <strong>{editorTitle}</strong>
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
                            {selectedValue && activeValue != null ? (
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
                            ) : (
                                <div className="ecc-silk-rule-block-example-values-dialog__empty-editor">
                                    {t("taskViews.ruleBlock.examples.dialog.selectValueHint")}
                                </div>
                            )}
                        </FieldSet>
                    </div>
                ) : null}
            </div>
        </>
    );
};
