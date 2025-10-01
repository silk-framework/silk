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
    Link,
    Markdown,
    MenuItem,
    ContextMenu,
    Icon,
    IconButton,
    InteractionGate,
    Notification,
    Spacing,
    Tag,
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
    UnlabeledEntityLink,
} from "../LinkingRuleActiveLearning.typings";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { EntityLink, EntityLinkPropertyPairValues } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";
import ConnectionEnabled from "./../components/ConnectionEnabled";
import ConnectionAvailable from "./../components/ConnectionAvailable";
import { useTranslation } from "react-i18next";
import utils from "../LinkingRuleActiveLearning.utils";
import { ActiveLearningValueExamples, sameValues, highlightedTagColor } from "../shared/ActiveLearningValueExamples";
import { EntityLinkUrisModal } from "../../referenceLinks/EntityLinkUrisModal";

interface Props {
    /** Called when changes are made that still need to be saved. */
    setUnsavedStateExists: () => any;
}

export const LinkingRuleActiveLearningFeedbackComponent = ({ setUnsavedStateExists }: Props) => {
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
    const [entityUrisToOpenInModal, setEntityUrisToOpenInModal] = React.useState<UnlabeledEntityLink | undefined>(
        undefined
    );

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
                setUnsavedStateExists();
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
                showEntityUris={
                    activeLearningFeedbackContext.selectedLink
                        ? () => setEntityUrisToOpenInModal(activeLearningFeedbackContext.selectedLink)
                        : undefined
                }
            />
            <Divider />
            <CardContent>
                {showInfo && (
                    <>
                        <Notification
                            intent="neutral"
                            icon={<Icon name="item-question" />}
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
                            resourceLinks={{
                                source: activeLearningFeedbackContext.selectedLink?.sourceBrowserUrl,
                                target: activeLearningFeedbackContext.selectedLink?.targetBrowserUrl,
                            }}
                            valuesToDisplay={valuesToDisplay}
                            propertyPairs={activeLearningContext.propertiesToCompare}
                            labelPropertyPairIds={labelPropertyPairIds}
                            toggleLabelPropertyPair={toggleLabelPropertyPair}
                        />
                    ) : (
                        <Notification message={t("ActiveLearning.feedback.noSelection")} />
                    )}
                </InteractionGate>
                {entityUrisToOpenInModal ? (
                    <EntityLinkUrisModal
                        link={entityUrisToOpenInModal}
                        onClose={() => setEntityUrisToOpenInModal(undefined)}
                    />
                ) : null}
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
    /** If defined, adds a menu to show the entity URIs. */
    showEntityUris?: () => any;
}

