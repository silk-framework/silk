import React from "react";
import {
    Button,
    Card,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    Divider,
    HtmlContentBlock,
    IconButton,
    Markdown,
    Notification,
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
    ComparisionDataBody,
    ComparisionDataCell,
    ComparisionDataConnection,
    ComparisionDataContainer,
    ComparisionDataHead,
    ComparisionDataHeader,
    ComparisionDataRow,
} from "./components/ComparisionData";
import { PropertyBox } from "./components/PropertyBox";
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
import { ActiveLearningValueExamples, sameValues } from "./shared/ActiveLearningValueExamples";

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
    const [propertiesToCompare, setPropertiesToCompare] = React.useState<ComparisonPairWithId[]>(activeLearningContext.propertiesToCompare)
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
            if (comparisonPairs.selectedPairs.length > 0 && propertiesToCompare.length === 0) {
                setPropertiesToCompare(
                    comparisonPairs.selectedPairs.map((cp) => toComparisonPairWithId(cp))
                );
            }
            const suggestions = comparisonPairs.suggestedPairs.map((cp) => toComparisonPairWithId(cp));
            setSuggestionsWarnings(comparisonPairs.warnings);
            setSuggestions(suggestions);
        } catch (ex) {
            registerError(
                "LinkingRuleActiveLearningConfig.loadCandidatePairs",
                t("ActiveLearning.config.errors.fetchComparisionConfig"),
                ex
            );
        } finally {
            setLoadSuggestions(false);
        }
    };

    const addComparisonPair = React.useCallback(async (pair: ComparisonPairWithId): Promise<boolean> => {
        try {
            await addActiveLearningComparisonPair(projectId, linkingTaskId, pair);
            setPropertiesToCompare((current) => [...current, pair]);
            return true;
        } catch (ex) {
            registerError(
                "LinkingRuleActiveLearningConfig.addComparisonPair",
                t("ActiveLearning.config.errors.addFailed"),
                ex
            );
            return false;
        }
    }, []);

    const removePair = React.useCallback(async (pairId: string) => {
        try {
            const pair = propertiesToCompare.find((pair) => pair.pairId === pairId);
            if (pair) {
                await removeActiveLearningComparisonPair(projectId, linkingTaskId, pair);
                setPropertiesToCompare( propertiesToCompare =>
                    propertiesToCompare.filter((pair) => pair.pairId !== pairId)
                );
                // Add again to top of list, so the user can re-add immediately
                setSuggestions(suggestions => [pair, ...suggestions]);
            }
        } catch (error) {
            registerError("ActiveLearningFeedback.removePair", "Removing comparison pair has failed.", error)
        }
    }, [propertiesToCompare]);

    const addSuggestion = React.useCallback(async (pairId: string) => {
        const pairToAdd = suggestions.find((s) => s.pairId === pairId);
        if (pairToAdd) {
            try {
                await addActiveLearningComparisonPair(projectId, linkingTaskId, pairToAdd);
                setSuggestions(suggestions => suggestions.filter((s) => s.pairId !== pairId));
                setPropertiesToCompare(propertiesToCompare => [...propertiesToCompare, pairToAdd]);
            } catch (error) {
                registerError("ActiveLearningFeedback.addSuggestion", "Adding suggestion has failed.", error)
            }
        }
    }, [suggestions, propertiesToCompare]);

    const Title = () => {
        return (
            <SectionHeader>
                <Toolbar noWrap>
                    <ToolbarSection canShrink>
                        <TitleMainsection>{t("ActiveLearning.config.title")}</TitleMainsection>
                    </ToolbarSection>
                    <ToolbarSection canGrow>
                        <Spacing vertical />
                    </ToolbarSection>
                    <ToolbarSection>
                        <IconButton
                            name={"item-reset"}
                            disruptive={true}
                            text={t("ActiveLearning.config.buttons.resetTooltip")}
                            onClick={activeLearningContext.showResetDialog}
                        />
                        <Spacing vertical={true} size="small" />
                        <Button
                            affirmative={true}
                            disabled={propertiesToCompare.length === 0}
                            onClick={() => {
                                activeLearningContext.setPropertiesToCompare(propertiesToCompare)
                                activeLearningContext.navigateTo("linkLearning")
                            }}
                        >
                            {activeLearningContext.learningStarted
                                ? t("ActiveLearning.config.buttons.continueLearning")
                                : t("ActiveLearning.config.buttons.startLearning")}
                        </Button>
                    </ToolbarSection>
                </Toolbar>
            </SectionHeader>
        );
    };

    return (
        <Section>
            <Title />
            <Spacing />
            <SelectedPropertiesWidget
                propertiesToCompare={propertiesToCompare}
                removePair={removePair}
            />
            <Spacing />
            <ManualComparisonPairSelection
                projectId={projectId}
                linkingTaskId={linkingTaskId}
                addComparisonPair={addComparisonPair}
            />
            <Spacing />
            <SuggestionWidget
                suggestions={suggestions}
                loadingSuggestions={loadingSuggestions}
                suggestionWarnings={suggestionWarnings}
                addSuggestion={addSuggestion}
            />
        </Section>
    );
};

