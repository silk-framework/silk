import React from "react";
import {
    Button,
    ClassNames,
    CodeEditor,
    FieldItem,
    FieldSet,
    Grid,
    GridColumn,
    GridRow,
    IconButton,
    OverviewItem,
    OverflowText,
    PropertyName,
    PropertyValue,
    PropertyValueList,
    PropertyValuePair,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemList,
    SearchField,
    SimpleDialog,
    Spacing,
    Tag,
    TagList,
    TextField,
    Toolbar,
    ToolbarSection,
    Tooltip,
} from "@eccenca/gui-elements";
import { TFunction, useTranslation } from "react-i18next";
import { IRuleBlockInputExample, IRuleBlockPort } from "./ruleBlock.types";
import ruleBlockUtils from "./ruleBlock.utils";
import "./ExampleValuesDialog.scss";

interface ExampleValuesDialogProps {
    ports: IRuleBlockPort[];
    inputExamples: IRuleBlockInputExample[];
    highlightedPortId?: string;
    onClose: () => void;
    onApply: (inputExamples: IRuleBlockInputExample[]) => void;
}

interface ActiveValueSelection {
    exampleId: string;
    portId: string;
    valueIndex: number;
}

const EDITOR_HEIGHT = 220;
const EDITOR_HEADER_HEIGHT = 56;

const exampleTitle = (index: number, t: TFunction): string =>
    t("taskViews.ruleBlock.examples.dialog.exampleTitle", {
        defaultValue: "Example {{index}}",
        index: index + 1,
    });

