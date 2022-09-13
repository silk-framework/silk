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
    Markdown,
    IconButton,
    InteractionGate,
    Notification,
    Spacing,
    Tag,
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
} from "./../components/ComparisionData";
import { PropertyBox } from "./../components/PropertyBox";
import { LinkingRuleActiveLearningFeedbackContext } from "../contexts/LinkingRuleActiveLearningFeedbackContext";
import { scoreColorConfig, scoreColorRepresentation } from "../LinkingRuleActiveLearning.shared";
import {
    ActiveLearningDecisions,
    ActiveLearningLinkCandidate,
    ActiveLearningReferenceLink,
    ComparisonPair,
    ComparisonPairWithId,
    TypedPath,
} from "../LinkingRuleActiveLearning.typings";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { EntityLink, EntityLinkPropertyPairValues } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";
import ConnectionEnabled from "./../components/ConnectionEnabled";
import ConnectionAvailable from "./../components/ConnectionAvailable";
import { useTranslation } from "react-i18next";
import utils from "../LinkingRuleActiveLearning.utils";
import { ActiveLearningValueExamples, sameValues, highlightedTagColor } from "../shared/ActiveLearningValueExamples";

export const LinkingRuleActiveLearningFeedbackComponent = () => {
    const [t] = useTranslation();
    const [showInfo, setShowInfo] = React.useState<boolean>(false);
    /** Contexts */
    const activeLearningFeedbackContext = React.useContext(LinkingRuleActiveLearningFeedbackContext);
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    /** The property pairs that will be displayed as entity title during the active learning. */
    const [labelPropertyPairs, setLabelPropertyPairs] = React.useState<ComparisonPairWithId[]>([]);
    /** The values of the selected entity link. */
    const [valuesToDisplay, setValuesToDisplay] = React.useState<ComparisonPair[] | undefined>();
    const [submittingEntityLink, setSubmittingEntityLink] = React.useState(false);
    const labelPropertyPairIds = new Set(labelPropertyPairs.map((lpp) => lpp.pairId));
    // When the component is inactive, show spinner
    const loading = activeLearningFeedbackContext.loadingLinkCandidate || submittingEntityLink;

    React.useEffect(() => {
        activeLearningContext.changeLabelPaths({
            sourceProperties: labelPropertyPairs.map((lpp) => lpp.source.path),
            targetProperties: labelPropertyPairs.map((lpp) => lpp.target.path),
        });
    }, [labelPropertyPairs]);

    // Initially set the "label" properties, i.e. values of these properties are shown for entity links
    React.useEffect(() => {
        if (labelPropertyPairs.length === 0 && activeLearningContext.propertiesToCompare.length > 0) {
            setLabelPropertyPairs(activeLearningContext.propertiesToCompare.slice(0, 1));
        }
    }, [activeLearningContext.propertiesToCompare]);

    // Extract values for property pairs
    React.useEffect(() => {
        if (activeLearningContext.propertiesToCompare && activeLearningFeedbackContext.selectedLink) {
            setValuesToDisplay(activeLearningFeedbackContext.selectedLink.comparisons);
        }
    }, [activeLearningContext.propertiesToCompare, activeLearningFeedbackContext.selectedLink]);

    const toggleLabelPropertyPair = (pairId: string) => {
        setLabelPropertyPairs((pairs) => {
            const newPair = activeLearningContext.propertiesToCompare.find((lpp) => lpp.pairId === pairId);
            return pairs.some((value) => value.pairId === pairId) || !newPair
                ? pairs.filter((p) => p.pairId !== pairId)
                : [...pairs, newPair];
        });
    };

    /** Changes the status of a link to the given decision. */
    const submitLink = async (
        link: EntityLink | ActiveLearningLinkCandidate | undefined,
        decision: ActiveLearningDecisions
    ) => {
        if (link) {
            setSubmittingEntityLink(true);
            try {
                await activeLearningFeedbackContext.updateReferenceLink(link, decision);
            } finally {
                setSubmittingEntityLink(false);
            }
        }
    };

    return (
        <Card elevation={0}>
            <Header
                disabledButtons={!activeLearningFeedbackContext.selectedLink}
                selectedDecision={(activeLearningFeedbackContext.selectedLink as ActiveLearningReferenceLink)?.decision}
                cancel={activeLearningFeedbackContext.cancel}
                toggleInfo={() => setShowInfo(!showInfo)}
            />
            <Divider />
            <CardContent>
                {showInfo && (
                    <>
                        <Notification
                            neutral
                            actions={
                                <IconButton
                                    text={t("common.action.close")}
                                    name="navigation-close"
                                    onClick={() => setShowInfo(false)}
                                />
                            }
                        >
                            <HtmlContentBlock>
                                <Markdown inheritBlock>{t("ActiveLearning.feedback.info")}</Markdown>
                                <MatchingColorInfo />
                            </HtmlContentBlock>
                        </Notification>
                        <Spacing />
                    </>
                )}
                <InteractionGate inert={loading} showSpinner={loading} spinnerProps={{ delay: 500 }}>
                    <DecisionButtons
                        disabledButtons={!activeLearningFeedbackContext.selectedLink}
                        submitLink={(decision: ActiveLearningDecisions) =>
                            submitLink(activeLearningFeedbackContext.selectedLink, decision)
                        }
                        selectedDecision={
                            (activeLearningFeedbackContext.selectedLink as ActiveLearningReferenceLink)?.decision
                        }
                        cancel={activeLearningFeedbackContext.cancel}
                    />
                    <Spacing size={"small"} />
                    {valuesToDisplay ? (
                        <SelectedEntityLink
                            valuesToDisplay={valuesToDisplay}
                            propertyPairs={activeLearningContext.propertiesToCompare}
                            labelPropertyPairIds={labelPropertyPairIds}
                            toggleLabelPropertyPair={toggleLabelPropertyPair}
                        />
                    ) : (
                        <Notification message={t("ActiveLearning.feedback.noSelection")} />
                    )}
                </InteractionGate>
            </CardContent>
        </Card>
    );
};

