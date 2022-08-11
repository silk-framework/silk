import {
    Button,
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
import { ActiveLearningDecisions, ComparisonPairWithId, TypedPath } from "../LinkingRuleActiveLearning.typings";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { EntityLink, EntityLinkPropertyPairValues } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";
import referenceLinksUtils from "../../referenceLinks/LinkingRuleReferenceLinks.utils";

export const LinkingRuleActiveLearningFeedbackComponent = () => {
    /** Contexts */
    const activeLearningFeedbackContext = React.useContext(LinkingRuleActiveLearningFeedbackContext);
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    /** The property pairs that will be displayed as entity title during the active learning. */
    const [labelPropertyPairs, setLabelPropertyPairs] = React.useState<ComparisonPairWithId[]>([]);
    /** The values of the selected entity link. */
    const [valuesToDisplay, setValuesToDisplay] = React.useState<EntityLinkPropertyPairValues[] | undefined>();
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
            const sourceValues = referenceLinksUtils.pickEntityValues(
                activeLearningFeedbackContext.selectedLink.source,
                activeLearningContext.propertiesToCompare.map((prop) => prop.source.path)
            );
            const targetValues = referenceLinksUtils.pickEntityValues(
                activeLearningFeedbackContext.selectedLink.target,
                activeLearningContext.propertiesToCompare.map((prop) => prop.target.path)
            );
            const pairValues = sourceValues.map((sourceVals, idx) => ({
                sourceValues: sourceVals,
                targetValues: targetValues[idx] ?? [],
            }));
            setValuesToDisplay(pairValues);
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
    const submitLink = async (link: EntityLink | undefined, decision: ActiveLearningDecisions) => {
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
        <div>
            <Header
                disabledButtons={!activeLearningFeedbackContext.selectedLink}
                submitLink={(decision: ActiveLearningDecisions) =>
                    submitLink(activeLearningFeedbackContext.selectedLink, decision)
                }
            />
            <Spacing />
            {activeLearningFeedbackContext.loadingLinkCandidate ? (
                <Spinner />
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
        </div>
    );
};

interface HeaderProps {
    disabledButtons: boolean;
    /** Submit an entity link to the active learning backend. */
    submitLink: (decision: ActiveLearningDecisions) => Promise<void>;
}

/** TODO: Clean up sub-components */
const Header = ({ disabledButtons, submitLink }: HeaderProps) => {
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);

    return (
        <Toolbar>
            <ToolbarSection>Reference feedback</ToolbarSection>
            <ToolbarSection canGrow={true} />
            <ToolbarSection>
                <Button
                    title={"Confirm that the shown entities are a valid link."}
                    affirmative={true}
                    disabled={disabledButtons}
                    style={{ backgroundColor: "green" }}
                    onClick={() => submitLink("positive")}
                >
                    Confirm
                </Button>
            </ToolbarSection>
            <ToolbarSection>
                <Spacing vertical size={"large"} />
                <Button disabled={disabledButtons} title={"Uncertain"} onClick={() => submitLink("unlabeled")}>
                    Uncertain
                </Button>
                <Spacing vertical size={"large"} />
            </ToolbarSection>
            <ToolbarSection>
                <Button
                    title={"Decline that the shown entities are a valid link."}
                    disabled={disabledButtons}
                    onClick={() => submitLink("negative")}
                    disruptive={true}
                >
                    Decline
                </Button>
            </ToolbarSection>
            <ToolbarSection canGrow={true} />
            <ToolbarSection>
                <IconButton name={"settings"} onClick={() => activeLearningContext.navigateTo("config")} />
            </ToolbarSection>
        </Toolbar>
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
                <ArrowLeft />
                <Tag>owl:sameAs</Tag>
                <ArrowRight />
            </GridColumn>
            <GridColumn style={columnStyles.headerColumnStyle}>Target entity: {targetTitle}</GridColumn>
        </GridRow>
    );
};

interface SelectedEntityLinkProps {
    valuesToDisplay: EntityLinkPropertyPairValues[];
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
    const sourceEntityLabel = labelPropertyPairValues.map((values) => values.sourceValues.join(", ")).join(", ");
    const targetEntityLabel = labelPropertyPairValues.map((values) => values.targetValues.join(", ")).join(", ");
    return (
        <Grid columns={3} fullWidth={true}>
            <EntityComparisonHeader sourceTitle={sourceEntityLabel} targetTitle={targetEntityLabel} />
            {(valuesToDisplay ?? []).map((selected, idx) => (
                <EntitiesPropertyPair
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
            <EntityPropertyValues property={propertyPair.source} values={values.sourceValues} />
            <GridColumn style={columnStyles.centerColumnStyle}>
                <HoverToggler
                    baseElement={
                        <Toolbar style={{ height: "100%" }}>
                            <ToolbarSection canGrow={true}>
                                <ArrowLeft />
                            </ToolbarSection>
                            <ToolbarSection>
                                <Tag>
                                    {propertyPair.source.valueType != null &&
                                    propertyPair.source.valueType === propertyPair.target.valueType
                                        ? propertyPair.source.valueType
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
                                    name={selectedForLabel ? "favorite-filled" : "favorite-empty"}
                                    disruptive
                                    onClick={toggleLabelSelection}
                                />
                            </ToolbarSection>
                            <ToolbarSection canGrow={true} />
                        </Toolbar>
                    }
                />
            </GridColumn>
            <EntityPropertyValues property={propertyPair.target} values={values.targetValues} />
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
                    <OverviewItemLine>{propertyLabel}</OverviewItemLine>
                    {values.length > 0 ? (
                        <OverviewItemLine title={exampleTitle}>
                            {values.map((example) => {
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
