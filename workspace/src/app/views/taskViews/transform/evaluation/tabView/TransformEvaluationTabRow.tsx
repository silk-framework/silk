import React from "react";
import { IPluginDetails } from "@ducks/common/typings";
import { TableCell, TableExpandedRow, TableExpandRow, TreeNodeInfo } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { newNode, NodeTagValues } from "./TransformEvaluationTabViewUtils";
import {
    EvaluatedComplexRule,
    EvaluatedEntity,
    EvaluatedEntityOperator,
    EvaluatedRuleOperator,
    EvaluatedURIRule,
} from "./typing";
import TableTree from "../../../../../views/taskViews/shared/evaluations/TableTreeView";

interface TransformEvaluationTabRowProps {
    rowExpandedByParent: boolean;
    rowItem: { uri: string; id: string };
    entity: EvaluatedEntity;
    colSpan: number;
    operatorPlugins: Array<IPluginDetails>;
    rules: Array<EvaluatedURIRule | EvaluatedComplexRule>;
    zebra?: boolean;
}

const TransformEvaluationTabRow: React.FC<TransformEvaluationTabRowProps> = React.memo(
    ({ rowItem, colSpan, rowExpandedByParent, entity, rules, operatorPlugins, zebra = false }) => {
        const [rowIsExpanded, setRowIsExpanded] = React.useState<boolean>(rowExpandedByParent);
        const [treeExpansionMap, setTreeExpansionMap] = React.useState<Map<string, boolean>>(new Map());
        const [multipleTrees, setMultipleTrees] = React.useState<TreeNodeInfo[]>([]);
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

        const handleTreeExpansion = React.useCallback(
            (rowId: string) =>
                setTreeExpansionMap((prevExpansion) => new Map([...prevExpansion, [rowId, !prevExpansion.get(rowId)]])),
            []
        );

        const buildTree = React.useCallback(() => {
            setMultipleTrees(
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
                        hasCaret: false,
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
                                    <span className="tree-nodeData-label">{tree.nodeData.label}</span>
                                    {!tree.isExpanded ? (
                                        <NodeTagValues values={entityValue.values} error={entityValue.error} />
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
            <>
                <TableExpandRow
                    useZebraStyle={zebra}
                    isExpanded={rowIsExpanded}
                    onExpand={handleRowExpansion}
                    togglerText={
                        rowIsExpanded
                            ? t("linkingEvaluationTabView.table.collapseRow")
                            : t("linkingEvaluationTabView.table.expandRow")
                    }
                >
                    <TableCell style={{ verticalAlign: "middle" }}>{rowItem.uri}</TableCell>
                </TableExpandRow>
                {(rowIsExpanded && (
                    <TableExpandedRow colSpan={colSpan} className="linking-table__expanded-row-container">
                        {multipleTrees.map((tree) => (
                            <TableTree
                                columnWidths={["30px", "100%"]}
                                treeIsExpanded={!!treeExpansionMap.get(tree.id as string)}
                                key={tree.id}
                                nodes={[tree]}
                                toggleTableExpansion={() => handleTreeExpansion(tree.id as string)}
                            />
                        ))}
                    </TableExpandedRow>
                )) ||
                    null}
            </>
        );
    }
);

export default TransformEvaluationTabRow;
