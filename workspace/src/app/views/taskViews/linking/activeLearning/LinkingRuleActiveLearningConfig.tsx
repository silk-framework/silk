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
            activeLearningContext.setPropertiesToCompare((current) => [...current, pair]);
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
                    <ComparisionDataHeader className="diapp-linking-learningdata__source">
                        {t("widget.Filterbar.subsections.valueLabels.itemType.dataset")} 1
                    </ComparisionDataHeader>
                    <ComparisionDataConnection>
                        <ConnectionAvailable actions={<Tag emphasis="weak">owl:sameAs</Tag>} />
                    </ComparisionDataConnection>
                    <ComparisionDataHeader className="diapp-linking-learningdata__target">
                        {t("widget.Filterbar.subsections.valueLabels.itemType.dataset")} 2
                    </ComparisionDataHeader>
                </ComparisionDataRow>
            </ComparisionDataHead>
        );
    };

    const SelectedProperty = ({
        property,
        exampleValues,
        filterByPath,
        datasink
    }: {
        property: TypedPath;
        exampleValues: string[][];
        sameExampleValues: Set<string>;
        filterByPath?: () => any;
        datasink?: "source" | "target"
    }) => {
        const flatExampleValues: string[] = [].concat.apply([], exampleValues);
        const showLabel: boolean = !!property.label && property.label.toLowerCase() !== property.path.toLowerCase();
        const exampleTitle = flatExampleValues.join(" | ");
        return (
            <ComparisionDataCell className={datasink ? `diapp-linking-learningdata__${datasink}` : undefined}>
                <PropertyBox
                    propertyName={property.label ?? property.path}
                    propertyTooltip={showLabel ? property.path : undefined}
                    exampleValues={flatExampleValues.length > 0 ? (
                        <ActiveLearningValueExamples
                            exampleValues={flatExampleValues}
                        />
                    ) : undefined}
                    exampleTooltip={exampleTitle}
                    onFilter={filterByPath}
                />
            </ComparisionDataCell>
        );
    };

    const SelectedPropertyPair = ({ pair }: { pair: ComparisonPairWithId }) => {
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
                                onClick={() => removePair(pair.pairId)}
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

    const SelectedPropertiesWidget = () => {
        return (
            <Card elevation={0}>
                <CardHeader>
                    <CardTitle>{t("ActiveLearning.config.entitiyPair.title")}</CardTitle>
                </CardHeader>
                <Divider />
                <CardContent>
                    <ComparisionDataContainer>
                        <ConfigHeader />
                        {(!activeLearningContext.propertiesToCompare ||
                            activeLearningContext.propertiesToCompare.length === 0) && (
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

        const filterByPath = React.useCallback((path: string, isTarget: boolean) => {
            const current = pathFilter.current;
            if (current && current.path === path && current.isTarget === isTarget) {
                // Reset filter when same path is clicked again
                resetFilter();
            } else {
                filterSuggestions(path, isTarget);
            }
        }, []);

        return (
            <Card elevation={0}>
                <CardHeader>
                    <CardTitle>
                        {t("ActiveLearning.config.suggestions.title")}
                        {!loadSuggestions && suggestions.length > 0 && " (" + suggestions.length + ")"}
                    </CardTitle>
                    <CardOptions>
                        {!loadingSuggestions && suggestionWarnings && (
                            <SuggestionsWarningModal warnings={suggestionWarnings} />
                        )}
                        {!loadSuggestions && suggestions.length > 0 && (
                            <IconButton
                                name="item-info"
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
                                    <Spacing />
                                </div>
                            ) : null}
                            {suggestions.length > 0 && (
                                <ComparisionDataContainer>
                                    <ConfigHeader />
                                    <ComparisionDataBody>
                                        {(filteredSuggestions ?? suggestions).map((suggestion) => (
                                            <SuggestedPathSelection
                                                key={suggestion.pairId}
                                                pair={suggestion}
                                                filterByPath={filterByPath}
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
    };

    const SuggestionsWarningModal = ({ warnings }: { warnings: string[] }) => {
        const [showWarningsModal, setShowWarningsModal] = React.useState<boolean>(false);

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

    const SuggestedPathSelection = ({
        pair,
        filterByPath,
    }: {
        pair: ComparisonPairWithId;
        filterByPath: (path: string, isTarget: boolean) => any;
    }) => {
        const sameExampleValues = sameValues(pair.sourceExamples.flat(), pair.targetExamples.flat());
        return (
            <ComparisionDataRow className="diapp-linking-learningdata__row-body">
                <SelectedProperty
                    property={pair.source}
                    exampleValues={pair.sourceExamples}
                    sameExampleValues={sameExampleValues}
                    filterByPath={() => filterByPath(pair.source.path, false)}
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
                    datasink="target"
                />
            </ComparisionDataRow>
        );
    };

    const InfoWidget = () => {
        return <Notification iconName={"item-info"} message={t("ActiveLearning.config.entitiyPair.info")} />;
    };

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
                            disabled={activeLearningContext.propertiesToCompare.length === 0}
                            onClick={() => activeLearningContext.navigateTo("linkLearning")}
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