interface HeaderProps {
    disabledButtons: boolean;
    /** The currently selected decision. */
    selectedDecision?: ActiveLearningDecisions;
    /** Cancel changing an existing link. */
    cancel: () => any;
    /** Handler to toggle the info area. */
    toggleInfo: () => void;
}

/** TODO: Clean up sub-components */
const Header = ({ disabledButtons, selectedDecision, cancel, toggleInfo }: HeaderProps) => {
    const [t] = useTranslation();
    const positiveSelected = selectedDecision === "positive";
    const negativeSelected = selectedDecision === "negative";

    return (
        <CardHeader>
            <CardTitle>{t("ActiveLearning.feedback.title")}</CardTitle>
            <CardOptions>
                {(positiveSelected || negativeSelected) && (
                    <>
                        <Button
                            text={t("ActiveLearning.feedback.cancel", "Cancel re-evaluation")}
                            onClick={() => cancel()}
                            disabled={disabledButtons}
                        />
                        <Spacing vertical size="small" />
                    </>
                )}
                <IconButton
                    text={t("ActiveLearning.config.buttons.showInfo")}
                    name="item-info"
                    onClick={() => toggleInfo()}
                />
            </CardOptions>
        </CardHeader>
    );
};

interface DecisionButtonsProps {
    disabledButtons: boolean;
    /** Submit an entity link to the active learning backend. */
    submitLink: (decision: ActiveLearningDecisions) => Promise<void>;
    /** The currently selected decision. */
    selectedDecision?: ActiveLearningDecisions;
    /** Cancel changing an existing link. */
    cancel: () => any;
}

