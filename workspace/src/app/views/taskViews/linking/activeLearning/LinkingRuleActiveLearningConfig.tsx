import React, { CSSProperties } from "react";

import {
    AutoSuggestion,
    Button,
    Grid,
    GridColumn,
    GridRow,
    HoverToggler,
    Icon,
    IconButton,
    Notification,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Tag,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import { CandidateProperty, CandidatePropertyPair } from "./LinkingRuleActiveLearning.typings";
import "./LinkingRuleActiveLeraningConfig.scss";
import { LinkingRuleActiveLearningContext } from "./contexts/LinkingRuleActiveLearningContext";
import { partialAutoCompleteLinkingInputPaths } from "../LinkingRuleEditor.requests";
import { IPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { checkValuePathValidity } from "../../../pages/MappingEditor/HierarchicalMapping/store";

interface LinkingRuleActiveLearningConfigProps {
    projectId: string;
    linkingTaskId: string;
}

// TODO: remove when not needed anymore
const mockPairs: CandidatePropertyPair[] = [
    {
        pairId: "s1",
        left: {
            value: "propA",
            label: "Suggested property",
            exampleValues: ["Value 1", "Value 2"],
            type: "date",
        },
        right: {
            value: "urn:prop:propB",
            label: "Other suggested property",
            exampleValues: [
                "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongValue",
                "Next Value",
            ],
            type: "date",
        },
    },
    {
        pairId: "s2",
        left: {
            value: "prop X",
            exampleValues: ["Value 1", "Value 2"],
            type: "string",
        },
        right: {
            value: "urn:prop:propB2",
            label: "urn:prop:propB",
            exampleValues: ["val", "Next Value"],
            type: "number",
        },
    },
];

let randomId = 0;
const nextId = () => {
    randomId += 1;
    return randomId;
};

/** Allows to configure the property pairs for the active learning. */
export const LinkingRuleActiveLearningConfig = ({ projectId, linkingTaskId }: LinkingRuleActiveLearningConfigProps) => {
    const { registerError } = useErrorHandler();
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const [suggestions, setSuggestions] = React.useState<CandidatePropertyPair[]>(mockPairs);
    const manualSourcePath = React.useRef<CandidateProperty | undefined>(undefined);
    const manualTargetPath = React.useRef<CandidateProperty | undefined>(undefined);
    const [hasValidPath, setHasValidPath] = React.useState(false);
    const [t] = useTranslation();

    const removePair = (pairId: string) => {
        activeLearningContext.setPropertiesToCompare(
            activeLearningContext.propertiesToCompare.filter((pair) => pair.pairId !== pairId)
        );
    };

    const mainColumnStyle: CSSProperties = {
        // TODO: style via sass
        width: "40%",
        maxWidth: "40%",
        textAlign: "center",
        padding: "5px",
        borderWidth: "thin",
        borderStyle: "solid",
        borderColor: "lightgray",
    };
    const headerColumnStyle: CSSProperties = {
        ...mainColumnStyle,
        backgroundColor: "blue",
        color: "white",
    };
    const centerColumnStyle: CSSProperties = {
        display: "flex",
        columnWidth: "20%",
        maxWidth: "20%",
        padding: "5px",
        alignItems: "center",
        justifyContent: "center",
    };
    const fetchAutoCompletionResult =
        (isTarget: boolean) =>
        async (inputString: string, cursorPosition: number): Promise<IPartialAutoCompleteResult | undefined> => {
            try {
                const result = await partialAutoCompleteLinkingInputPaths(
                    projectId,
                    linkingTaskId,
                    isTarget ? "target" : "source",
                    inputString,
                    cursorPosition,
                    200
                );
                return result.data;
            } catch (err) {
                registerError(
                    "ActiveLearning.fetchAutoCompletionResult",
                    t("ActiveLearning.config.errors.fetchAutoCompletionResult"),
                    err
                );
            }
        };

    const changeManualSourcePath = (value: string) => {
        // TODO: How to fetch example values and other meta data?
        manualSourcePath.current = value ? { value: value, exampleValues: [] } : undefined;
        checkPathValidity();
    };

    const changeManualTargetPath = (value: string) => {
        // TODO: How to fetch example values and other meta data?
        manualTargetPath.current = value ? { value: value, exampleValues: [] } : undefined;
        checkPathValidity();
    };

    const checkPathValidity = () => {
        // TODO: Add path validation (syntax) check
        if (manualSourcePath.current && manualTargetPath.current) {
            setHasValidPath(true);
        } else {
            setHasValidPath(false);
        }
    };

    const addManuallyChosenPair = () => {
        if (manualSourcePath.current && manualTargetPath.current) {
            activeLearningContext.setPropertiesToCompare([
                ...activeLearningContext.propertiesToCompare,
                {
                    pairId: `manually chose: ${nextId()}`,
                    left: manualSourcePath.current,
                    right: manualTargetPath.current,
                },
            ]);
            changeManualSourcePath("");
            changeManualTargetPath("");
        }
    };

    const addSuggestion = (pairId: string) => {
        const pairToAdd = suggestions.find((s) => s.pairId === pairId);
        if (pairToAdd) {
            setSuggestions(suggestions.filter((s) => s.pairId !== pairId));
            activeLearningContext.setPropertiesToCompare([...activeLearningContext.propertiesToCompare, pairToAdd]);
        }
    };
    const ArrowRight = () => <div className={"arrow-right"} />;
    const ArrowLeft = () => <div className={"arrow-left"} />;
    const ConfigHeader = () => {
        return (
            <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
                <GridColumn style={headerColumnStyle}>row1</GridColumn>
                <GridColumn style={centerColumnStyle}>
                    <ArrowLeft />
                    <Tag>owl:sameAs</Tag>
                    <ArrowRight />
                </GridColumn>
                <GridColumn style={headerColumnStyle}>row3</GridColumn>
            </GridRow>
        );
    };

    const SelectedProperty = ({ property }: { property: CandidateProperty }) => {
        const showLabel: boolean = !!property.label && property.label.toLowerCase() !== property.value.toLowerCase();
        const exampleTitle = property.exampleValues.join(" | ");
        return (
            <GridColumn style={mainColumnStyle}>
                <OverviewItem>
                    <OverviewItemDescription>
                        {showLabel ? <OverviewItemLine>{property.label}</OverviewItemLine> : null}
                        <OverviewItemLine small={showLabel}>{property.value}</OverviewItemLine>
                        {property.exampleValues.length > 0 ? (
                            <OverviewItemLine small={showLabel} title={exampleTitle}>
                                {property.exampleValues.map((example) => {
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

    const SelectedPropertyPair = ({ pair }: { pair: CandidatePropertyPair }) => {
        return (
            <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
                <SelectedProperty property={pair.left} />
                <GridColumn style={centerColumnStyle}>
                    <HoverToggler
                        baseElement={
                            <Toolbar style={{ height: "100%" }}>
                                <ToolbarSection canGrow={true}>
                                    <ArrowLeft />
                                </ToolbarSection>
                                <ToolbarSection>
                                    <Tag>
                                        {pair.left.type != null && pair.left.type === pair.right.type
                                            ? pair.left.type
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
                                        name={"item-remove"}
                                        disruptive
                                        onClick={() => removePair(pair.pairId)}
                                    />
                                </ToolbarSection>
                                <ToolbarSection canGrow={true} />
                            </Toolbar>
                        }
                    />
                </GridColumn>
                <SelectedProperty property={pair.right} />
            </GridRow>
        );
    };

    const SelectedPropertiesWidget = () => {
        return (
            <Grid columns={3} fullWidth={true}>
                <ConfigHeader />
                {(activeLearningContext.propertiesToCompare ?? []).map((selected) => (
                    <SelectedPropertyPair pair={selected} />
                ))}
            </Grid>
        );
    };

    const PathAutoCompletion = ({ isTarget }: { isTarget: boolean }) => {
        return (
            <GridColumn style={{ ...mainColumnStyle, textAlign: "left" }}>
                <AutoSuggestion
                    leftElement={
                        <Icon name={"operation-search"} tooltipText={"Allows to construct complex input paths."} />
                    }
                    initialValue={
                        isTarget ? manualTargetPath.current?.value ?? "" : manualSourcePath.current?.value ?? ""
                    }
                    onChange={(value) => {
                        if (isTarget) {
                            changeManualTargetPath(value);
                        } else {
                            changeManualSourcePath(value);
                        }
                    }}
                    fetchSuggestions={fetchAutoCompletionResult(isTarget)}
                    placeholder={"Enter an input path"}
                    checkInput={checkValuePathValidity}
                />
            </GridColumn>
        );
    };

    const DashedLine = () => {
        return (
            <svg width="100%" height="10px" viewBox="0 0 100 10" preserveAspectRatio="none">
                <g fill="none" stroke="black">
                    <line x1="0" y1="5" x2="100" y2="5" strokeDasharray={"2 2"} />
                </g>
            </svg>
        );
    };

    const ManualPropertyPathSelection = () => {
        return (
            <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
                <PathAutoCompletion isTarget={false} />
                <GridColumn style={centerColumnStyle}>
                    <Toolbar style={{ height: "100%" }}>
                        <ToolbarSection canGrow={true}>
                            <DashedLine />
                        </ToolbarSection>
                        <ToolbarSection>
                            <IconButton
                                name={"item-add-artefact"}
                                disabled={!hasValidPath}
                                title={hasValidPath ? "Add" : "At least one paths is not valid"}
                                onClick={addManuallyChosenPair}
                            />
                        </ToolbarSection>
                        <ToolbarSection canGrow={true}>
                            <DashedLine />
                        </ToolbarSection>
                    </Toolbar>
                </GridColumn>
                <PathAutoCompletion isTarget={true} />
            </GridRow>
        );
    };

    const SuggestionWidget = () => {
        return (
            <Grid columns={3} fullWidth={true}>
                {suggestions.map((suggestion) => (
                    <SuggestedPathSelection pair={suggestion} />
                ))}
            </Grid>
        );
    };

    const SuggestedPathSelection = ({ pair }: { pair: CandidatePropertyPair }) => {
        return (
            <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
                <SelectedProperty property={pair.left} />
                <GridColumn style={centerColumnStyle}>
                    <Toolbar style={{ height: "100%" }}>
                        <ToolbarSection canGrow={true}>
                            <DashedLine />
                        </ToolbarSection>
                        <ToolbarSection>
                            <IconButton name={"item-add-artefact"} onClick={() => addSuggestion(pair.pairId)} />
                        </ToolbarSection>
                        <ToolbarSection canGrow={true}>
                            <DashedLine />
                        </ToolbarSection>
                    </Toolbar>
                </GridColumn>
                <SelectedProperty property={pair.right} />
            </GridRow>
        );
    };

    const InfoWidget = () => {
        return <Notification message={"Choose properties to compare."} iconName={"item-info"} neutral={true} />;
    };

    // TODO: Navigate to next step on clicking the button. i18n
    const Title = () => {
        return (
            <Toolbar>
                <ToolbarSection>
                    Configuration: Define properties to compare between entities of each data source.
                </ToolbarSection>
                <ToolbarSection canGrow>
                    <Spacing vertical />
                </ToolbarSection>
                <ToolbarSection>
                    <Button
                        title={"Start learning"}
                        affirmative={true}
                        disabled={activeLearningContext.propertiesToCompare.length === 0}
                    >
                        Start learning
                    </Button>
                </ToolbarSection>
            </Toolbar>
        );
    };

    const SuggestionSelectionSubHeader = () => {
        return (
            <Notification
                neutral={true}
                message={`Found ${suggestions.length} comparison suggestions you might want to add. Click button to add.`}
                iconName={null}
            />
        );
    };

    const PathSelectionSubHeader = () => {
        return <Notification neutral={true} message={"Specify property paths to be compared."} iconName={null} />;
    };

    return (
        <div>
            <Title />
            <Spacing hasDivider={true} />
            <InfoWidget />
            <Spacing size={"small"} />
            <SelectedPropertiesWidget />
            {suggestions.length > 0 ? (
                <>
                    <Spacing />
                    <SuggestionSelectionSubHeader />
                    <Spacing />
                    <SuggestionWidget />
                </>
            ) : null}
            <Spacing />
            <PathSelectionSubHeader />
            <Spacing />
            <ManualPropertyPathSelection />
        </div>
    );
};
