import {
    Button,
    Card,
    CardHeader,
    CardTitle,
    CardOptions,
    CardContent,
    Divider,
    Grid,
    GridColumn,
    GridRow,
    HoverToggler,
    IconButton,
    Notification,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Spinner,
    Tag,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import React from "react";
import { LinkingRuleActiveLearningFeedbackContext } from "../contexts/LinkingRuleActiveLearningFeedbackContext";
import { ArrowLeft, ArrowRight, columnStyles } from "../LinkingRuleActiveLearning.shared";
import {
    ActiveLearningDecisions,
    ActiveLearningLinkCandidate,
    ComparisonPair,
    ComparisonPairWithId,
    TypedPath,
} from "../LinkingRuleActiveLearning.typings";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { EntityLink, EntityLinkPropertyPairValues } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";
import referenceLinksUtils from "../../referenceLinks/LinkingRuleReferenceLinks.utils";
import { useTranslation } from "react-i18next";
import utils from "../LinkingRuleActiveLearning.utils";

export const LinkingRuleActiveLearningFeedbackComponent = () => {
    /** Contexts */
    const activeLearningFeedbackContext = React.useContext(LinkingRuleActiveLearningFeedbackContext);
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    /** The property pairs that will be displayed as entity title during the active learning. */
    const [labelPropertyPairs, setLabelPropertyPairs] = React.useState<ComparisonPairWithId[]>([]);
    /** The values of the selected entity link. */
    const [valuesToDisplay, setValuesToDisplay] = React.useState<
        EntityLinkPropertyPairValues[] | ComparisonPair[] | undefined
    >();
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
            if ((activeLearningFeedbackContext.selectedLink as EntityLink).decision) {
                const link = activeLearningFeedbackContext.selectedLink as EntityLink;
                const sourceValues = referenceLinksUtils.pickEntityValues(
                    link.source,
                    activeLearningContext.propertiesToCompare.map((prop) => prop.source.path)
                );
                const targetValues = referenceLinksUtils.pickEntityValues(
                    link.target,
                    activeLearningContext.propertiesToCompare.map((prop) => prop.target.path)
                );
                const pairValues = sourceValues.map((sourceVals, idx) => ({
                    sourceExamples: sourceVals,
                    targetExamples: targetValues[idx] ?? [],
                }));
                setValuesToDisplay(pairValues);
            } else {
                const candidate = activeLearningFeedbackContext.selectedLink as ActiveLearningLinkCandidate;
                setValuesToDisplay(candidate.comparisons);
            }
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
                submitLink={(decision: ActiveLearningDecisions) =>
                    submitLink(activeLearningFeedbackContext.selectedLink, decision)
                }
                selectedDecision={(activeLearningFeedbackContext.selectedLink as EntityLink)?.decision}
                cancel={activeLearningFeedbackContext.cancel}
            />
            <Divider />
            <CardContent>
                {loading ? (
                    <Spinner delay={500} />
                ) : valuesToDisplay ? (
                    <SelectedEntityLink
                        valuesToDisplay={valuesToDisplay}
                        propertyPairs={activeLearningContext.propertiesToCompare}
                        labelPropertyPairIds={labelPropertyPairIds}
                        toggleLabelPropertyPair={toggleLabelPropertyPair}
                    />
                ) : (
                    <Notification message={"No entity link selected"} />
                )}
                <Spacing />
                <MatchingColorInfo />
            </CardContent>
        </Card>
    );
};

interface HeaderProps {
    disabledButtons: boolean;
    /** Submit an entity link to the active learning backend. */
    submitLink: (decision: ActiveLearningDecisions) => Promise<void>;
    /** The currently selected decision. */
    selectedDecision?: ActiveLearningDecisions;
    /** Cancel changing an existing link. */
    cancel: () => any;
}

/** TODO: Clean up sub-components */
const Header = ({ disabledButtons, submitLink, selectedDecision, cancel }: HeaderProps) => {
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const [t] = useTranslation();

    return (
        <CardHeader>
            <CardTitle>Reference feedback</CardTitle>
            <CardOptions>
                <Button
                    title={"Confirm that the shown entities are a valid link."}
                    icon={"state-confirmed"}
                    disabled={disabledButtons}
                    onClick={() => submitLink("positive")}
                    elevated={selectedDecision === "positive"}
                    outlined
                >
                    {t("ActiveLearning.feedback.confirm")}
                </Button>
                <Spacing vertical size={"small"} />
                <Button outlined disabled={disabledButtons} title={"Uncertain"} onClick={() => submitLink("unlabeled")}>
                    {t("ActiveLearning.feedback.uncertain")}
                </Button>
                <Spacing vertical size={"small"} />
                <Button
                    title={"Decline that the shown entities are a valid link."}
                    disabled={disabledButtons}
                    onClick={() => submitLink("negative")}
                    icon={"state-declined"}
                    elevated={selectedDecision === "negative"}
                    outlined
                >
                    {t("ActiveLearning.feedback.decline")}
                </Button>
                <Spacing vertical />
                <IconButton name={"settings"} onClick={() => activeLearningContext.navigateTo("config")} />
            </CardOptions>
        </CardHeader>
    );
};

