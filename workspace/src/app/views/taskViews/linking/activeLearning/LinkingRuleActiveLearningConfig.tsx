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
    Icon,
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
    ComparisonDataBody,
    ComparisonDataCell,
    ComparisonDataConnection,
    ComparisonDataContainer,
    ComparisonDataHead,
    ComparisonDataHeader,
    ComparisonDataRow,
} from "./components/ComparisionData";
import { PropertyBox } from "./components/PropertyBox";
import { ComparisonPair, ComparisonPairs, ComparisonPairWithId, TypedPath } from "./LinkingRuleActiveLearning.typings";
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
import { legacyApiEndpoint } from "../../../../utils/getApiEndpoint";
import { connectWebSocket } from "../../../../services/websocketUtils";

interface LinkingRuleActiveLearningConfigProps {
    projectId: string;
    linkingTaskId: string;
}

interface ComparisonPairState {
    suggestions: ComparisonPairWithId[];
    suggestionWarnings: string[];
    propertiesToCompare: ComparisonPairWithId[];
}

interface ComparisonPairOverview {
    nrSuggestions: number;
    warnings: string;
    nrSelected;
}

/** Allows to configure the property pairs for the active learning. */
export const LinkingRuleActiveLearningConfig = ({ projectId, linkingTaskId }: LinkingRuleActiveLearningConfigProps) => {
    const { registerError } = useErrorHandler();
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const [loadingSuggestions, setLoadingSuggestions] = React.useState(true);
    const [comparisonPairState, setComparisonPairState] = React.useState<ComparisonPairState>({
        suggestions: [],
        suggestionWarnings: [],
        propertiesToCompare: activeLearningContext.propertiesToCompare,
    });
    const comparisonPairStateOverview = React.useRef<ComparisonPairOverview>({
        nrSuggestions: 0,
        warnings: "",
        nrSelected: 0,
    });
    const [t] = useTranslation();

    // Updates all or parts of the comparison pair state. Checks if the state will change first.
    const updateComparisonPairState = (
        stateUpdateFunction: (currentState: ComparisonPairState) => Partial<ComparisonPairState>
    ) => {
        setComparisonPairState((current) => {
            const updatedState = stateUpdateFunction(current);
            const overview = comparisonPairStateOverview.current;
            let updatedOverviewProperties: Partial<ComparisonPairOverview> | undefined = undefined;
            const update = (updated: Partial<ComparisonPairOverview>) => {
                updatedOverviewProperties = {
                    ...(updatedOverviewProperties ?? {}),
                    ...updated,
                };
            };
            if (updatedState.propertiesToCompare && updatedState.propertiesToCompare.length !== overview.nrSelected) {
                update({ nrSelected: updatedState.propertiesToCompare.length });
            }
            if (updatedState.suggestions && updatedState.suggestions.length !== overview.nrSuggestions) {
                update({ nrSuggestions: updatedState.suggestions.length });
            }
            if (updatedState.suggestionWarnings && updatedState.suggestionWarnings.join(" ") !== overview.warnings) {
                update({ warnings: updatedState.suggestionWarnings.join(" ") });
            }
            if (updatedOverviewProperties) {
                // Only update if one of the overview parameters has changed. This should avoid updating twice in a row (websocket and programmatically)
                comparisonPairStateOverview.current = {
                    ...comparisonPairStateOverview.current,
                    ...(updatedOverviewProperties as Partial<ComparisonPairOverview>),
                };
                return {
                    ...current,
                    ...updatedState,
                };
            } else {
                return current;
            }
        });
    };

    React.useEffect(() => {
        if (!activeLearningContext.comparisonPairsLoading) {
            loadCandidatePairs(projectId, linkingTaskId);
        }
    }, [activeLearningContext.comparisonPairsLoading]);

    const toComparisonPairWithId = (cp: ComparisonPair) => {
        return {
            ...cp,
            pairId: `${cp.source.path} ${cp.target.path} ${cp.source.valueType} ${cp.target.valueType}`,
        };
    };

    React.useEffect(() => {
        const query = `?project=${projectId}&task=${linkingTaskId}&activity=ActiveLearning-ComparisonPairs`;
        setLoadingSuggestions(true);
        const updateSuggestions = (comparisonPairs: ComparisonPairs) => {
            const suggestions = comparisonPairs.suggestedPairs.map((cp) => toComparisonPairWithId(cp));
            // Postpone update a little bit to avoid race condition with updates right after an add or remove action
            setTimeout(() => {
                updateComparisonPairState(() => {
                    return {
                        suggestionWarnings: comparisonPairs.warnings,
                        suggestions: suggestions,
                    };
                });
                if (suggestions.length > 0 || comparisonPairs.finished) {
                    setLoadingSuggestions(false);
                }
            }, 5);
        };
        return connectWebSocket(
            legacyApiEndpoint(`/activities/valueUpdatesWebSocket${query}`),
            legacyApiEndpoint(`/activities/valueUpdates${query}`),
            updateSuggestions
        );
    }, [projectId, linkingTaskId]);

    const loadCandidatePairs = async (projectId: string, taskId: string) => {
        try {
            const comparisonPairs = (await activeLearningComparisonPairs(projectId, taskId)).data;
            if (comparisonPairs.selectedPairs.length > 0 && comparisonPairState.propertiesToCompare.length === 0) {
                updateComparisonPairState(() => ({
                    propertiesToCompare: comparisonPairs.selectedPairs.map((cp) => toComparisonPairWithId(cp)),
                }));
            }
        } catch (ex) {
            registerError(
                "LinkingRuleActiveLearningConfig.loadCandidatePairs",
                t("ActiveLearning.config.errors.fetchComparisionConfig"),
                ex
            );
        }
    };

    const addComparisonPair = React.useCallback(async (pair: ComparisonPairWithId): Promise<boolean> => {
        try {
            await addActiveLearningComparisonPair(projectId, linkingTaskId, pair);
            updateComparisonPairState((current) => ({
                propertiesToCompare: [...current.propertiesToCompare, pair],
            }));
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

    const removePair = React.useCallback(
        async (pairId: string) => {
            try {
                const pair = comparisonPairState.propertiesToCompare.find((pair) => pair.pairId === pairId);
                if (pair) {
                    await removeActiveLearningComparisonPair(projectId, linkingTaskId, pair);
                    updateComparisonPairState((current) => ({
                        propertiesToCompare: current.propertiesToCompare.filter((pair) => pair.pairId !== pairId),
                        // Add again to top of list, so the user can re-add immediately
                        suggestions: [pair, ...current.suggestions],
                    }));
                }
            } catch (error) {
                registerError("ActiveLearningFeedback.removePair", "Removing comparison pair has failed.", error);
            }
        },
        [comparisonPairState.propertiesToCompare]
    );

    const addSuggestion = React.useCallback(
        async (pairId: string) => {
            const pairToAdd = comparisonPairState.suggestions.find((s) => s.pairId === pairId);
            if (pairToAdd) {
                try {
                    await addActiveLearningComparisonPair(projectId, linkingTaskId, pairToAdd);
                    updateComparisonPairState((current) => ({
                        propertiesToCompare: [...current.propertiesToCompare, pairToAdd],
                        // Add again to top of list, so the user can re-add immediately
                        suggestions: current.suggestions.filter((s) => s.pairId !== pairId),
                    }));
                } catch (error) {
                    registerError("ActiveLearningFeedback.addSuggestion", "Adding suggestion has failed.", error);
                }
            }
        },
        [comparisonPairState.suggestions, comparisonPairState.propertiesToCompare]
    );

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
                            data-test-id={"go-to-learning-btn"}
                            affirmative={true}
                            disabled={comparisonPairState.propertiesToCompare.length === 0}
                            onClick={() => {
                                activeLearningContext.setPropertiesToCompare(comparisonPairState.propertiesToCompare);
                                activeLearningContext.navigateTo("linkLearning");
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
                propertiesToCompare={comparisonPairState.propertiesToCompare}
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
                suggestions={comparisonPairState.suggestions}
                loadingSuggestions={loadingSuggestions}
                suggestionWarnings={comparisonPairState.suggestionWarnings}
                addSuggestion={addSuggestion}
            />
        </Section>
    );
};

interface SelectedPropertyPairProps {
    pair: ComparisonPairWithId;
    remove: () => any;
}

/** A single comparison pair in the selected pair list. */
const SelectedPropertyPair = ({ pair, remove }: SelectedPropertyPairProps) => {
    const [t] = useTranslation();
    const sameExampleValues = sameValues(pair.sourceExamples.flat(), pair.targetExamples.flat());
    return (
        <ComparisonDataRow className="diapp-linking-learningdata__row-body" data-test-id={"selected-property-pair-row"}>
            <SelectedProperty
                property={pair.source}
                exampleValues={pair.sourceExamples}
                sameExampleValues={sameExampleValues}
                datasink="source"
            />
            <ComparisonDataConnection>
                <ConnectionEnabled
                    label={utils.comparisonType(pair)}
                    actions={
                        <IconButton text={t("common.action.remove")} name={"item-remove"} disruptive onClick={remove} />
                    }
                />
            </ComparisonDataConnection>
            <SelectedProperty
                property={pair.target}
                exampleValues={pair.targetExamples}
                sameExampleValues={sameExampleValues}
                datasink="target"
            />
        </ComparisonDataRow>
    );
};

interface SelectedPropertiesWidgetProps {
    propertiesToCompare: ComparisonPairWithId[];
    removePair: (pairId: string) => any;
}

/** Shows the selected comparison pairs used for active learning. */
const SelectedPropertiesWidget = ({ propertiesToCompare, removePair }: SelectedPropertiesWidgetProps) => {
    const [t] = useTranslation();

    return (
        <Card elevation={0} data-test-id={"selected-properties-for-active-learning"}>
            <CardHeader>
                <CardTitle>{t("ActiveLearning.config.entitiyPair.title")}</CardTitle>
            </CardHeader>
            <Divider />
            <CardContent>
                <ComparisonDataContainer>
                    <ComparisonPairTableHeader />
                    {(!propertiesToCompare || propertiesToCompare.length === 0) && (
                        <>
                            <Spacing size="small" />
                            <InfoWidget />
                        </>
                    )}
                    <ComparisonDataBody>
                        {(propertiesToCompare ?? []).map((selected) => (
                            <SelectedPropertyPair
                                key={selected.pairId}
                                remove={() => removePair(selected.pairId)}
                                pair={selected}
                            />
                        ))}
                    </ComparisonDataBody>
                </ComparisonDataContainer>
            </CardContent>
        </Card>
    );
};

const InfoWidget = () => {
    const [t] = useTranslation();
    return <Notification message={t("ActiveLearning.config.entitiyPair.info")} />;
};

interface SuggestedPathSelectionProps {
    pair: ComparisonPairWithId;
    filterByPath: (path: string, isTarget: boolean) => any;
    isActiveFilterCheck: (path: string, isTarget: boolean) => any;
    addSuggestion: (pairId: string) => any;
}

/** A comparison pair of the suggested pairs. */
const SuggestedPathSelection = ({
    pair,
    filterByPath,
    isActiveFilterCheck,
    addSuggestion,
}: SuggestedPathSelectionProps) => {
    const [t] = useTranslation();
    const sameExampleValues = sameValues(pair.sourceExamples.flat(), pair.targetExamples.flat());
    return (
        <ComparisonDataRow
            className="diapp-linking-learningdata__row-body"
            data-test-id={"suggested-comparison-pair-row"}
        >
            <SelectedProperty
                property={pair.source}
                exampleValues={pair.sourceExamples}
                sameExampleValues={sameExampleValues}
                filterByPath={() => filterByPath(pair.source.path, false)}
                isActiveFilter={isActiveFilterCheck(pair.source.path, false)}
                datasink="source"
            />
            <ComparisonDataConnection>
                <ConnectionAvailable
                    actions={
                        <IconButton
                            data-test-id={"add-comparison-pair-btn"}
                            text={t("common.action.add")}
                            name={"item-add-artefact"}
                            onClick={() => addSuggestion(pair.pairId)}
                        />
                    }
                />
            </ComparisonDataConnection>
            <SelectedProperty
                property={pair.target}
                exampleValues={pair.targetExamples}
                sameExampleValues={sameExampleValues}
                filterByPath={() => filterByPath(pair.target.path, true)}
                isActiveFilter={isActiveFilterCheck(pair.target.path, true)}
                datasink="target"
            />
        </ComparisonDataRow>
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
        <ComparisonDataCell className={datasink ? `diapp-linking-learningdata__${datasink}` : undefined}>
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
        </ComparisonDataCell>
    );
};

interface SuggestionWidgetProps {
    suggestions: ComparisonPairWithId[];
    loadingSuggestions: boolean;
    suggestionWarnings: string[];
    addSuggestion: (suggestionPair: string) => any;
}

/** Shows the comparison pair suggestion list. */
const SuggestionWidget = ({
    suggestions,
    loadingSuggestions,
    suggestionWarnings,
    addSuggestion,
}: SuggestionWidgetProps) => {
    const [t] = useTranslation();
    const [showInfo, setShowInfo] = React.useState<boolean>(false);
    const pathFilter = React.useRef<{ path: string; isTarget: boolean; label?: string } | undefined>(undefined);
    const [filteredSuggestions, setFilteredSuggestions] = React.useState<ComparisonPairWithId[] | undefined>(undefined);

    React.useEffect(() => {
        if (!showInfo && suggestions.length === 0) {
            setShowInfo(true);
        }
        if (pathFilter.current) {
            // Suggestions have changed, update filtered suggestions
            filterSuggestions(pathFilter.current.path, pathFilter.current.isTarget);
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

    const filterByPath = React.useCallback(
        (path: string, isTarget: boolean) => {
            if (isActiveFilter(path, isTarget)) {
                // Reset filter when same path is clicked again
                resetFilter();
            } else {
                filterSuggestions(path, isTarget);
            }
        },
        [suggestions]
    );

    return (
        <Card elevation={0} data-test-id={"comparison-pair-suggestions"}>
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
                                    icon={suggestions.length > 0 ? <Icon name="item-question" /> : undefined}
                                    intent={suggestions.length > 0 ? "neutral" : undefined}
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
                            <Toolbar>
                                <ToolbarSection canGrow={pathFilter.current.isTarget} />
                                <ToolbarSection>
                                    <div>
                                        <Tag onRemove={resetFilter}>
                                            {pathFilter.current.label ?? pathFilter.current.path}
                                        </Tag>
                                        <Spacing size="small" />
                                    </div>
                                </ToolbarSection>
                            </Toolbar>
                        ) : null}
                        {suggestions.length > 0 && (
                            <ComparisonDataContainer>
                                <ComparisonPairTableHeader />
                                <ComparisonDataBody>
                                    {(filteredSuggestions ?? suggestions).map((suggestion) => {
                                        return (
                                            <SuggestedPathSelection
                                                key={suggestion.pairId}
                                                pair={suggestion}
                                                filterByPath={filterByPath}
                                                isActiveFilterCheck={isActiveFilter}
                                                addSuggestion={addSuggestion}
                                            />
                                        );
                                    })}
                                </ComparisonDataBody>
                            </ComparisonDataContainer>
                        )}
                    </>
                )}
            </CardContent>
        </Card>
    );
};

/** Shows warnings regarding the suggestions. */
const SuggestionsWarningModal = ({ warnings }: { warnings: string[] }) => {
    const [showWarningsModal, setShowWarningsModal] = React.useState<boolean>(false);
    const [t] = useTranslation();

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
            intent="warning"
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
    const [t] = useTranslation();
    return (
        <ComparisonDataHead>
            <ComparisonDataRow>
                <ComparisonDataHeader className="diapp-linking-learningdata__source">
                    {t("ActiveLearning.config.entitiyPair.sourceColumnTitle")}
                </ComparisonDataHeader>
                <ComparisonDataConnection>
                    <ConnectionAvailable actions={<Tag emphasis="weak">owl:sameAs</Tag>} />
                </ComparisonDataConnection>
                <ComparisonDataHeader className="diapp-linking-learningdata__target">
                    {t("ActiveLearning.config.entitiyPair.targetColumnTitle")}
                </ComparisonDataHeader>
            </ComparisonDataRow>
        </ComparisonDataHead>
    );
};
