import React from "react";
import {
    Button,
    Grid,
    GridColumn,
    GridRow,
    IconButton,
    Notification,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Section,
    SectionHeader,
    Spacing,
    Spinner,
    Tag,
    TitleMainsection,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import { ComparisonPair, ComparisonPairWithId, TypedPath } from "./LinkingRuleActiveLearning.typings";
import { LinkingRuleActiveLearningContext } from "./contexts/LinkingRuleActiveLearningContext";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { columnStyles } from "./LinkingRuleActiveLearning.shared";
import {
    activeLearningComparisonPairs,
    addActiveLearningComparisonPair,
    removeActiveLearningComparisonPair,
} from "./LinkingRuleActiveLearning.requests";
import { ManualComparisonPairSelection } from "./config/ManualComparisonPairSelection";
import ConnectionEnabled from "./components/ConnectionEnabled";
import ConnectionAvailable from "./components/ConnectionAvailable";
import { useTranslation } from "react-i18next";
import utils from "./LinkingRuleActiveLearning.utils";

interface LinkingRuleActiveLearningConfigProps {
    projectId: string;
    linkingTaskId: string;
}

/** Allows to configure the property pairs for the active learning. */
export const LinkingRuleActiveLearningConfig = ({ projectId, linkingTaskId }: LinkingRuleActiveLearningConfigProps) => {
    const { registerError } = useErrorHandler();
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const [suggestions, setSuggestions] = React.useState<ComparisonPairWithId[]>([]);
    const [loadSuggestions, setLoadSuggestions] = React.useState(true);
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

    const addComparisonPair = React.useCallback(async (pair: ComparisonPairWithId): Promise<boolean> => {
        try {
            await addActiveLearningComparisonPair(projectId, linkingTaskId, pair);
            activeLearningContext.setPropertiesToCompare([...activeLearningContext.propertiesToCompare, pair]);
            return true;
        } catch (ex) {
            // TODO: i18n
            registerError(
                "LinkingRuleActiveLearningConfig.addComparisonPair",
                "Adding comparison pair has failed.",
                ex
            );
            return false;
        }
    }, []);

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
                <GridColumn style={columnStyles.headerColumnStyle}>Dataset 1</GridColumn>
                <GridColumn style={columnStyles.centerColumnStyle}>
                    <ConnectionEnabled label={"owl:sameAs"} />
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
                        <OverviewItemLine title={showLabel ? property.path : undefined}>
                            {property.label ?? property.path}
                        </OverviewItemLine>
                        {flatExampleValues.length > 0 ? (
                            <OverviewItemLine small={showLabel} title={exampleTitle}>
                                {flatExampleValues.map((example, idx) => {
                                    return (
                                        <Tag
                                            key={example + idx}
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
                    <ConnectionEnabled
                        label={utils.comparisonType(pair)}
                        actions={<IconButton name={"item-remove"} disruptive onClick={() => removePair(pair.pairId)} />}
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
                    <SelectedPropertyPair key={selected.pairId} pair={selected} />
                ))}
            </Grid>
        );
    };

    const SuggestionWidget = () => {
        return (
            <Grid columns={3} fullWidth={true}>
                {suggestions.map((suggestion) => (
                    <SuggestedPathSelection key={suggestion.pairId} pair={suggestion} />
                ))}
            </Grid>
        );
    };

    const SuggestedPathSelection = ({ pair }: { pair: ComparisonPairWithId }) => {
        return (
            <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
                <SelectedProperty property={pair.source} exampleValues={pair.sourceExamples} />
                <GridColumn style={columnStyles.centerColumnStyle}>
                    <ConnectionAvailable
                        actions={<IconButton name={"item-add-artefact"} onClick={() => addSuggestion(pair.pairId)} />}
                    />
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
            <SectionHeader>
                <Toolbar noWrap>
                    <ToolbarSection canShrink>
                        <TitleMainsection>
                            Configuration: Define properties to compare between entities of each data source.
                        </TitleMainsection>
                    </ToolbarSection>
                    <ToolbarSection canGrow>
                        <Spacing vertical />
                    </ToolbarSection>
                    <ToolbarSection>
                        <IconButton
                            name={"item-remove"}
                            disruptive={true}
                            text={t("ActiveLearning.config.buttons.resetTooltip")}
                            onClick={activeLearningContext.showResetDialog}
                        />
                        <Spacing vertical={true} size="small" />
                        <Button
                            title={t("ActiveLearning.config.buttons.startLearning")}
                            affirmative={true}
                            disabled={activeLearningContext.propertiesToCompare.length === 0}
                            onClick={() => activeLearningContext.navigateTo("linkLearning")}
                        >
                            Start learning
                        </Button>
                    </ToolbarSection>
                </Toolbar>
            </SectionHeader>
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
        <Section>
            <Title />
            <Spacing />
            <InfoWidget />
            <Spacing size={"small"} />
            <SelectedPropertiesWidget />
            <Spacing />
            <PathSelectionSubHeader />
            <Spacing />
            <ManualComparisonPairSelection
                projectId={projectId}
                linkingTaskId={linkingTaskId}
                addComparisonPair={addComparisonPair}
            />
            <Spacing />
            <SuggestionSelectionSubHeader />
            <Spacing />
            {loadingSuggestions ? <Spinner /> : suggestions.length > 0 ? <SuggestionWidget /> : null}
        </Section>
    );
};
