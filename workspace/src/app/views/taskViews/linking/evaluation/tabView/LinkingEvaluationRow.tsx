import React from "react";
import { TreeNodeInfo } from "@eccenca/gui-elements";
import { EvaluationLinkInputValue, HoveredValuedType, LinkingEvaluationResult, ReferenceLinkType } from "./typings";
import { useTranslation } from "react-i18next";
import { getParentNodes } from "./LinkingEvaluationViewUtils";
import { IAggregationOperator, ISimilarityOperator } from "../../linking.types";
import { ComparisonDataCell, ComparisonDataContainer } from "../../activeLearning/components/ComparisionData";
import { PropertyBox } from "../../activeLearning/components/PropertyBox";
import { ActiveLearningValueExamples } from "../../activeLearning/shared/ActiveLearningValueExamples";
import TableTree from "../../../shared/evaluations/TableTreeView";
import { IPluginDetails } from "@ducks/common/typings";
import { EvaluationResultType } from "../LinkingRuleEvaluation";
import { ValidIconName } from "@eccenca/gui-elements/src/components/Icon/canonicalIconNames";

import {
    ConfidenceValue,
    Divider,
    Highlighter,
    Icon,
    IconButton,
    InteractionGate,
    OverflowText,
    Spacing,
    Table,
    TableBody,
    TableCell,
    TableExpandedRow,
    TableExpandRow,
    TableRow,
    Tag,
} from "@eccenca/gui-elements";
import { OperatorLabel } from "../../../../../views/taskViews/shared/evaluations/OperatorLabel";
import { LinkType } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";
import { Intent as BlueprintIntent } from "@blueprintjs/core";

interface ExpandedEvaluationRowProps {
    rowIdx: number;
    colSpan: number;
    inputValues?: EvaluationLinkInputValue;
    rowIsExpandedByParent: boolean;
    linkingEvaluationResult?: LinkingEvaluationResult;
    handleReferenceLinkTypeUpdate: (
        currentLinkType: ReferenceLinkType,
        linkType: ReferenceLinkType,
        source: string,
        target: string,
        index: number,
    ) => Promise<boolean>;
    searchQuery: string;
    linkRuleOperatorTree?: ISimilarityOperator;
    operatorTreeExpandedByDefault: boolean;
    inputValuesExpandedByDefault: boolean;
    operatorPlugins: Array<IPluginDetails>;
    evaluationMap?: Map<string, EvaluationResultType[number]>;
    /** If the row is expanded because of a search match. Only the row values should be shown in that case. */
    expandedBySearch: boolean;
}

const operatorInputMapping = {
    transformInput: "Transform",
    pathInput: "Input",
};

const linkStateButtons: {
    icon: ValidIconName;
    linkType: LinkType;
    intent?: BlueprintIntent | "accent";
    tooltip: string;
}[] = [
    { icon: "state-confirmed", intent: "success", linkType: "positive", tooltip: "Confirm" },
    { icon: "item-question", linkType: "unlabeled", tooltip: "Uncertain" },
    { icon: "state-declined", intent: "danger", linkType: "negative", tooltip: "Decline" },
];