const exampleDisplayTitle = (example: IRuleBlockInputExample, index: number, t: TFunction): string => {
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

const firstActiveValue = (
    example: IRuleBlockInputExample | undefined,
    ports: IRuleBlockPort[],
): ActiveValueSelection | undefined => {
    if (!example) {
        return undefined;
    }
    for (const port of ports) {
        const values = example.inputs[port.id] ?? [];
        if (values.length > 0) {
            return {
                exampleId: example.id,
                portId: port.id,
                valueIndex: 0,
            };
        }
    }
    return undefined;
};

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

/** Dialog for editing stored example cases of a reusable rule block. */
export const ExampleValuesDialog = ({
    ports,
    inputExamples,
    highlightedPortId,
    onClose,
    onApply,
}: ExampleValuesDialogProps) => {
    const [t] = useTranslation();
    const [searchText, setSearchText] = React.useState("");
    const [draftExamples, setDraftExamples] = React.useState<IRuleBlockInputExample[]>([]);
    const [selectedExampleId, setSelectedExampleId] = React.useState<string | undefined>(undefined);
    const [activeValueSelection, setActiveValueSelection] = React.useState<ActiveValueSelection | undefined>(undefined);
    const [isEditorVisible, setIsEditorVisible] = React.useState(false);

    const sortedPorts = React.useMemo(() => ruleBlockUtils.sortRuleBlockPorts(ports), [ports]);

    React.useEffect(() => {
        const nextExamples = ruleBlockUtils.cloneInputExamples(inputExamples);
        setDraftExamples(nextExamples);
        setSearchText("");
        setSelectedExampleId(nextExamples[0]?.id);
        setActiveValueSelection(undefined);
        setIsEditorVisible(false);
    }, [inputExamples]);

    const filteredExamples = React.useMemo(() => {
        const normalizedSearchText = searchText.trim().toLowerCase();
        if (!normalizedSearchText) {
            return draftExamples;
        }
        return draftExamples.filter((example, index) => {
            const searchableText = [
                exampleDisplayTitle(example, index, t),
                ...Object.values(example.inputs).flat(),
            ]
                .join(" ")
                .toLowerCase();
            return searchableText.includes(normalizedSearchText);
        });
    }, [draftExamples, searchText, t]);

    const selectedExample = draftExamples.find((example) => example.id === selectedExampleId) ?? draftExamples[0];

    React.useEffect(() => {
        if (!selectedExample) {
            if (draftExamples.length === 0 && inputExamples.length > 0) {
                return;
            }
            setSelectedExampleId(undefined);
            setActiveValueSelection(undefined);
            setIsEditorVisible(false);
            return;
        }
        if (selectedExample.id !== selectedExampleId) {
            setSelectedExampleId(selectedExample.id);
        }
        if (
            !activeValueSelection ||
            activeValueSelection.exampleId !== selectedExample.id ||
            activeValueSelection.valueIndex >= (selectedExample.inputs[activeValueSelection.portId] ?? []).length
        ) {
            const nextActiveValue = firstActiveValue(selectedExample, sortedPorts);
            if (isEditorVisible) {
                setActiveValueSelection(nextActiveValue);
                if (!nextActiveValue) {
                    setIsEditorVisible(false);
                }
            } else if (activeValueSelection) {
                setActiveValueSelection(undefined);
            }
        }
    }, [activeValueSelection, draftExamples.length, inputExamples.length, isEditorVisible, selectedExample, selectedExampleId, sortedPorts]);

    const replaceSelectedExample = React.useCallback(
        (updater: (example: IRuleBlockInputExample) => IRuleBlockInputExample): IRuleBlockInputExample | undefined => {
            let updatedExample: IRuleBlockInputExample | undefined;
            setDraftExamples((currentExamples) =>
                currentExamples.map((example) => {
                    if (example.id !== selectedExample?.id) {
                        return example;
                    }
                    updatedExample = updater(example);
                    return updatedExample;
                }),
            );
            return updatedExample;
        },
        [selectedExample],
    );

    const createNewExample = React.useCallback(() => {
        const nextExample: IRuleBlockInputExample = {
            id: ruleBlockUtils.generateInputExampleId(),
            label: "",
            inputs: {},
        };
        setDraftExamples((currentExamples) => [...currentExamples, nextExample]);
        setSelectedExampleId(nextExample.id);
        setActiveValueSelection(undefined);
        setIsEditorVisible(false);
    }, []);

    const clearActiveValueSelection = React.useCallback(() => {
        setActiveValueSelection(undefined);
        setIsEditorVisible(false);
    }, []);

    const selectExample = React.useCallback((exampleId: string) => {
        setSelectedExampleId(exampleId);
        clearActiveValueSelection();
    }, [clearActiveValueSelection]);

    const duplicateExample = React.useCallback((exampleToDuplicate: IRuleBlockInputExample) => {
        const duplicatedExample: IRuleBlockInputExample = {
            id: ruleBlockUtils.generateInputExampleId(),
            label: exampleToDuplicate.label ?? "",
            inputs: Object.fromEntries(
                Object.entries(exampleToDuplicate.inputs).map(([portId, values]) => [portId, [...values]] as const),
            ),
        };
        setDraftExamples((currentExamples) => {
            const selectedExampleIndex = currentExamples.findIndex((example) => example.id === exampleToDuplicate.id);
            if (selectedExampleIndex < 0) {
                return [...currentExamples, duplicatedExample];
            }
            return [
                ...currentExamples.slice(0, selectedExampleIndex + 1),
                duplicatedExample,
                ...currentExamples.slice(selectedExampleIndex + 1),
            ];
        });
        setSelectedExampleId(duplicatedExample.id);
        clearActiveValueSelection();
    }, [clearActiveValueSelection]);

    const duplicateSelectedExample = React.useCallback(() => {
        if (!selectedExample) {
            return;
        }
        duplicateExample(selectedExample);
    }, [duplicateExample, selectedExample]);

    const deleteExample = React.useCallback(
        (exampleId: string) => {
            const fallbackExample = exampleAfterDeletion(draftExamples, exampleId);
            setDraftExamples((currentExamples) => currentExamples.filter((example) => example.id !== exampleId));
            setSelectedExampleId(fallbackExample?.id);
            setActiveValueSelection(undefined);
            setIsEditorVisible(false);
        },
        [draftExamples],
    );

    const addValue = React.useCallback(
        (portId: string) => {
            if (!selectedExample) {
                return;
            }
            let nextValueIndex = 0;
            replaceSelectedExample((example) => {
                const currentValues = example.inputs[portId] ?? [];
                nextValueIndex = currentValues.length;
                return {
                    ...example,
                    inputs: {
                        ...example.inputs,
                        [portId]: [...currentValues, ""],
                    },
                };
            });
            setActiveValueSelection({
                exampleId: selectedExample.id,
                portId,
                valueIndex: nextValueIndex,
            });
            setIsEditorVisible(true);
        },
        [replaceSelectedExample, selectedExample],
    );

    const deleteValue = React.useCallback(
        (portId: string, valueIndex: number) => {
            if (!selectedExample) {
                return;
            }
            let nextValuesLength = 0;
            let updatedExample: IRuleBlockInputExample | undefined;
            replaceSelectedExample((example) => {
                const currentValues = example.inputs[portId] ?? [];
                const nextValues = currentValues.filter((_, currentIndex) => currentIndex !== valueIndex);
                nextValuesLength = nextValues.length;
                const nextInputs = { ...example.inputs };
                if (nextValues.length > 0) {
                    nextInputs[portId] = nextValues;
                } else {
                    delete nextInputs[portId];
                }
                updatedExample = {
                    ...example,
                    inputs: nextInputs,
                };
                return updatedExample;
            });
            if (
                activeValueSelection?.exampleId === selectedExample.id &&
                activeValueSelection.portId === portId
            ) {
                if (nextValuesLength === 0) {
                    const nextActiveValue = firstActiveValue(updatedExample, sortedPorts);
                    setActiveValueSelection(nextActiveValue);
                    if (!nextActiveValue) {
                        setIsEditorVisible(false);
                    }
                } else {
                    setActiveValueSelection({
                        exampleId: selectedExample.id,
                        portId,
                        valueIndex: Math.max(0, Math.min(valueIndex, nextValuesLength - 1)),
                    });
                }
            }
        },
        [activeValueSelection, replaceSelectedExample, selectedExample, sortedPorts],
    );

    const updateActiveValue = React.useCallback(
        (nextValue: string) => {
            if (!selectedExample || !activeValueSelection || activeValueSelection.exampleId !== selectedExample.id) {
                return;
            }
            replaceSelectedExample((example) => {
                const currentValues = [...(example.inputs[activeValueSelection.portId] ?? [])];
                currentValues[activeValueSelection.valueIndex] = nextValue;
                return {
                    ...example,
                    inputs: {
                        ...example.inputs,
                        [activeValueSelection.portId]: currentValues,
                    },
                };
            });
        },
        [activeValueSelection, replaceSelectedExample, selectedExample],
    );

    const updateSelectedExampleLabel = React.useCallback(
        (nextLabel: string) => {
            if (!selectedExample) {
                return;
            }
            replaceSelectedExample((example) => ({
                ...example,
                label: nextLabel,
            }));
        },
        [replaceSelectedExample, selectedExample],
    );

    const activeValue =
        selectedExample && activeValueSelection?.exampleId === selectedExample.id
            ? (selectedExample.inputs[activeValueSelection.portId] ?? [])[activeValueSelection.valueIndex]
            : undefined;

    const editorKey = activeValueSelection
        ? `${activeValueSelection.exampleId}:${activeValueSelection.portId}:${activeValueSelection.valueIndex}`
        : "no-selection";

    const handleApplyAndClose = React.useCallback(() => {
        onApply(ruleBlockUtils.cloneInputExamples(draftExamples));
    }, [draftExamples, onApply]);

    const handleSearchTextChange = React.useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
        setSearchText(event.target.value);
    }, []);

    const handleSelectedExampleLabelChange = React.useCallback(
        (event: React.ChangeEvent<HTMLInputElement>) => {
            updateSelectedExampleLabel(event.target.value);
        },
        [updateSelectedExampleLabel],
    );

    const handleExampleDuplicateClick = React.useCallback(
        (event: React.MouseEvent, example: IRuleBlockInputExample) => {
            event.stopPropagation();
            duplicateExample(example);
        },
        [duplicateExample],
    );

    const handleExampleDeleteClick = React.useCallback(
        (event: React.MouseEvent, exampleId: string) => {
            event.stopPropagation();
            deleteExample(exampleId);
        },
        [deleteExample],
    );

    const handleSelectValue = React.useCallback(
        (portId: string, valueIndex: number) => {
            if (!selectedExample) {
                return;
            }
            setIsEditorVisible(true);
            setActiveValueSelection({
                exampleId: selectedExample.id,
                portId,
                valueIndex,
            });
        },
        [selectedExample],
    );

    const handleCloseEditor = React.useCallback(() => {
        clearActiveValueSelection();
    }, [clearActiveValueSelection]);

    return (
        <SimpleDialog
            isOpen={true}
            size="xlarge"
            title={t("taskViews.ruleBlock.examples.dialog.title", "Example editor")}
            onClose={onClose}
            actions={[
                <Button key="apply" affirmative onClick={handleApplyAndClose}>
                    {t("taskViews.ruleBlock.examples.dialog.applyAndClose", "Apply and close")}
                </Button>,
                <Button key="close" onClick={onClose}>
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
                            <Toolbar className="ecc-silk-rule-block-example-values-dialog__header" noWrap>
                                <ToolbarSection canGrow>
                                    <strong>{t("taskViews.ruleBlock.examples.dialog.examples")}</strong>
                                </ToolbarSection>
                                <ToolbarSection>
                                    <Button
                                        text={t("taskViews.ruleBlock.examples.dialog.newExample")}
                                        intent={"accent"}
                                        rightIcon={"item-add-artefact"}
                                        onClick={createNewExample}
                                        data-test-id={"example-values-new-example"}
                                    />
                                </ToolbarSection>
                            </Toolbar>
                            <Spacing size="small" />
                            <SearchField
                                value={searchText}
                                onChange={handleSearchTextChange}
                                onClearanceHandler={() => setSearchText("")}
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
                                    {filteredExamples.map((example) => {
                                        const exampleIndex = draftExamples.findIndex((draftExample) => draftExample.id === example.id);
                                        return (
                                            <OverviewItem
                                                key={example.id}
                                                className="ecc-silk-rule-block-example-values-dialog__example-item"
                                                hasCardWrapper
                                                cardProps={{
                                                    className: example.id === selectedExample?.id
                                                        ? `ecc-silk-rule-block-example-values-dialog__example-item-card ${ClassNames.Intent.ACCENT}`
                                                        : "ecc-silk-rule-block-example-values-dialog__example-item-card",
                                                }}
                                                onClick={() => selectExample(example.id)}
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
                                                        onClick={(event: React.MouseEvent) =>
                                                            handleExampleDuplicateClick(event, example)
                                                        }
                                                    />
                                                    <IconButton
                                                        name="item-remove"
                                                        text={t("taskViews.ruleBlock.examples.dialog.deleteExample")}
                                                        intent="danger"
                                                        onClick={(event: React.MouseEvent) =>
                                                            handleExampleDeleteClick(event, example.id)
                                                        }
                                                    />
                                                </OverviewItemActions>
                                            </OverviewItem>
                                        );
                                    })}
                                </OverviewItemList>
                            </div>
                        </GridColumn>
                        <GridColumn className="ecc-silk-rule-block-example-values-dialog__right-column">
                            {selectedExample ? (
                                <>
                                    <Toolbar className="ecc-silk-rule-block-example-values-dialog__header" noWrap>
                                        <ToolbarSection canGrow canShrink hideOverflow>
                                            <h3 className="ecc-silk-rule-block-example-values-dialog__title">
                                                {exampleDisplayTitle(
                                                    selectedExample,
                                                    draftExamples.findIndex((example) => example.id === selectedExample.id),
                                                    t,
                                                )}
                                            </h3>
                                        </ToolbarSection>
                                        <ToolbarSection className="ecc-silk-rule-block-example-values-dialog__header-actions">
                                            <Button onClick={duplicateSelectedExample} rightIcon={"item-clone"}>
                                                {t("taskViews.ruleBlock.examples.dialog.duplicateExample")}
                                            </Button>
                                            <Button disruptive onClick={() => deleteExample(selectedExample.id)} rightIcon={"item-remove"}>
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
                                                value={selectedExample.label ?? ""}
                                                placeholder={exampleTitle(
                                                    draftExamples.findIndex((example) => example.id === selectedExample.id),
                                                    t,
                                                )}
                                                onChange={handleSelectedExampleLabelChange}
                                                data-test-id="example-values-label"
                                            />
                                        </FieldItem>
                                        <Spacing size="small" />
                                        <div className="ecc-silk-rule-block-example-values-dialog__port-list-scroll">
                                            <PropertyValueList className="ecc-silk-rule-block-example-values-dialog__port-list">
                                                {sortedPorts.map((port) => {
                                                    const values = selectedExample.inputs[port.id] ?? [];
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
                                                                    <OverflowText>
                                                                        {port.label}
                                                                    </OverflowText>
                                                                </Tooltip>
                                                            </PropertyName>
                                                            <PropertyValue>
                                                                <Toolbar
                                                                    className="ecc-silk-rule-block-example-values-dialog__port-value-content"
                                                                    noWrap
                                                                >
                                                                    <ToolbarSection
                                                                        canGrow
                                                                        canShrink
                                                                        className="ecc-silk-rule-block-example-values-dialog__port-tags"
                                                                    >
                                                                        <TagList>
                                                                            {values.map((value, valueIndex) => {
                                                                                const isActive =
                                                                                    activeValueSelection?.exampleId === selectedExample.id &&
                                                                                    activeValueSelection.portId === port.id &&
                                                                                    activeValueSelection.valueIndex === valueIndex;
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
                                                                                            onClick={() => handleSelectValue(port.id, valueIndex)}
                                                                                            onRemove={() => deleteValue(port.id, valueIndex)}
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
                                                                            onClick={() => addValue(port.id)}
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
                                        {isEditorVisible ? (
                                            <div className="ecc-silk-rule-block-example-values-dialog__editor-section">
                                                <Spacing size="small" hasDivider />
                                                <FieldSet boxed className="ecc-silk-rule-block-example-values-dialog__editor-pane">
                                                    <Toolbar className="ecc-silk-rule-block-example-values-dialog__editor-header" noWrap>
                                                        <ToolbarSection canGrow canShrink hideOverflow>
                                                            <strong>
                                                                {activeValueSelection && activeValue != null
                                                                    ? t("taskViews.ruleBlock.examples.dialog.editingValue", {
                                                                        defaultValue: "Editing {{portLabel}}",
                                                                        portLabel:
                                                                            sortedPorts.find((port) => port.id === activeValueSelection.portId)
                                                                                ?.label ?? activeValueSelection.portId,
                                                                    })
                                                                    : t("taskViews.ruleBlock.examples.dialog.selectValueHint")}
                                                            </strong>
                                                        </ToolbarSection>
                                                        <ToolbarSection>
                                                            <IconButton
                                                                name="navigation-close"
                                                                text={t("common.action.close")}
                                                                onClick={handleCloseEditor}
                                                                data-test-id="example-values-editor-close"
                                                            />
                                                        </ToolbarSection>
                                                    </Toolbar>
                                                    {activeValueSelection && activeValue != null ? (
                                                        <CodeEditor
                                                            key={editorKey}
                                                            id="rule-block-example-value-editor"
                                                            name="rule-block-example-value-editor"
                                                            mode="markdown"
                                                            defaultValue={activeValue}
                                                            onChange={updateActiveValue}
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
                            ) : (
                                <div className="ecc-silk-rule-block-example-values-dialog__empty-state">
                                    {t("taskViews.ruleBlock.examples.dialog.noExamples")}
                                </div>
                            )}
                        </GridColumn>
                    </GridRow>
                </Grid>
            </div>
        </SimpleDialog>
    );
};

export default ExampleValuesDialog;