interface SelectedPropertyPairProps {
    pair: ComparisonPairWithId
    remove: () => any
}

/** A single comparison pair in the selected pair list. */
const SelectedPropertyPair = ({ pair, remove }: SelectedPropertyPairProps) => {
    const [t] = useTranslation()
    const sameExampleValues = sameValues(pair.sourceExamples.flat(), pair.targetExamples.flat());
    return (
        <ComparisionDataRow className="diapp-linking-learningdata__row-body">
            <SelectedProperty
                property={pair.source}
                exampleValues={pair.sourceExamples}
                sameExampleValues={sameExampleValues}
                datasink="source"
            />
            <ComparisionDataConnection>
                <ConnectionEnabled
                    label={utils.comparisonType(pair)}
                    actions={
                        <IconButton
                            text={t("common.action.remove")}
                            name={"item-remove"}
                            disruptive
                            onClick={remove}
                        />
                    }
                />
            </ComparisionDataConnection>
            <SelectedProperty
                property={pair.target}
                exampleValues={pair.targetExamples}
                sameExampleValues={sameExampleValues}
                datasink="target"
            />
        </ComparisionDataRow>
    );
};

interface SelectedPropertiesWidgetProps {
    propertiesToCompare: ComparisonPairWithId[]
    removePair: (pairId: string) => any
}

/** Shows the selected comparison pairs used for active learning. */
const SelectedPropertiesWidget = ({propertiesToCompare, removePair}: SelectedPropertiesWidgetProps) => {
    const [t] = useTranslation()

    return (
        <Card elevation={0}>
            <CardHeader>
                <CardTitle>{t("ActiveLearning.config.entitiyPair.title")}</CardTitle>
            </CardHeader>
            <Divider />
            <CardContent>
                <ComparisionDataContainer>
                    <ComparisonPairTableHeader />
                    {(!propertiesToCompare ||
                        propertiesToCompare.length === 0) && (
                        <>
                            <Spacing size="small" />
                            <InfoWidget />
                        </>
                    )}
                    <ComparisionDataBody>
                        {(propertiesToCompare ?? []).map((selected) => (
                            <SelectedPropertyPair
                                key={selected.pairId}
                                remove={() => removePair(selected.pairId)}
                                pair={selected}
                            />
                        ))}
                    </ComparisionDataBody>
                </ComparisionDataContainer>
            </CardContent>
        </Card>
    );
};

const InfoWidget = () => {
    const [t] = useTranslation()
    return <Notification message={t("ActiveLearning.config.entitiyPair.info")} />;
};

interface SuggestedPathSelectionProps {
    pair: ComparisonPairWithId;
    filterByPath: (path: string, isTarget: boolean) => any;
    isActiveFilterCheck: (path: string, isTarget: boolean) => any;
    addSuggestion: (pairId: string) => any
}