/** A single row (link) in the linking evaluation view. */
export const LinkingEvaluationRow = React.memo(
    ({
        rowIdx,
        colSpan,
        inputValues,
        rowIsExpandedByParent,
        linkingEvaluationResult,
        handleReferenceLinkTypeUpdate,
        searchQuery,
        linkRuleOperatorTree,
        operatorTreeExpandedByDefault,
        inputValuesExpandedByDefault,
        operatorPlugins,
        evaluationMap,
        expandedBySearch,
    }: ExpandedEvaluationRowProps) => {
        const [treeNodes, setTreeNodes] = React.useState<TreeNodeInfo | undefined>(undefined);
        const [valueToHighlight, setValueToHighlight] = React.useState<HoveredValuedType | undefined>(undefined);
        const [inputValueTableExpanded, setInputValueTableExpanded] =
            React.useState<boolean>(inputValuesExpandedByDefault);
        const [operatorTreeExpansion, setOperatorTreeExpansion] = React.useState<{
            expanded: boolean;
            precinct: boolean;
        }>({ expanded: operatorTreeExpandedByDefault, precinct: false });
        const [nodeParentHighlightedIds, setNodeParentHighlightedIds] = React.useState<Map<number, string[]>>(
            new Map(),
        );
        const [updateOperationPending, setUpdateOperationPending] = React.useState(false);
        // Keeps track of the current link type when it gets updated via the state buttons
        const [currentLinkType, setCurrentLinkType] = React.useState<ReferenceLinkType>(
            linkingEvaluationResult?.decision ?? "unlabeled",
        );
        const [rowIsExpanded, setRowIsExpanded] = React.useState<boolean>(rowIsExpandedByParent);
        const [t] = useTranslation();

        const handleInputTableExpansion = React.useCallback(() => {
            setInputValueTableExpanded((prev) => !prev);
        }, []);

        React.useEffect(() => {
            setRowIsExpanded(rowIsExpandedByParent || expandedBySearch);
        }, [rowIsExpandedByParent, expandedBySearch]);

        React.useEffect(() => {
            if (rowIsExpanded) {
                if (inputValuesExpandedByDefault !== inputValueTableExpanded) {
                    setInputValueTableExpanded(inputValuesExpandedByDefault);
                }
                const treeExpanded = operatorTreeExpandedByDefault && (!expandedBySearch || rowIsExpandedByParent);
                if (treeExpanded !== operatorTreeExpansion.expanded) {
                    setOperatorTreeExpansion({ ...operatorTreeExpansion, expanded: treeExpanded });
                }
            }
        }, [rowIsExpanded, expandedBySearch, rowIsExpandedByParent]);

        const toggleRuleTreeExpand = React.useCallback(() => {
            setOperatorTreeExpansion((old) => ({
                ...old,
                expanded: !old.expanded,
            }));
        }, []);

        const getOperatorConfidence = React.useCallback(
            (id: string) => {
                if (!evaluationMap) {
                    return <></>;
                } else {
                    return evaluationMap.get(id)?.value.map((val, i) => (
                        <ConfidenceValue
                            key={i}
                            value={val.includes("Score") ? Number(val.replace("Score: ", "")) : Number(val)}
                            spaceUsage="minimal"
                            tagProps={{
                                // TODO: get color from CSS config
                                backgroundColor: nodeParentHighlightedIds.get(rowIdx)?.includes(id)
                                    ? "#0097a7"
                                    : undefined,
                            }}
                        />
                    ));
                }
            },
            [evaluationMap, nodeParentHighlightedIds],
        );

        // Returns an icon element that warns the user that the entity has no values at all
        const emptyEntityWarning = (values?: Record<string, string[]>): JSX.Element | null => {
            if (!values) {
                return null;
            }
            const entries = Object.entries(values);
            if (entries.length === 0) {
                // If there is no input path at all, do not highlight empty entity
                return null;
            }
            const allEmpty = entries.every(
                ([_prop, propertyValues]) => !Array.isArray(propertyValues) || propertyValues.length === 0,
            );
            return allEmpty ? (
                <>
                    <Spacing vertical={true} size={"small"} />
                    <Icon
                        intent={"neutral"}
                        name={"state-info"}
                        tooltipText={t("ReferenceLinks.warnings.entityHasNoValues")}
                    />
                </>
            ) : null;
        };

        const updateTreeNode = React.useCallback(() => {
            const operatorNode = linkRuleOperatorTree;
            if (!operatorNode) return;
            const treeInfo: TreeNodeInfo = {
                id: operatorNode.id,
                isExpanded: operatorTreeExpansion.expanded,
                hasCaret: false,
                label: (
                    <OperatorLabel
                        tagPluginType={operatorNode.type}
                        operator={operatorNode}
                        operatorPlugins={operatorPlugins}
                    >
                        {getOperatorConfidence(operatorNode.id)}
                    </OperatorLabel>
                ),
                childNodes: [],
            };

            const getSubTree = (node: any, parentTree?: TreeNodeInfo) => {
                //Possibly comparison operators
                const inputPathCategory = {
                    sourceInput: "Source path",
                    targetInput: "Target path",
                };

                if (node?.inputs?.length) {
                    node.inputs.forEach((nodeInput) => {
                        getSubTree(nodeInput, {
                            id: nodeInput.id,
                            isExpanded: true,
                            hasCaret: false,
                            label: (
                                <OperatorLabel
                                    tagPluginType={nodeInput.type}
                                    operator={nodeInput}
                                    operatorPlugins={operatorPlugins}
                                >
                                    {getOperatorConfidence(nodeInput.id)}
                                </OperatorLabel>
                            ),
                            childNodes: [],
                        });
                    });
                } else {
                    ["sourceInput", "targetInput"].forEach((inputPath) => {
                        const isSourceEntity = inputPath === "sourceInput";
                        //is comparison operator
                        let inputNode: TreeNodeInfo = {
                            id: node[inputPath].id,
                            isExpanded: true,
                            hasCaret: false,
                            label: (
                                <OperatorLabel
                                    tagPluginType={
                                        node[inputPath].type === "pathInput"
                                            ? inputPathCategory[inputPath]
                                            : operatorInputMapping[node[inputPath].type]
                                    }
                                    operator={node[inputPath]}
                                    operatorPlugins={operatorPlugins}
                                >
                                    {getLinkValues(node[inputPath].id, rowIdx, treeInfo, {
                                        path: node[inputPath].path ?? "",
                                        isSourceEntity,
                                    })}
                                </OperatorLabel>
                            ),
                            childNodes: [],
                        };

                        if (node[inputPath].inputs?.length) {
                            node[inputPath].inputs.forEach((i) => {
                                buildInputTree(
                                    i,
                                    inputNode,
                                    rowIdx,
                                    inputPathCategory[inputPath],
                                    treeInfo,
                                    isSourceEntity,
                                );
                            });
                        }

                        if (parentTree) {
                            parentTree.childNodes!.push(inputNode);
                        } else {
                            treeInfo.childNodes!.push(inputNode);
                        }
                    });
                }
                parentTree && treeInfo.childNodes!.push(parentTree);
            };

            operatorNode.type === "Aggregation"
                ? (operatorNode as IAggregationOperator).inputs.forEach((i: any) => {
                      getSubTree(i, {
                          id: i.id,
                          isExpanded: true,
                          hasCaret: false,
                          label: (
                              <OperatorLabel tagPluginType={i.type} operator={i} operatorPlugins={operatorPlugins}>
                                  {getOperatorConfidence(i.id)}
                              </OperatorLabel>
                          ),
                          childNodes: [],
                      });
                  })
                : getSubTree(operatorNode);

            return treeInfo;
        }, [operatorPlugins.length, valueToHighlight, nodeParentHighlightedIds, operatorTreeExpansion]);

        const buildInputTree = (
            input: any,
            tree: TreeNodeInfo,
            index: number,
            tagInputTag: "Source path" | "Target path",
            parentTree: TreeNodeInfo,
            isSourceEntity = false,
        ): TreeNodeInfo => {
            if (!input.inputs?.length) {
                const newChild = {
                    id: input.id,
                    hasCaret: false,
                    isExpanded: true,
                    label: (
                        <OperatorLabel tagPluginType={tagInputTag} operator={input} operatorPlugins={operatorPlugins}>
                            {getLinkValues(input.id, index, parentTree, {
                                path: input.path ?? "",
                                isSourceEntity,
                            })}
                        </OperatorLabel>
                    ),
                };
                tree.childNodes = [...(tree?.childNodes ?? []), newChild];
                return tree;
            }

            return input.inputs.reduce((acc, i) => {
                const newChildTree = {
                    id: input.id,
                    hasCaret: false,
                    isExpanded: true,
                    label: (
                        <OperatorLabel
                            tagPluginType={operatorInputMapping[input.type]}
                            operator={input}
                            operatorPlugins={operatorPlugins}
                        >
                            {getLinkValues(input.id, index, parentTree, {
                                path: input.path ?? "",
                                isSourceEntity,
                            })}
                        </OperatorLabel>
                    ),
                };
                tree.childNodes = [...(tree?.childNodes ?? []), newChildTree];

                acc = buildInputTree(i, newChildTree, index, tagInputTag, parentTree, isSourceEntity);
                return acc;
            }, {} as TreeNodeInfo);
        };

        const getLinkValues = React.useCallback(
            (id: string, index: number, tree: TreeNodeInfo, nodeData: Omit<HoveredValuedType, "value">) => {
                const cutAfter = 14;
                if (evaluationMap && nodeData) {
                    const currentHighlightedValue = valueToHighlight;
                    //if path === path from state and is sourceEntity matches and value matches
                    const isHighlightMatch = (val: string) =>
                        nodeData &&
                        currentHighlightedValue &&
                        currentHighlightedValue.value === val &&
                        Object.entries(nodeData).reduce((acc, [key, val]) => {
                            acc = acc && currentHighlightedValue[key] === val;
                            return acc;
                        }, true);

                    const otherCount =
                        (evaluationMap.get(id)?.value || []).length > cutAfter ? (
                            <Tag className="diapp-linking-evaluation__cutinfo" round intent="info">
                                +{(evaluationMap.get(id)?.value || []).length - cutAfter}
                            </Tag>
                        ) : null;
                    let exampleValues: JSX.Element[] = [];

                    if (!evaluationMap.get(id)?.value.length) {
                        exampleValues = [
                            <Tag
                                htmlTitle={t("common.messages.noValuesAvailable")}
                                round={true}
                                intent={"neutral"}
                                emphasis={"weak"}
                            >
                                N/A
                            </Tag>,
                        ];
                    }

                    exampleValues =
                        evaluationMap
                            .get(id)
                            ?.value.slice(0, cutAfter)
                            .map((val, i) => (
                                <Tag
                                    key={val + i}
                                    round
                                    emphasis="stronger"
                                    interactive
                                    backgroundColor={
                                        isHighlightMatch(val)
                                            ? "#746a85" // TODO: get color from CSS config
                                            : nodeParentHighlightedIds.get(index)?.includes(id)
                                              ? "#0097a7" // TODO: get color from CSS config
                                              : undefined
                                    }
                                    onMouseEnter={() => {
                                        handleValueHover({
                                            value: val,
                                            ...nodeData,
                                        });
                                        handleParentNodeHighlights(tree, id, index);
                                    }}
                                    onMouseLeave={() => {
                                        handleValueHover({ value: "", path: "", isSourceEntity: false });
                                        handleParentNodeHighlights(tree, id, index, true);
                                    }}
                                >
                                    {searchQuery ? <Highlighter label={val} searchValue={searchQuery} /> : val}
                                </Tag>
                            )) ?? [];
                    return [exampleValues, [otherCount]];
                }
            },
            [evaluationMap, valueToHighlight, nodeParentHighlightedIds, searchQuery],
        );

        const handleParentNodeHighlights = React.useCallback((tree, id: string, index: number, reset = false) => {
            setNodeParentHighlightedIds((prev) => new Map([[index, reset ? [] : getParentNodes(tree, id)]]));
        }, []);

        const handleValueHover = React.useCallback((hoveredTagProps: HoveredValuedType) => {
            setValueToHighlight(hoveredTagProps);
        }, []);

        React.useEffect(() => {
            if (operatorPlugins.length) {
                setTreeNodes(updateTreeNode());
            }
        }, [valueToHighlight, nodeParentHighlightedIds, operatorPlugins.length, operatorTreeExpansion]);

        const highlightSourceTableValue = React.useCallback(
            (currentPath: string, isSourceEntity: boolean): Set<string> | undefined =>
                valueToHighlight &&
                valueToHighlight.isSourceEntity === isSourceEntity &&
                currentPath === valueToHighlight.path
                    ? new Set([valueToHighlight.value])
                    : undefined,
            [valueToHighlight],
        );
        const onLinkStateUpdate = async (linkType: ReferenceLinkType) => {
            if (linkingEvaluationResult) {
                setUpdateOperationPending(true);
                const success = await handleReferenceLinkTypeUpdate(
                    currentLinkType,
                    linkType,
                    linkingEvaluationResult.source,
                    linkingEvaluationResult.target,
                    rowIdx,
                );
                if (success) {
                    setCurrentLinkType(linkType);
                }
                setUpdateOperationPending(false);
            }
        };
        const onExpandRow = React.useCallback(() => setRowIsExpanded((e) => !e), []);

        //score does not match decision
        const mismatchExists: boolean =
            linkingEvaluationResult?.decision != null &&
            linkingEvaluationResult?.confidence != null &&
            linkingEvaluationResult.decision !== "unlabeled" &&
            (linkingEvaluationResult.confidence >= 0 ? "positive" : "negative") !== linkingEvaluationResult?.decision;
        return (
            <React.Fragment key={rowIdx}>
                {linkingEvaluationResult && (
                    <TableExpandRow
                        key={rowIdx}
                        isExpanded={rowIsExpanded}
                        onExpand={onExpandRow}
                        togglerText={
                            rowIsExpanded
                                ? t("linkingEvaluationTabView.table.collapseRow")
                                : t("linkingEvaluationTabView.table.expandRow")
                        }
                        className="diapp-linking-evaluation__row-item"
                        useZebraStyle={rowIdx % 2 === 1}
                    >
                        <TableCell alignVertical="middle">
                            {mismatchExists ? (
                                <Icon
                                    intent="warning"
                                    name="state-warning"
                                    data-test-id="decision-mismatch-warning"
                                    tooltipText="decision mismatch"
                                />
                            ) : null}
                        </TableCell>
                        <TableCell key={"sourceEntity"} alignVertical="middle">
                            <Highlighter label={linkingEvaluationResult.source} searchValue={searchQuery} />
                            {emptyEntityWarning(inputValues?.source)}
                        </TableCell>
                        <TableCell key={"targetEntity"} alignVertical="middle">
                            <Highlighter label={linkingEvaluationResult.target} searchValue={searchQuery} />
                            {emptyEntityWarning(inputValues?.target)}
                        </TableCell>
                        <TableCell key="confidence" alignVertical="middle">
                            <ConfidenceValue value={linkingEvaluationResult.confidence} />
                        </TableCell>
                        <TableCell key="linkstate">
                            <div style={{ whiteSpace: "nowrap" }}>
                                <InteractionGate
                                    showSpinner={updateOperationPending}
                                    spinnerProps={{ size: "tiny", position: "inline", delay: 500 }}
                                >
                                    {linkStateButtons.map(({ linkType, icon, ...otherProps }, btnIndex) => (
                                        <React.Fragment key={icon}>
                                            <IconButton
                                                data-test-id={`link-state-button-${linkType}`}
                                                name={icon}
                                                onClick={() => onLinkStateUpdate(linkType)}
                                                {...otherProps}
                                                outlined={currentLinkType !== linkType}
                                                minimal={false}
                                            />
                                            {btnIndex !== linkStateButtons.length - 1 && (
                                                <Spacing vertical size="tiny" />
                                            )}
                                        </React.Fragment>
                                    ))}
                                </InteractionGate>
                            </div>
                        </TableCell>
                    </TableExpandRow>
                )}
                {!!inputValues && rowIsExpanded && (
                    <TableExpandedRow colSpan={colSpan} className="linking-table__expanded-row-container">
                        <Table
                            size="small"
                            columnWidths={["30px", "40%", "40%", "7rem", "9rem"]}
                            hasDivider={false}
                            colorless
                        >
                            <TableBody>
                                <TableRow style={{ height: "1px" }}>
                                    <TableCell style={{ padding: "0" }}></TableCell>
                                    <TableCell colSpan={4} style={{ paddingTop: "0" }}>
                                        <Divider width="half" alignment="center" />
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableCell style={{ padding: "0" }}>
                                        <IconButton
                                            data-test-id="input-table-expand-btn"
                                            id={`input-table-${inputValueTableExpanded ? "expanded" : "collapsed"}`}
                                            onClick={handleInputTableExpansion}
                                            name={!inputValueTableExpanded ? "toggler-caretright" : "toggler-caretdown"}
                                        />
                                    </TableCell>
                                    <TableCell alignVertical="middle">
                                        {!inputValueTableExpanded && (
                                            <OverflowText>
                                                {t("linkingEvaluationTabView.table.infoCollapsedInputValue")}
                                            </OverflowText>
                                        )}
                                        {!!inputValueTableExpanded && (
                                            <ComparisonDataContainer>
                                                {Object.entries(inputValues.source).map(([key, values]) => (
                                                    <ComparisonDataCell
                                                        key={key}
                                                        fullWidth
                                                        className={!inputValueTableExpanded ? "shrink" : ""}
                                                    >
                                                        <PropertyBox
                                                            propertyName={key}
                                                            exampleValues={
                                                                <ActiveLearningValueExamples
                                                                    interactive
                                                                    valuesToHighlight={highlightSourceTableValue(
                                                                        key,
                                                                        true,
                                                                    )}
                                                                    onHover={(val) =>
                                                                        handleValueHover({
                                                                            path: key,
                                                                            isSourceEntity: true,
                                                                            value: val,
                                                                        })
                                                                    }
                                                                    exampleValues={values ?? []}
                                                                    searchQuery={searchQuery}
                                                                />
                                                            }
                                                        />
                                                    </ComparisonDataCell>
                                                ))}
                                            </ComparisonDataContainer>
                                        )}
                                    </TableCell>
                                    <TableCell>
                                        {!!inputValueTableExpanded && (
                                            <ComparisonDataContainer>
                                                {Object.entries(inputValues.target).map(([key, values]) => (
                                                    <ComparisonDataCell
                                                        key={key}
                                                        fullWidth
                                                        className={!inputValueTableExpanded ? "shrink" : ""}
                                                    >
                                                        <PropertyBox
                                                            propertyName={key}
                                                            exampleValues={
                                                                <ActiveLearningValueExamples
                                                                    interactive
                                                                    valuesToHighlight={highlightSourceTableValue(
                                                                        key,
                                                                        false,
                                                                    )}
                                                                    onHover={(val) =>
                                                                        handleValueHover({
                                                                            path: key,
                                                                            isSourceEntity: false,
                                                                            value: val,
                                                                        })
                                                                    }
                                                                    exampleValues={values ?? []}
                                                                    searchQuery={searchQuery}
                                                                />
                                                            }
                                                        />
                                                    </ComparisonDataCell>
                                                ))}
                                            </ComparisonDataContainer>
                                        )}
                                    </TableCell>
                                </TableRow>
                            </TableBody>
                        </Table>
                        <TableTree
                            columnWidths={["30px", "40%", "40%", "7rem", "9rem"]}
                            treeIsExpanded={operatorTreeExpansion.expanded}
                            nodes={treeNodes ? [treeNodes] : []}
                            toggleTableExpansion={toggleRuleTreeExpand}
                        />
                    </TableExpandedRow>
                )}
            </React.Fragment>
        );
    },
);