/** TODO: Clean up sub-components */
const DecisionButtons = ({ disabledButtons, submitLink, selectedDecision, cancel }: DecisionButtonsProps) => {
    const [t] = useTranslation();
    const positiveSelected = selectedDecision === "positive";
    const negativeSelected = selectedDecision === "negative";

    return (
        <div style={{ textAlign: "center" }}>
            <Button
                title={t("ActiveLearning.feedback.confirmDescription")}
                icon={"state-confirmed"}
                disabled={disabledButtons}
                onClick={() => (positiveSelected ? cancel() : submitLink("positive"))}
                elevated={positiveSelected}
                outlined
            >
                {t("ActiveLearning.feedback.confirm")}
            </Button>
            <Spacing vertical size={"small"} />
            <Button
                title={t("ActiveLearning.feedback.uncertainDescription")}
                disabled={disabledButtons}
                onClick={() => submitLink("unlabeled")}
                outlined
            >
                {t("ActiveLearning.feedback.uncertain")}
            </Button>
            <Spacing vertical size={"small"} />
            <Button
                title={t("ActiveLearning.feedback.declineDescription")}
                disabled={disabledButtons}
                onClick={() => (negativeSelected ? cancel() : submitLink("negative"))}
                icon={"state-declined"}
                elevated={negativeSelected}
                outlined
            >
                {t("ActiveLearning.feedback.decline")}
            </Button>
        </div>
    );
};

interface EntityComparisonHeaderProps {
    sourceTitle: string;
    targetTitle: string;
}

const EntityComparisonHeader = ({ sourceTitle, targetTitle }: EntityComparisonHeaderProps) => {
    const [t] = useTranslation();
    return (
        <ComparisionDataHead>
            <ComparisionDataRow>
                <ComparisionDataHeader className="diapp-linking-learningdata__source">
                    {t("ActiveLearning.feedback.sourceColumnTitle")}
                    {sourceTitle ? ": " : ""}
                    {sourceTitle}
                </ComparisionDataHeader>
                <ComparisionDataConnection>
                    <ConnectionAvailable actions={<Tag emphasis="weak">owl:sameAs</Tag>} />
                </ComparisionDataConnection>
                <ComparisionDataHeader className="diapp-linking-learningdata__target">
                    {t("ActiveLearning.feedback.targetColumnTitle")}
                    {targetTitle ? ": " : ""}
                    {targetTitle}
                </ComparisionDataHeader>
            </ComparisionDataRow>
        </ComparisionDataHead>
    );
};

interface SelectedEntityLinkProps {
    valuesToDisplay: EntityLinkPropertyPairValues[] | ComparisonPair[];
    propertyPairs: ComparisonPairWithId[];
    labelPropertyPairIds: Set<string>;
    toggleLabelPropertyPair: (pairId: string) => any;
}

/** The two entities that are considered to be linked. */
const SelectedEntityLink = ({
    valuesToDisplay,
    propertyPairs,
    labelPropertyPairIds,
    toggleLabelPropertyPair,
}: SelectedEntityLinkProps) => {
    const propertyPairMap = new Map(propertyPairs.map((pp, idx) => [pp.pairId, idx]));
    const labelPropertyPairValues = [...labelPropertyPairIds]
        .map((id, idx) => (propertyPairMap.has(id) ? valuesToDisplay[propertyPairMap.get(id)!!] : null))
        .filter((values) => values != null) as EntityLinkPropertyPairValues[];
    const sourceEntityLabel = labelPropertyPairValues.map((values) => values.sourceExamples.join(", ")).join(", ");
    const targetEntityLabel = labelPropertyPairValues.map((values) => values.targetExamples.join(", ")).join(", ");
    return (
        <ComparisionDataContainer>
            <EntityComparisonHeader sourceTitle={sourceEntityLabel} targetTitle={targetEntityLabel} />
            <ComparisionDataBody>
                {(valuesToDisplay ?? []).map((selected: EntityLinkPropertyPairValues | ComparisonPair, idx) => {
                    const values: EntityLinkPropertyPairValues = {
                        sourceExamples: selected.sourceExamples.flat(),
                        targetExamples: selected.targetExamples.flat(),
                    };
                    return (
                        <EntitiesPropertyPair
                            key={idx}
                            propertyPair={propertyPairs[idx]}
                            selectedForLabel={labelPropertyPairIds.has(propertyPairs[idx].pairId)}
                            toggleLabelSelection={() => toggleLabelPropertyPair(propertyPairs[idx].pairId)}
                            values={values}
                            score={(selected as ComparisonPair).score}
                        />
                    );
                })}
            </ComparisionDataBody>
        </ComparisionDataContainer>
    );
};