const Header = ({ disabledButtons, selectedDecision, cancel, toggleInfo, showEntityUris }: HeaderProps) => {
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
                    name={"item-question"}
                    onClick={() => toggleInfo()}
                />
                <ContextMenu
                    data-test-id={"active-learning-feedback-more-menu"}
                    togglerText={t("common.action.moreOptions", "Show more options")}
                    disabled={!showEntityUris}
                >
                    {showEntityUris ? (
                        <MenuItem
                            data-test-id="show-entity-uris"
                            icon="item-viewdetails"
                            onClick={showEntityUris}
                            text={t("ReferenceLinks.showEntityUris.menuText")}
                        />
                    ) : undefined}
                </ContextMenu>
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

const DecisionButtons = ({ disabledButtons, submitLink, selectedDecision, cancel }: DecisionButtonsProps) => {
    const [t] = useTranslation();
    const positiveSelected = selectedDecision === "positive";
    const negativeSelected = selectedDecision === "negative";

    return (
        <div style={{ textAlign: "center" }}>
            <Button
                data-test-id={"learning-confirm-btn"}
                title={t("ActiveLearning.feedback.confirmDescription")}
                icon={"state-confirmed"}
                disabled={disabledButtons}
                onClick={() => (positiveSelected ? cancel() : submitLink("positive"))}
                outlined={!positiveSelected}
                intent="success"
            >
                {t("ActiveLearning.feedback.confirm")}
            </Button>
            <Spacing vertical size={"small"} />
            <Button
                data-test-id={"learning-uncertain-btn"}
                title={t("ActiveLearning.feedback.uncertainDescription")}
                disabled={disabledButtons}
                onClick={() => submitLink("unlabeled")}
                outlined
            >
                {t("ActiveLearning.feedback.uncertain")}
            </Button>
            <Spacing vertical size={"small"} />
            <Button
                data-test-id={"learning-decline-btn"}
                title={t("ActiveLearning.feedback.declineDescription")}
                disabled={disabledButtons}
                onClick={() => (negativeSelected ? cancel() : submitLink("negative"))}
                icon={"state-declined"}
                outlined={!negativeSelected}
                intent="danger"
            >
                {t("ActiveLearning.feedback.decline")}
            </Button>
        </div>
    );
};

interface EntityComparisonHeaderProps {
    sourceTitle: string;
    targetTitle: string;
    sourceUrl?: string;
    targetUrl?: string;
}

const EntityComparisonHeader = ({ sourceTitle, targetTitle, sourceUrl, targetUrl }: EntityComparisonHeaderProps) => {
    const [t] = useTranslation();
    return (
        <ComparisonDataHead>
            <ComparisonDataRow>
                <ComparisonDataHeader className="diapp-linking-learningdata__source">
                    {!sourceTitle && sourceUrl ? (
                        <Link href={sourceUrl} target="_new">
                            {t("ActiveLearning.feedback.sourceColumnTitle")}
                        </Link>
                    ) : (
                        t("ActiveLearning.feedback.sourceColumnTitle")
                    )}
                    {sourceTitle ? ": " : ""}
                    {sourceTitle && sourceUrl ? (
                        <Link href={sourceUrl} target="_new">
                            {sourceTitle}
                        </Link>
                    ) : (
                        sourceTitle
                    )}
                </ComparisonDataHeader>
                <ComparisonDataConnection>
                    <ConnectionAvailable actions={<Tag emphasis="weak">owl:sameAs</Tag>} />
                </ComparisonDataConnection>
                <ComparisonDataHeader className="diapp-linking-learningdata__target">
                    {!targetTitle && targetUrl ? (
                        <Link href={targetUrl} target="_new">
                            {t("ActiveLearning.feedback.targetColumnTitle")}
                        </Link>
                    ) : (
                        t("ActiveLearning.feedback.targetColumnTitle")
                    )}
                    {targetTitle ? ": " : ""}
                    {targetTitle && targetUrl ? (
                        <Link href={targetUrl} target="_new">
                            {targetTitle}
                        </Link>
                    ) : (
                        targetTitle
                    )}
                </ComparisonDataHeader>
            </ComparisonDataRow>
        </ComparisonDataHead>
    );
};

interface SelectedEntityLinkProps {
    resourceLinks?: { source?: string; target?: string };
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
    resourceLinks,
}: SelectedEntityLinkProps) => {
    const propertyPairMap = new Map(propertyPairs.map((pp, idx) => [pp.pairId, idx]));
    const labelPropertyPairValues = [...labelPropertyPairIds]
        .map((id, idx) => (propertyPairMap.has(id) ? valuesToDisplay[propertyPairMap.get(id)!!] : null))
        .filter((values) => values != null) as EntityLinkPropertyPairValues[];
    const sourceEntityLabel = labelPropertyPairValues.map((values) => values.sourceExamples.join(", ")).join(", ");
    const targetEntityLabel = labelPropertyPairValues.map((values) => values.targetExamples.join(", ")).join(", ");
    return (
        <ComparisonDataContainer>
            <EntityComparisonHeader
                sourceUrl={resourceLinks ? resourceLinks.source : undefined}
                targetUrl={resourceLinks ? resourceLinks.target : undefined}
                sourceTitle={sourceEntityLabel}
                targetTitle={targetEntityLabel}
            />
            <ComparisonDataBody>
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
            </ComparisonDataBody>
        </ComparisonDataContainer>
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
        <ComparisonDataRow className="diapp-linking-learningdata__row-body">
            <EntityPropertyValues
                property={propertyPair.source}
                values={values.sourceExamples}
                sameExampleValues={sameExampleValues}
                datasink="source"
            />
            <ComparisonDataConnection>
                <ConnectionEnabled
                    label={utils.comparisonType(propertyPair)}
                    actions={
                        <IconButton
                            text={
                                selectedForLabel
                                    ? t("ActiveLearning.feedback.removeFromLabel")
                                    : t("ActiveLearning.feedback.addToLabel")
                            }
                            name={selectedForLabel ? "toggler-star-filled" : "toggler-star-empty"}
                            elevated
                            onClick={toggleLabelSelection}
                        />
                    }
                    color={scoreColor}
                />
            </ComparisonDataConnection>
            <EntityPropertyValues
                property={propertyPair.target}
                values={values.targetExamples}
                sameExampleValues={sameExampleValues}
                datasink="target"
            />
        </ComparisonDataRow>
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
        <ComparisonDataCell className={datasink ? `diapp-linking-learningdata__${datasink}` : undefined}>
            <PropertyBox
                propertyName={propertyLabel}
                exampleValues={
                    values.length > 0 ? (
                        <ActiveLearningValueExamples exampleValues={values} valuesToHighlight={sameExampleValues} />
                    ) : undefined
                }
                exampleTooltip={exampleTitle}
            />
        </ComparisonDataCell>
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
