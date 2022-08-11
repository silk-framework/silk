import React from "react";

import {
    AutoSuggestion,
    Button,
    Grid,
    GridColumn,
    GridRow,
    HoverToggler,
    Icon,
    IconButton,
    Notification,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Tag,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import { ComparisonPair, ComparisonPairWithId, TypedPath } from "./LinkingRuleActiveLearning.typings";
import { LinkingRuleActiveLearningContext } from "./contexts/LinkingRuleActiveLearningContext";
import { partialAutoCompleteLinkingInputPaths } from "../LinkingRuleEditor.requests";
import { IPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { checkValuePathValidity } from "../../../pages/MappingEditor/HierarchicalMapping/store";
import { ArrowLeft, ArrowRight, columnStyles, DashedLine } from "./LinkingRuleActiveLearning.shared";
import {
    activeLearningComparisonPairs,
    addActiveLearningComparisonPair,
    removeActiveLearningComparisonPair,
} from "./LinkingRuleActiveLearning.requests";
import { Spinner } from "@blueprintjs/core";

interface LinkingRuleActiveLearningConfigProps {
    projectId: string;
    linkingTaskId: string;
}

let randomId = 0;
const nextId = () => {
    randomId += 1;
    return randomId;
};

/** Allows to configure the property pairs for the active learning. */
export const LinkingRuleActiveLearningConfig = ({ projectId, linkingTaskId }: LinkingRuleActiveLearningConfigProps) => {
    const { registerError } = useErrorHandler();
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const [suggestions, setSuggestions] = React.useState<ComparisonPairWithId[]>([]);
    const [loadSuggestions, setLoadSuggestions] = React.useState(true);
    const manualSourcePath = React.useRef<TypedPath | undefined>(undefined);
    const manualTargetPath = React.useRef<TypedPath | undefined>(undefined);
    const [hasValidPath, setHasValidPath] = React.useState(false);
    const [t] = useTranslation();

    const loadingSuggestions = activeLearningContext.comparisonPairsLoading || loadSuggestions;

    React.useEffect(() => {
        if (!activeLearningContext.comparisonPairsLoading) {
            loadCandidatePairs(projectId, linkingTaskId);
        }
    }, [activeLearningContext.comparisonPairsLoading]);

    const loadCandidatePairs = async (projectId: string, taskId: string) => {
        setLoadSuggestions(true);
        try {
            const comparisonPairs = (await activeLearningComparisonPairs(projectId, taskId)).data;
            const toComparisonPairWithId = (cp: ComparisonPair) => {
                return {
                    ...cp,
                    pairId: `${cp.source.path} ${cp.target.path} ${cp.source.valueType} ${cp.target.valueType}`,
                };
            };
            if (comparisonPairs.selectedPairs.length > 0 && activeLearningContext.propertiesToCompare.length === 0) {
                activeLearningContext.setPropertiesToCompare(
                    comparisonPairs.selectedPairs.map((cp) => toComparisonPairWithId(cp))
                );
            }
            const suggestions = comparisonPairs.suggestedPairs.map((cp) => toComparisonPairWithId(cp));
            setSuggestions(suggestions);
        } catch (ex) {
            // TODO: i18n
            registerError(
                "LinkingRuleActiveLearningConfig.loadCandidatePairs",
                "Could not fetch comparison config",
                ex
            );
        } finally {
            setLoadSuggestions(false);
        }
    };

    const removePair = async (pairId: string) => {
        try {
            const pair = activeLearningContext.propertiesToCompare.find((pair) => pair.pairId === pairId);
            if (pair) {
                await removeActiveLearningComparisonPair(projectId, linkingTaskId, pair);
                activeLearningContext.setPropertiesToCompare(
                    activeLearningContext.propertiesToCompare.filter((pair) => pair.pairId !== pairId)
                );
                // Add again to top of list, so the user can re-add immediately
                setSuggestions([pair, ...suggestions]);
            }
        } catch (err) {
            // TODO
        }
    };

    const fetchAutoCompletionResult =
        (isTarget: boolean) =>
        async (inputString: string, cursorPosition: number): Promise<IPartialAutoCompleteResult | undefined> => {
            try {
                const result = await partialAutoCompleteLinkingInputPaths(
                    projectId,
                    linkingTaskId,
                    isTarget ? "target" : "source",
                    inputString,
                    cursorPosition,
                    200
                );
                return result.data;
            } catch (err) {
                registerError(
                    "ActiveLearning.fetchAutoCompletionResult",
                    t("ActiveLearning.config.errors.fetchAutoCompletionResult"),
                    err
                );
            }
        };

    const changeManualSourcePath = (value: string) => {
        // TODO: How to fetch example values and other meta data?
        manualSourcePath.current = value ? { path: value, valueType: "StringValueType" } : undefined;
        checkPathValidity();
    };

    const changeManualTargetPath = (value: string) => {
        // TODO: How to fetch example values and other meta data?
        manualTargetPath.current = value ? { path: value, valueType: "StringValueType" } : undefined;
        checkPathValidity();
    };

    const checkPathValidity = () => {
        // TODO: Add path validation (syntax) check
        if (manualSourcePath.current && manualTargetPath.current) {
            setHasValidPath(true);
        } else {
            setHasValidPath(false);
        }
    };

    const addManuallyChosenPair = () => {
        if (manualSourcePath.current && manualTargetPath.current) {
            activeLearningContext.setPropertiesToCompare([
                ...activeLearningContext.propertiesToCompare,
                {
                    pairId: `manually chose: ${nextId()}`,
                    source: manualSourcePath.current,
                    target: manualTargetPath.current,
                    // TODO: where to get examples from?
                    sourceExamples: [],
                    targetExamples: [],
                },
            ]);
            changeManualSourcePath("");
            changeManualTargetPath("");
        }
    };

    const addSuggestion = async (pairId: string) => {
        const pairToAdd = suggestions.find((s) => s.pairId === pairId);
        if (pairToAdd) {
            try {
                await addActiveLearningComparisonPair(projectId, linkingTaskId, pairToAdd);
                setSuggestions(suggestions.filter((s) => s.pairId !== pairId));
                activeLearningContext.setPropertiesToCompare([...activeLearningContext.propertiesToCompare, pairToAdd]);
            } catch (error) {
                // TODO
            }
        }
    };
    const ConfigHeader = () => {
        return (
            <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
                <GridColumn style={columnStyles.headerColumnStyle}>Properties of dataset 1</GridColumn>
                <GridColumn style={columnStyles.centerColumnStyle}>
                    <ArrowLeft />
                    <Tag>owl:sameAs</Tag>
                    <ArrowRight />
                </GridColumn>
                <GridColumn style={columnStyles.headerColumnStyle}>Dataset 2</GridColumn>
            </GridRow>
        );
    };

    const SelectedProperty = ({ property, exampleValues }: { property: TypedPath; exampleValues: string[][] }) => {
        const flatExampleValues: string[] = [].concat.apply([], exampleValues);
        const showLabel: boolean = !!property.label && property.label.toLowerCase() !== property.path.toLowerCase();
        const exampleTitle = flatExampleValues.join(" | ");
        return (
            <GridColumn style={columnStyles.mainColumnStyle}>
                <OverviewItem>
                    <OverviewItemDescription>
                        {showLabel ? <OverviewItemLine>{property.label}</OverviewItemLine> : null}
                        <OverviewItemLine small={showLabel}>{property.path}</OverviewItemLine>
                        {flatExampleValues.length > 0 ? (
                            <OverviewItemLine small={showLabel} title={exampleTitle}>
                                {flatExampleValues.map((example) => {
                                    return (
                                        <Tag
                                            small={true}
                                            minimal={true}
                                            round={true}
                                            style={{ marginRight: "0.25rem" }}
                                            htmlTitle={exampleTitle}
                                        >
                                            {example}
                                        </Tag>
                                    );
                                })}
                            </OverviewItemLine>
                        ) : null}
                    </OverviewItemDescription>
                </OverviewItem>
            </GridColumn>
        );
    };

    const SelectedPropertyPair = ({ pair }: { pair: ComparisonPairWithId }) => {
        return (
            <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
                <SelectedProperty property={pair.source} exampleValues={pair.sourceExamples} />
                <GridColumn style={columnStyles.centerColumnStyle}>
                    <HoverToggler
                        baseElement={
                            <Toolbar style={{ height: "100%" }}>
                                <ToolbarSection canGrow={true}>
                                    <ArrowLeft />
                                </ToolbarSection>
                                <ToolbarSection>
                                    <Tag>
                                        {pair.source.valueType != null &&
                                        pair.source.valueType === pair.target.valueType
                                            ? pair.source.valueType
                                            : "string"}
                                    </Tag>
                                </ToolbarSection>
                                <ToolbarSection canGrow={true}>
                                    <ArrowRight />
                                </ToolbarSection>
                            </Toolbar>
                        }
                        hoverElement={
                            <Toolbar style={{ height: "100%" }}>
                                <ToolbarSection canGrow={true} />
                                <ToolbarSection>
                                    <IconButton
                                        name={"item-remove"}
                                        disruptive
                                        onClick={() => removePair(pair.pairId)}
                                    />
                                </ToolbarSection>
                                <ToolbarSection canGrow={true} />
                            </Toolbar>
                        }
                    />
                </GridColumn>
                <SelectedProperty property={pair.target} exampleValues={pair.targetExamples} />
            </GridRow>
        );
    };

    const SelectedPropertiesWidget = () => {
        return (
            <Grid columns={3} fullWidth={true}>
                <ConfigHeader />
                {(activeLearningContext.propertiesToCompare ?? []).map((selected) => (
                    <SelectedPropertyPair pair={selected} />
                ))}
            </Grid>
        );
    };

    const PathAutoCompletion = ({ isTarget }: { isTarget: boolean }) => {
        return (
            <GridColumn style={{ ...columnStyles.mainColumnStyle, textAlign: "left" }}>
                <AutoSuggestion
                    leftElement={
                        <Icon name={"operation-search"} tooltipText={"Allows to construct complex input paths."} />
                    }
                    initialValue={
                        isTarget ? manualTargetPath.current?.path ?? "" : manualSourcePath.current?.path ?? ""
                    }
                    onChange={(value) => {
                        if (isTarget) {
                            changeManualTargetPath(value);
                        } else {
                            changeManualSourcePath(value);
                        }
                    }}
                    fetchSuggestions={fetchAutoCompletionResult(isTarget)}
                    placeholder={"Enter an input path"}
                    checkInput={checkValuePathValidity}
                />
            </GridColumn>
        );
    };

    const ManualPropertyPathSelection = () => {
        return (
            <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
                <PathAutoCompletion isTarget={false} />
                <GridColumn style={columnStyles.centerColumnStyle}>
                    <Toolbar style={{ height: "100%" }}>
                        <ToolbarSection canGrow={true}>
                            <DashedLine />
                        </ToolbarSection>
                        <ToolbarSection>
                            <IconButton
                                name={"item-add-artefact"}
                                disabled={!hasValidPath}
                                title={hasValidPath ? "Add" : "At least one paths is not valid"}
                                onClick={addManuallyChosenPair}
                            />
                        </ToolbarSection>
                        <ToolbarSection canGrow={true}>
                            <DashedLine />
                        </ToolbarSection>
                    </Toolbar>
                </GridColumn>
                <PathAutoCompletion isTarget={true} />
            </GridRow>
        );
    };

    const SuggestionWidget = () => {
        return (
            <Grid columns={3} fullWidth={true}>
                {suggestions.map((suggestion) => (
                    <SuggestedPathSelection pair={suggestion} />
                ))}
            </Grid>
        );
    };

    const SuggestedPathSelection = ({ pair }: { pair: ComparisonPairWithId }) => {
        return (
            <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
                <SelectedProperty property={pair.source} exampleValues={pair.sourceExamples} />
                <GridColumn style={columnStyles.centerColumnStyle}>
                    <Toolbar style={{ height: "100%" }}>
                        <ToolbarSection canGrow={true}>
                            <DashedLine />
                        </ToolbarSection>
                        <ToolbarSection>
                            <IconButton name={"item-add-artefact"} onClick={() => addSuggestion(pair.pairId)} />
                        </ToolbarSection>
                        <ToolbarSection canGrow={true}>
                            <DashedLine />
                        </ToolbarSection>
                    </Toolbar>
                </GridColumn>
                <SelectedProperty property={pair.target} exampleValues={pair.targetExamples} />
            </GridRow>
        );
    };

    const InfoWidget = () => {
        return <Notification message={"Choose properties to compare."} iconName={"item-info"} neutral={true} />;
    };

    // TODO: i18n
    const Title = () => {
        return (
            <Toolbar>
                <ToolbarSection>
                    Configuration: Define properties to compare between entities of each data source.
                </ToolbarSection>
                <ToolbarSection canGrow>
                    <Spacing vertical />
                </ToolbarSection>
                <ToolbarSection>
                    <Button
                        title={"Start learning"}
                        affirmative={true}
                        disabled={activeLearningContext.propertiesToCompare.length === 0}
                        onClick={() => activeLearningContext.navigateTo("linkLearning")}
                    >
                        Start learning
                    </Button>
                </ToolbarSection>
            </Toolbar>
        );
    };

    const SuggestionSelectionSubHeader = () => {
        // TODO: i18n
        const message = loadingSuggestions
            ? "Suggestions loading..."
            : suggestions.length > 0
            ? `Found ${suggestions.length} comparison suggestions you might want to add. Click button to add.`
            : "No suggestions available. You can add further comparison pairs manually.";
        return <Notification neutral={true} message={message} iconName={null} />;
    };

    const PathSelectionSubHeader = () => {
        return <Notification neutral={true} message={"Specify property paths to be compared."} iconName={null} />;
    };

    return (
        <div>
            <Title />
            <Spacing hasDivider={true} />
            <InfoWidget />
            <Spacing size={"small"} />
            <SelectedPropertiesWidget />
            <Spacing />
            <SuggestionSelectionSubHeader />
            <Spacing />
            {loadingSuggestions ? <Spinner /> : suggestions.length > 0 ? <SuggestionWidget /> : null}
            <Spacing />
            <PathSelectionSubHeader />
            <Spacing />
            <ManualPropertyPathSelection />
        </div>
    );
};