interface EntitiesPropertyPairProps {
    propertyPair: ComparisonPairWithId;
    values: EntityLinkPropertyPairValues;
    selectedForLabel: boolean;
    toggleLabelSelection: () => any;
    score?: number;
}

const EntitiesPropertyPair = ({
    propertyPair,
    values,
    selectedForLabel,
    toggleLabelSelection,
    score,
}: EntitiesPropertyPairProps) => {
    const scoreColor = scoreColorRepresentation(score);
    const [t] = useTranslation();
    const sameExampleValues = sameValues(values.sourceExamples, values.targetExamples);
    return (
        <ComparisionDataRow className="diapp-linking-learningdata__row-body">
            <EntityPropertyValues
                property={propertyPair.source}
                values={values.sourceExamples}
                sameExampleValues={sameExampleValues}
                datasink="source"
            />
            <ComparisionDataConnection>
                <ConnectionEnabled
                    label={utils.comparisonType(propertyPair)}
                    actions={
                        <IconButton
                            text={
                                selectedForLabel
                                    ? t("ActiveLearning.feedback.removeFromLabel")
                                    : t("ActiveLearning.feedback.addToLabel")
                            }
                            name={selectedForLabel ? "favorite-filled" : "favorite-empty"}
                            elevated
                            onClick={toggleLabelSelection}
                        />
                    }
                    color={scoreColor}
                />
            </ComparisionDataConnection>
            <EntityPropertyValues
                property={propertyPair.target}
                values={values.targetExamples}
                sameExampleValues={sameExampleValues}
                datasink="target"
            />
        </ComparisionDataRow>
    );
};

interface EntityPropertyValuesProps {
    values: string[];
    property: TypedPath;
    sameExampleValues: Set<string>;
    datasink?: "source" | "target";
}

const EntityPropertyValues = ({ property, values, sameExampleValues, datasink }: EntityPropertyValuesProps) => {
    const propertyLabel = property.label ? property.label : property.path;
    const exampleTitle = values.join(" | ");
    return (
        <ComparisionDataCell className={datasink ? `diapp-linking-learningdata__${datasink}` : undefined}>
            <PropertyBox
                propertyName={propertyLabel}
                exampleValues={values.length > 0 ? (
                    <ActiveLearningValueExamples
                        exampleValues={values}
                        valuesToHighlight={sameExampleValues}
                    />
                ) : undefined}
                exampleTooltip={exampleTitle}
            />
        </ComparisionDataCell>
    );
};

const MatchingColorInfo = () => {
    const [t] = useTranslation();
    return (
        <Toolbar>
            <ToolbarSection canGrow={true} style={{ width: "75%" }}>
                <Tag large backgroundColor={scoreColorConfig.strongEquality.backgroundColor} style={{ width: "33%" }}>
                    {t("ActiveLearning.feedback.scoreProbablyEqual")}
                </Tag>
                <Tag large backgroundColor={scoreColorConfig.weakEquality.backgroundColor} style={{ width: "33%" }}>
                    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;
                </Tag>
                <Tag
                    large
                    backgroundColor={scoreColorConfig.noEquality.backgroundColor}
                    style={{ width: "33%", textAlign: "right" }}
                >
                    {t("ActiveLearning.feedback.scoreProbablyUnequal")}
                </Tag>
            </ToolbarSection>
            <ToolbarSection canGrow={true} />
            <ToolbarSection canGrow={true} style={{ width: "15%" }}>
                <Tag round large backgroundColor={highlightedTagColor} style={{ width: "100%", textAlign: "center" }}>
                    {t("ActiveLearning.feedback.equalValues")}
                </Tag>
            </ToolbarSection>
        </Toolbar>
    );
};