/** A comparison pair of the suggested pairs. */
const SuggestedPathSelection = ({
                                    pair,
                                    filterByPath,
                                    isActiveFilterCheck,
                                    addSuggestion
                                }: SuggestedPathSelectionProps) => {
    const [t] = useTranslation()
    const sameExampleValues = sameValues(pair.sourceExamples.flat(), pair.targetExamples.flat());
    return (
        <ComparisionDataRow className="diapp-linking-learningdata__row-body">
            <SelectedProperty
                property={pair.source}
                exampleValues={pair.sourceExamples}
                sameExampleValues={sameExampleValues}
                filterByPath={() => filterByPath(pair.source.path, false)}
                isActiveFilter={isActiveFilterCheck(pair.source.path, false)}
                datasink="source"
            />
            <ComparisionDataConnection>
                <ConnectionAvailable
                    actions={
                        <IconButton
                            text={t("common.action.add")}
                            name={"item-add-artefact"}
                            onClick={() => addSuggestion(pair.pairId)}
                        />
                    }
                />
            </ComparisionDataConnection>
            <SelectedProperty
                property={pair.target}
                exampleValues={pair.targetExamples}
                sameExampleValues={sameExampleValues}
                filterByPath={() => filterByPath(pair.target.path, true)}
                isActiveFilter={isActiveFilterCheck(pair.target.path, true)}
                datasink="target"
            />
        </ComparisionDataRow>
    );
};

/** A property of a comparison pair. */
const SelectedProperty = ({
                              property,
                              exampleValues,
                              filterByPath,
                              isActiveFilter,
                              datasink,
                          }: {
    property: TypedPath;
    exampleValues: string[];
    sameExampleValues: Set<string>;
    filterByPath?: () => any;
    isActiveFilter?: boolean;
    datasink?: "source" | "target";
}) => {
    const flatExampleValues: string[] = [].concat.apply([], exampleValues);
    const showLabel: boolean = !!property.label && property.label.toLowerCase() !== property.path.toLowerCase();
    const exampleTitle = flatExampleValues.join(" | ");
    return (
        <ComparisionDataCell className={datasink ? `diapp-linking-learningdata__${datasink}` : undefined}>
            <PropertyBox
                propertyName={property.label ?? property.path}
                propertyTooltip={showLabel ? property.path : undefined}
                exampleValues={
                    flatExampleValues.length > 0 ? (
                        <ActiveLearningValueExamples exampleValues={flatExampleValues} />
                    ) : undefined
                }
                exampleTooltip={exampleTitle}
                onFilter={filterByPath}
                filtered={isActiveFilter}
            />
        </ComparisionDataCell>
    );
};

interface SuggestionWidgetProps {
    suggestions: ComparisonPairWithId[]
    loadingSuggestions: boolean
    suggestionWarnings: string[]
    addSuggestion: (suggestionPair: string) => any
}

