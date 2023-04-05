import React from "react";
import { IPluginDetails } from "@ducks/common/typings";
import {
    Spacing,
    Table,
    TableBody,
    TableCell,
    TableExpandedRow,
    TableExpandRow,
    TableRow,
    Tree,
    TreeNodeInfo,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { newNode, NodeTagValues } from "./TransformEvaluationTabViewUtils";
import {
    EvaluatedComplexRule,
    EvaluatedEntity,
    EvaluatedEntityOperator,
    EvaluatedRuleOperator,
    EvaluatedURIRule,
} from "./typing";

interface TransformEvaluationTabRowProps {
    rowExpandedByParent: boolean;
    rowItem: { uri: string; id: string };
    entity: EvaluatedEntity;
    colSpan: number;
    operatorPlugins: Array<IPluginDetails>;
    rules: Array<EvaluatedURIRule | EvaluatedComplexRule>;
}

const TransformEvaluationTabRow: React.FC<TransformEvaluationTabRowProps> = React.memo(
    ({ rowItem, colSpan, rowExpandedByParent, entity, rules, operatorPlugins }) => {
        const [rowIsExpanded, setRowIsExpanded] = React.useState<boolean>(rowExpandedByParent);
        const [treeExpansionMap, setTreeExpansionMap] = React.useState<Map<string, boolean>>(new Map());
        const [treeNodes, setTreeNodes] = React.useState<TreeNodeInfo[]>([]);
        const [t] = useTranslation();

        React.useEffect(() => {
            setRowIsExpanded(rowExpandedByParent);
        }, [rowExpandedByParent]);

        //update tree
        React.useEffect(() => {
            if (rowIsExpanded) {
                buildTree();
            }
        }, [treeExpansionMap, rowIsExpanded]);

        const handleRowExpansion = React.useCallback(() => setRowIsExpanded((e) => !e), []);

        const handleNodeExpand = React.useCallback((node) => {
            setTreeExpansionMap((prev) => new Map([...prev, [node.id, true]]));
        }, []);

        const handleNodeCollapse = React.useCallback((node) => {
            setTreeExpansionMap((prev) => new Map([...prev, [node.id, false]]));
        }, []);

        const buildTree = React.useCallback(() => {
            setTreeNodes(
                entity.values.map((e, i) => {
                    const matchingRuleType = (
                        rules[i].operator
                            ? rules[i]
                            : {
                                  ...rules[i].rules?.propertyRules[0],
                                  mappingTarget: (rules[i] as EvaluatedComplexRule)?.mappingTarget,
                              }
                    ) as Omit<EvaluatedURIRule, "rules">;

                    let treeNodeInfo = {
                        hasCaret: true,
                        id: matchingRuleType.id,
                        isExpanded: treeExpansionMap.get(matchingRuleType.id),
                        label: "",
                        childNodes: [],
                        nodeData: {
                            label: (matchingRuleType as EvaluatedComplexRule)?.mappingTarget?.uri || "URI",
                            root: true,
                        },
                    } as TreeNodeInfo<Partial<{ root: boolean; label: string }>>;

                    const generateTree = (
                        rule: EvaluatedRuleOperator,
                        entityValue: EvaluatedEntityOperator,
                        tree: TreeNodeInfo<Partial<{ root: boolean; label: string }>>
                    ) => {
                        if (tree?.nodeData?.root && tree.nodeData.label) {
                            tree.label = (
                                <>
                                    <strong>{tree.nodeData.label}</strong>
                                    {!tree.isExpanded ? (
                                        <>
                                            <Spacing vertical size="tiny" />
                                            <NodeTagValues values={entityValue.values} error={entityValue.error} />
                                        </>
                                    ) : null}
                                </>
                            );
                        }
                        if (!rule.inputs?.length) {
                            tree.childNodes = [
                                ...(tree?.childNodes ?? []),
                                newNode({
                                    rule,
                                    values: entityValue.values,
                                    operatorPlugins,
                                    error: entityValue.error,
                                }),
                            ];
                            return tree;
                        }
                        const currentNode = newNode({
                            rule,
                            values: entityValue.values,
                            operatorPlugins,
                            error: entityValue.error,
                        });
                        tree.childNodes = [...(tree?.childNodes ?? []), currentNode];

                        for (let i = 0; i < rule.inputs.length; i++) {
                            generateTree(rule.inputs[i], entityValue.children[i], currentNode);
                        }
                    };

                    generateTree(matchingRuleType.operator!, e, treeNodeInfo);
                    return treeNodeInfo;
                })
            );
        }, [treeExpansionMap]);

        return (
            <React.Fragment key={rowItem.id}>
                <TableExpandRow
                    isExpanded={rowIsExpanded}
                    onExpand={handleRowExpansion}
                    togglerText={
                        rowIsExpanded
                            ? t("linkingEvaluationTabView.table.collapseRow")
                            : t("linkingEvaluationTabView.table.expandRow")
                    }
                >
                    <TableCell>{rowItem.uri}</TableCell>
                </TableExpandRow>
                {(rowIsExpanded && (
                    <TableExpandedRow colSpan={colSpan} className="linking-table__expanded-row-container">
                        <Table size="compact" hasDivider={false} colorless>
                            <TableBody>
                                <TableRow>
                                    <TableCell>
                                        <Tree
                                            contents={treeNodes}
                                            onNodeExpand={handleNodeExpand}
                                            onNodeCollapse={handleNodeCollapse}
                                        />
                                    </TableCell>
                                </TableRow>
                            </TableBody>
                        </Table>
                    </TableExpandedRow>
                )) ||
                    null}
            </React.Fragment>
        );
    }
);

export default TransformEvaluationTabRow;