interface EntityComparisonHeaderProps {
    sourceTitle: string;
    targetTitle: string;
}

const EntityComparisonHeader = ({ sourceTitle, targetTitle }: EntityComparisonHeaderProps) => {
    return (
        <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
            <GridColumn style={columnStyles.headerColumnStyle}>Source entity: {sourceTitle}</GridColumn>
            <GridColumn style={columnStyles.centerColumnStyle}>
                <Toolbar style={{ height: "100%" }}>
                    <ToolbarSection canGrow={true}>
                        <ArrowLeft />
                    </ToolbarSection>
                    <ToolbarSection>
                        <Tag>owl:sameAs</Tag>
                    </ToolbarSection>
                    <ToolbarSection canGrow={true}>
                        <ArrowRight />
                    </ToolbarSection>
                </Toolbar>
            </GridColumn>
            <GridColumn style={columnStyles.headerColumnStyle}>Target entity: {targetTitle}</GridColumn>
        </GridRow>
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
        <Grid columns={3} fullWidth={true}>
            <EntityComparisonHeader sourceTitle={sourceEntityLabel} targetTitle={targetEntityLabel} />
            {(valuesToDisplay ?? []).map((selected, idx) => (
                <EntitiesPropertyPair
                    key={idx}
                    propertyPair={propertyPairs[idx]}
                    selectedForLabel={labelPropertyPairIds.has(propertyPairs[idx].pairId)}
                    toggleLabelSelection={() => toggleLabelPropertyPair(propertyPairs[idx].pairId)}
                    values={selected}
                />
            ))}
        </Grid>
    );
};

interface EntitiesPropertyPairProps {
    propertyPair: ComparisonPairWithId;
    values: EntityLinkPropertyPairValues;
    selectedForLabel: boolean;
    toggleLabelSelection: () => any;
}

const EntitiesPropertyPair = ({
    propertyPair,
    values,
    selectedForLabel,
    toggleLabelSelection,
}: EntitiesPropertyPairProps) => {
    return (
        <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
            <EntityPropertyValues property={propertyPair.source} values={values.sourceExamples} />
            <GridColumn style={columnStyles.centerColumnStyle}>
                <HoverToggler
                    style={{ height: "100%" }}
                    baseContent={
                        <Toolbar>
                            <ToolbarSection canGrow={true}>
                                <ArrowLeft />
                            </ToolbarSection>
                            <ToolbarSection>
                                <Tag>
                                    {propertyPair.source.valueType != null &&
                                    propertyPair.source.valueType === propertyPair.target.valueType
                                        ? utils.convertValueType(propertyPair.source.valueType)
                                        : "string"}
                                </Tag>
                            </ToolbarSection>
                            <ToolbarSection canGrow={true}>
                                <ArrowRight />
                            </ToolbarSection>
                        </Toolbar>
                    }
                    baseContentProps={{ style: { width: "100%" } }}
                    hoverContent={
                        <>
                            <IconButton
                                name={selectedForLabel ? "favorite-filled" : "favorite-empty"}
                                elevated
                                onClick={toggleLabelSelection}
                            />
                        </>
                    }
                />
            </GridColumn>
            <EntityPropertyValues property={propertyPair.target} values={values.targetExamples} />
        </GridRow>
    );
};

const EntityPropertyValues = ({ property, values }: { values: string[]; property: TypedPath }) => {
    const propertyLabel = property.label ? property.label : property.path;
    const exampleTitle = values.join(" | ");
    return (
        <GridColumn style={columnStyles.mainColumnStyle}>
            <OverviewItem>
                <OverviewItemDescription>
                    <OverviewItemLine small>{propertyLabel}</OverviewItemLine>
                    {values.length > 0 ? (
                        <OverviewItemLine title={exampleTitle}>
                            {values.map((example) => {
                                return (
                                    <Tag
                                        key={example}
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

const MatchingColorInfo = () => {
    return (
        <Toolbar>
            <ToolbarSection canGrow={true} />
            <ToolbarSection canGrow={true}>
                <Button style={{ width: "10%", backgroundColor: "mediumblue", color: "white" }}>Probably equal</Button>
                <Button style={{ width: "10%", backgroundColor: "lightblue" }}>
                    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;
                </Button>
                <Button style={{ width: "10%" }}>Probably unequal</Button>
            </ToolbarSection>
            <ToolbarSection canGrow={true} />
        </Toolbar>
    );
};