/** Shows the comparison pair suggestion list. */
const SuggestionWidget = ({suggestions, loadingSuggestions, suggestionWarnings, addSuggestion}: SuggestionWidgetProps) => {
    const [t] = useTranslation()
    const [showInfo, setShowInfo] = React.useState<boolean>(false);
    const pathFilter = React.useRef<{ path: string; isTarget: boolean; label?: string } | undefined>(undefined);
    const [filteredSuggestions, setFilteredSuggestions] = React.useState<ComparisonPairWithId[] | undefined>(
        undefined
    );

    React.useEffect(() => {
        if (!showInfo && suggestions.length === 0) {
            setShowInfo(true);
        }
    }, [suggestions]);

    const filterSuggestions = (path: string, isTarget: boolean) => {
        pathFilter.current = { path, isTarget };
        const filteredSuggestions = suggestions.filter((suggestion) => {
            const suggestionPath = isTarget ? suggestion.target : suggestion.source;
            const samePath = path === suggestionPath.path;
            if (samePath && !pathFilter.current!.label) {
                pathFilter.current!.label = suggestionPath.label;
            }
            return samePath;
        });
        setFilteredSuggestions(filteredSuggestions);
    };

    const resetFilter = () => {
        pathFilter.current = undefined;
        setFilteredSuggestions(undefined);
    };

    const isActiveFilter = (path: string, isTarget: boolean) => {
        const current = pathFilter.current;
        return current && current.path === path && current.isTarget === isTarget;
    };

    const filterByPath = React.useCallback((path: string, isTarget: boolean) => {
        if (isActiveFilter(path, isTarget)) {
            // Reset filter when same path is clicked again
            resetFilter();
        } else {
            filterSuggestions(path, isTarget);
        }
    }, [suggestions]);

    return (
        <Card elevation={0}>
            <CardHeader>
                <CardTitle>
                    {t("ActiveLearning.config.suggestions.title")}
                    {!loadingSuggestions && suggestions.length > 0 && " (" + suggestions.length + ")"}
                </CardTitle>
                <CardOptions>
                    {!loadingSuggestions && suggestionWarnings && (
                        <SuggestionsWarningModal warnings={suggestionWarnings} />
                    )}
                    {!loadingSuggestions && suggestions.length > 0 && (
                        <IconButton
                            name={"item-question"}
                            text={t("ActiveLearning.config.buttons.showInfo")}
                            onClick={() => setShowInfo(!showInfo)}
                        />
                    )}
                </CardOptions>
            </CardHeader>
            <Divider />
            <CardContent>
                {loadingSuggestions ? (
                    <Spinner />
                ) : (
                    <>
                        {showInfo && (
                            <>
                                <Notification
                                    iconName={suggestions.length > 0 ? "item-question" : undefined}
                                    neutral={suggestions.length > 0}
                                    actions={
                                        suggestions.length > 0 ? (
                                            <IconButton
                                                name="navigation-close"
                                                text={t("ActiveLearning.config.buttons.closeInfo")}
                                                onClick={() => setShowInfo(false)}
                                            />
                                        ) : undefined
                                    }
                                >
                                    {suggestions.length > 0
                                        ? t("ActiveLearning.config.suggestions.foundSuggestions", {
                                            count: suggestions.length,
                                        })
                                        : t("ActiveLearning.config.suggestions.emptyList")}
                                </Notification>
                                <Spacing size="small" />
                            </>
                        )}
                        {pathFilter.current ? (
                            <div>
                                <Tag onRemove={resetFilter}>
                                    {pathFilter.current.label ?? pathFilter.current.path}
                                </Tag>
                                <Spacing size="small" />
                            </div>
                        ) : null}
                        {suggestions.length > 0 && (
                            <ComparisionDataContainer>
                                <ComparisonPairTableHeader />
                                <ComparisionDataBody>
                                    {(filteredSuggestions ?? suggestions).map((suggestion) => (
                                        <SuggestedPathSelection
                                            key={suggestion.pairId}
                                            pair={suggestion}
                                            filterByPath={filterByPath}
                                            isActiveFilterCheck={isActiveFilter}
                                            addSuggestion={addSuggestion}
                                        />
                                    ))}
                                </ComparisionDataBody>
                            </ComparisionDataContainer>
                        )}
                    </>
                )}
            </CardContent>
        </Card>
    );
}

/** Shows warnings regarding the suggestions. */
const SuggestionsWarningModal = ({ warnings }: { warnings: string[] }) => {
    const [showWarningsModal, setShowWarningsModal] = React.useState<boolean>(false);
    const [t] = useTranslation()

    if (warnings.length === 0) {
        return <></>;
    }

    const prefix = warnings.length > 1 ? "- " : "";
    const warningsModal = (
        <SimpleDialog
            title={t("ActiveLearning.config.suggestions.warningsTitle")}
            intent="warning"
            isOpen={showWarningsModal}
            actions={<Button text={t("common.action.close")} onClick={() => setShowWarningsModal(false)} />}
        >
            <HtmlContentBlock>
                <Markdown>{warnings.map((w) => `${prefix}${w}`).join("\n")}</Markdown>
            </HtmlContentBlock>
        </SimpleDialog>
    );
    const warningsToggler = (
        <IconButton
            text={t("ActiveLearning.config.suggestions.warningsButton")}
            name={"state-warning"}
            hasStateWarning={true}
            onClick={() => setShowWarningsModal(true)}
        />
    );

    return (
        <>
            {warningsToggler}
            {warningsModal}
        </>
    );
};

/** The header for the selected and suggested comparison pair lists. */
const ComparisonPairTableHeader = () => {
    const [t] = useTranslation()
    return (
        <ComparisionDataHead>
            <ComparisionDataRow>
                <ComparisionDataHeader className="diapp-linking-learningdata__source">
                    {t("ActiveLearning.config.entitiyPair.sourceColumnTitle")}
                </ComparisionDataHeader>
                <ComparisionDataConnection>
                    <ConnectionAvailable actions={<Tag emphasis="weak">owl:sameAs</Tag>} />
                </ComparisionDataConnection>
                <ComparisionDataHeader className="diapp-linking-learningdata__target">
                    {t("ActiveLearning.config.entitiyPair.targetColumnTitle")}
                </ComparisionDataHeader>
            </ComparisionDataRow>
        </ComparisionDataHead>
    );
};
