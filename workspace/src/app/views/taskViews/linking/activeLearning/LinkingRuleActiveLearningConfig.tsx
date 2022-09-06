import React from "react";
import {
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    CardOptions,
    Divider,
    HtmlContentBlock,
    IconButton,
    Markdown,
    Notification,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Section,
    SectionHeader,
    SimpleDialog,
    Spacing,
    Spinner,
    Tag,
    TitleMainsection,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import {
    ComparisionDataContainer,
    ComparisionDataHead,
    ComparisionDataBody,
    ComparisionDataRow,
    ComparisionDataHeader,
    ComparisionDataCell,
    ComparisionDataConnection,
} from "./components/ComparisionData";
import { ComparisonPair, ComparisonPairWithId, TypedPath } from "./LinkingRuleActiveLearning.typings";
import { LinkingRuleActiveLearningContext } from "./contexts/LinkingRuleActiveLearningContext";
import useErrorHandler from "../../../../hooks/useErrorHandler";
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
    const [suggestionWarnings, setSuggestionsWarnings] = React.useState<string[]>([]);
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
            setSuggestionsWarnings(comparisonPairs.warnings);
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
            <ComparisionDataHead>
                <ComparisionDataRow>
                    <ComparisionDataHeader>Dataset 1</ComparisionDataHeader>
                    <ComparisionDataConnection>
                        <ConnectionEnabled label={"owl:sameAs"} />
                    </ComparisionDataConnection>
                    <ComparisionDataHeader>Dataset 2</ComparisionDataHeader>
                </ComparisionDataRow>
            </ComparisionDataHead>
        );
    };

    const SelectedProperty = ({ property, exampleValues }: { property: TypedPath; exampleValues: string[][] }) => {
        const flatExampleValues: string[] = [].concat.apply([], exampleValues);
        const showLabel: boolean = !!property.label && property.label.toLowerCase() !== property.path.toLowerCase();
        const exampleTitle = flatExampleValues.join(" | ");
        return (
            <ComparisionDataCell>
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
            </ComparisionDataCell>
        );
    };

    const SelectedPropertyPair = ({ pair }: { pair: ComparisonPairWithId }) => {
        return (
            <ComparisionDataRow>
                <SelectedProperty property={pair.source} exampleValues={pair.sourceExamples} />
                <ComparisionDataConnection>
                    <ConnectionEnabled
                        label={utils.comparisonType(pair)}
                        actions={<IconButton name={"item-remove"} disruptive onClick={() => removePair(pair.pairId)} />}
                    />
                </ComparisionDataConnection>
                <SelectedProperty property={pair.target} exampleValues={pair.targetExamples} />
            </ComparisionDataRow>
        );
    };

    const SelectedPropertiesWidget = () => {
        // TODO: i18n
        return (
            <Card elevation={0}>
                <CardHeader>
                    <CardTitle>Selected properties to compare entities</CardTitle>
                </CardHeader>
                <Divider />
                <CardContent>
                    <ComparisionDataContainer>
                        <ConfigHeader />
                        {(!activeLearningContext.propertiesToCompare || activeLearningContext.propertiesToCompare.length === 0) && (
                            <>
                                <Spacing size="small" />
                                <InfoWidget />
                            </>
                        )}
                        <ComparisionDataBody>
                            {(activeLearningContext.propertiesToCompare ?? []).map((selected) => (
                                <SelectedPropertyPair key={selected.pairId} pair={selected} />
                            ))}
                        </ComparisionDataBody>
                    </ComparisionDataContainer>
                </CardContent>
            </Card>
        );
    };

    const SuggestionWidget = () => {
        // TODO: i18n
        const [showInfo, setShowInfo] = React.useState<boolean>(false);
        React.useEffect(() => {
            if (!showInfo && suggestions.length === 0) {
                setShowInfo(true);
            }
        }, [suggestions]);

        return (
            <Card elevation={0}>
                <CardHeader>
                    <CardTitle>
                        Add suggestions
                        {(!loadSuggestions && suggestions.length > 0) && " ("+suggestions.length+")"}
                    </CardTitle>
                    <CardOptions>
                        {!loadingSuggestions && suggestionWarnings && (
                            <SuggestionsWarningModal warnings={suggestionWarnings} />
                        )}
                        {!loadSuggestions && suggestions.length > 0 && (
                            <IconButton
                                name="item-info"
                                text="Show info"
                                onClick={() => setShowInfo(!showInfo)}
                            />
                        )}
                    </CardOptions>
                </CardHeader>
                <Divider />
                <CardContent>
                    {loadingSuggestions ? <Spinner /> : (
                        <>
                            {showInfo && (
                                <>
                                    <Notification
                                        actions={suggestions.length > 0 ? (
                                            <IconButton
                                                name="navigation-close"
                                                text="Close info"
                                                onClick={() => setShowInfo(false)}
                                            />
                                        ) : undefined}
                                    >
                                        {
                                            suggestions.length > 0 ?
                                            `Found ${suggestions.length} comparison suggestions. You might want to add them in the order you want to see them in the entity comparision later.` :
                                            "No suggestions available. You can add further comparison pairs manually by connection their property paths."
                                        }
                                    </Notification>
                                    <Spacing size="small" />
                                </>
                            )}
                            {suggestions.length > 0 && (
                                <ComparisionDataContainer>
                                    <ComparisionDataBody>
                                        {suggestions.map((suggestion) => (
                                            <SuggestedPathSelection key={suggestion.pairId} pair={suggestion} />
                                        ))}
                                    </ComparisionDataBody>
                                </ComparisionDataContainer>
                            )}
                        </>
                    )}
                </CardContent>
            </Card>
        );
    };

    const SuggestedPathSelection = ({ pair }: { pair: ComparisonPairWithId }) => {
        return (
            <ComparisionDataRow>
                <SelectedProperty property={pair.source} exampleValues={pair.sourceExamples} />
                <ComparisionDataConnection>
                    <ConnectionAvailable
                        actions={<IconButton name={"item-add-artefact"} onClick={() => addSuggestion(pair.pairId)} />}
                    />
                </ComparisionDataConnection>
                <SelectedProperty property={pair.target} exampleValues={pair.targetExamples} />
            </ComparisionDataRow>
        );
    };

    const InfoWidget = () => {
        // TODO: i18n
        return (
            <Notification
                iconName={"item-info"}
                message={
                    "Choose properties to compare. " +
                    "Select from the suggestions or add them by specifying property paths for both entities."
                }
            />
        );
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

    const SuggestionsWarningModal = ({ warnings }: { warnings: string[] }) => {
        const [showWarningsModal, setShowWarningsModal] = React.useState<boolean>(false);

        if (warnings.length === 0) {
            return <></>;
        }

        const prefix = warnings.length > 1 ? "- " : "";
        const warningsModal = (
            <SimpleDialog
                title="Warnings"
                intent="warning"
                isOpen={showWarningsModal}
                actions={<Button text={t("common.action.close")} onClick={() => setShowWarningsModal(false)}/>}
            >
                <HtmlContentBlock>
                    <Markdown>{warnings.map((w) => `${prefix}${w}`).join("\n")}</Markdown>
                </HtmlContentBlock>
            </SimpleDialog>
        );
        const warningsToggler = (
            <IconButton
                text="Show warnings"
                name={"state-warning"}
                hasStateWarning={true}
                onClick={() => setShowWarningsModal(true)}
            />
        );

        return (
            <>
                { warningsToggler }
                { warningsModal }
            </>
        );
    }

    return (
        <Section>
            <Title />
            <Spacing />
            <SelectedPropertiesWidget />
            <Spacing />
            <ManualComparisonPairSelection
                projectId={projectId}
                linkingTaskId={linkingTaskId}
                addComparisonPair={addComparisonPair}
            />
            <Spacing />
            <SuggestionWidget />
        </Section>
    );
};
