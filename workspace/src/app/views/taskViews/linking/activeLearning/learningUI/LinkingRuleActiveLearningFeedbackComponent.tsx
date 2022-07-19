import {
    Button,
    Grid,
    GridColumn,
    GridRow,
    HoverToggler,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Tag,
    Toolbar,
    ToolbarSection,
    Notification,
    IconButton,
} from "@eccenca/gui-elements";
import React from "react";
import { LinkingRuleActiveLearningFeedbackContext } from "../contexts/LinkingRuleActiveLearningFeedbackContext";
import { ArrowLeft, ArrowRight, columnStyles } from "../LinkingRuleActiveLearning.shared";
import { CandidateProperty, CandidatePropertyPair } from "../LinkingRuleActiveLearning.typings";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { EntityLink, EntityLinkPropertyPairValues } from "./LinkingRuleActiveLearningMain.typings";
import { IEntitySchema } from "../../../shared/rules/rule.typings";

export const LinkingRuleActiveLearningFeedbackComponent = () => {
    /** Contexts */
    const activeLearningFeedbackContext = React.useContext(LinkingRuleActiveLearningFeedbackContext);
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    /** The property pairs that will be compared during the active learning. */
    const [labelPropertyPairs, setlabelPropertyPairs] = React.useState<CandidatePropertyPair[]>([]);
    /** The values of the selected entity link. */
    const [valuesToDisplay, setValuesToDisplay] = React.useState<EntityLinkPropertyPairValues[] | undefined>();

    // Initially set the "label" properties, i.e. values of these properties are shown for entity links
    React.useEffect(() => {
        if (labelPropertyPairs.length === 0 && activeLearningContext.propertiesToCompare.length > 0) {
            setlabelPropertyPairs(activeLearningContext.propertiesToCompare.slice(0, 1));
        }
    }, [activeLearningContext.propertiesToCompare]);

    // Extract values for property pairs
    React.useEffect(() => {
        if (activeLearningContext.propertiesToCompare && activeLearningFeedbackContext.selectedLink) {
            const { source, target } = activeLearningFeedbackContext.selectedLink;
            const sourceValues = source.values;
            const sourceSchema = source.schema;
            const targetValues = target.values;
            const targetSchema = target.schema;
            const values: EntityLinkPropertyPairValues[] = [];
            activeLearningContext.propertiesToCompare.forEach((pair, idx) => {
                const sourcePropertyValues = fetchValueFor(pair.left.value, idx, sourceValues, sourceSchema);
                const targetPropertyValues = fetchValueFor(pair.right.value, idx, targetValues, targetSchema);
                values.push({ sourceValues: sourcePropertyValues, targetValues: targetPropertyValues });
            });
            setValuesToDisplay(values);
        }
    }, [activeLearningContext.propertiesToCompare, activeLearningFeedbackContext.selectedLink]);

    const fetchValueFor = (path: string, pathIdx: number, values: string[][], schema?: IEntitySchema): string[] => {
        const valueIdx = pathIdx;
        if (schema) {
            // TODO: Find idx via the schema
        }
        return values[valueIdx] ?? [];
    };

    return (
        <div>
            <Header disabledButtons={!activeLearningFeedbackContext.selectedLink} />
            <Spacing />
            {valuesToDisplay ? (
                <SelectedEntityLink
                    valuesToDisplay={valuesToDisplay}
                    propertyPairs={activeLearningContext.propertiesToCompare}
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
}

/** TODO: Clean up sub-components */
const Header = ({ disabledButtons }: HeaderProps) => {
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
                >
                    Confirm
                </Button>
            </ToolbarSection>
            <ToolbarSection>
                <Spacing vertical size={"large"} />
                <Button disabled={disabledButtons} title={"Uncertain"}>
                    Uncertain
                </Button>
                <Spacing vertical size={"large"} />
            </ToolbarSection>
            <ToolbarSection>
                <Button
                    title={"Decline that the shown entities are a valid link."}
                    disabled={disabledButtons}
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
    propertyPairs: CandidatePropertyPair[];
}

// TODO: Show labels as configured instead of first values
const SelectedEntityLink = ({ valuesToDisplay, propertyPairs }: SelectedEntityLinkProps) => {
    return (
        <Grid columns={3} fullWidth={true}>
            <EntityComparisonHeader
                sourceTitle={valuesToDisplay.length > 0 ? valuesToDisplay[0].sourceValues.join(", ") : ""}
                targetTitle={valuesToDisplay.length > 0 ? valuesToDisplay[0].targetValues.join(", ") : ""}
            />
            {(valuesToDisplay ?? []).map((selected, idx) => (
                <EntitiesPropertyPair propertyPair={propertyPairs[idx]} values={selected} />
            ))}
        </Grid>
    );
};

const EntitiesPropertyPair = ({
    propertyPair,
    values,
}: {
    propertyPair: CandidatePropertyPair;
    values: EntityLinkPropertyPairValues;
}) => {
    return (
        <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
            <EntityPropertyValues property={propertyPair.left} values={values.sourceValues} />
            <GridColumn style={columnStyles.centerColumnStyle}>
                <HoverToggler
                    baseElement={
                        <Toolbar style={{ height: "100%" }}>
                            <ToolbarSection canGrow={true}>
                                <ArrowLeft />
                            </ToolbarSection>
                            <ToolbarSection>
                                <Tag>
                                    {propertyPair.left.type != null &&
                                    propertyPair.left.type === propertyPair.right.type
                                        ? propertyPair.left.type
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
                                {/*<IconButton*/}
                                {/*    name={"item-remove"}*/}
                                {/*    disruptive*/}
                                {/*    onClick={() => removePair(pair.pairId)}*/}
                                {/*/>*/}
                            </ToolbarSection>
                            <ToolbarSection canGrow={true} />
                        </Toolbar>
                    }
                />
            </GridColumn>
            <EntityPropertyValues property={propertyPair.right} values={values.targetValues} />
        </GridRow>
    );
};

const EntityPropertyValues = ({ property, values }: { values: string[]; property: CandidateProperty }) => {
    const propertyLabel = property.label ? property.label : property.value;
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
