import { IPluginDetails } from "@ducks/common/typings";
import {
    Table,
    TableBody,
    TableCell,
    TableExpandedRow,
    TableExpandRow,
    TableRow,
    Tag,
    TagList,
    Tree,
    TreeNodeInfo,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { operatorInputMapping } from "../../../../../views/taskViews/linking/evaluation/tabView/LinkingEvaluationRow";
import { OperatorLabel } from "../../../../../views/taskViews/shared/evaluations/OperatorLabel";
import { EvaluatedComplexRule, EvaluatedEntity, EvaluatedEntityOperator, EvaluatedURIRule } from "./typing";

interface TransformEvaluationTabRowProps {
    rowExpandedByParent: boolean;
    rowItem: { uri: string; id: string };
    entity: EvaluatedEntity;
    colSpan: number;
    operatorPlugins: Array<IPluginDetails>;
    rules: Array<EvaluatedURIRule | EvaluatedComplexRule>;
}

const TransformEvaluationTabRow: React.FC<TransformEvaluationTabRowProps> = ({
    rowItem,
    colSpan,
    rowExpandedByParent,
    entity,
    rules,
    operatorPlugins,
}) => {
    const [rowIsExpanded, setRowIsExpanded] = React.useState<boolean>(rowExpandedByParent);
    const [treeExpansionMap, setTreeExpansionMap] = React.useState<Map<string, boolean>>(new Map());
    const [treeNodes, setTreeNodes] = React.useState<TreeNodeInfo[]>([]);
    const [t] = useTranslation();

    React.useEffect(() => {
        setRowIsExpanded(rowExpandedByParent);
    }, [rowExpandedByParent]);

    //update tree
    React.useEffect(() => {
        buildTree();
    }, [treeExpansionMap]);

    const handleRowExpansion = React.useCallback(() => setRowIsExpanded((e) => !e), []);

    const handleNodeExpand = React.useCallback((node) => {
        setTreeExpansionMap((prev) => new Map([...prev, [node.id, true]]));
    }, []);

    const handleNodeCollapse = React.useCallback((node) => {
        setTreeExpansionMap((prev) => new Map([...prev, [node.id, false]]));
    }, []);

    const buildTree = React.useCallback(() => {
        setTreeNodes(
            entity.values.map((e) => {
                const matchingRuleType = rules.find((rule) => rule.operator.id === e.operatorId)!;
                const newNode = (
                    rule: EvaluatedURIRule["operator"],
                    values: EvaluatedEntityOperator["values"]
                ): TreeNodeInfo<{}> => {
                    return {
                        id: rule.id,
                        hasCaret: false,
                        isExpanded: true,
                        label: (
                            <OperatorLabel
                                tagPluginType={operatorInputMapping[rule.type]}
                                operator={rule}
                                operatorPlugins={operatorPlugins}
                            >
                                <TagList>
                                    {values.map((v, i) => (
                                        <Tag key={i} round emphasis="stronger" interactive>
                                            {v}
                                        </Tag>
                                    ))}
                                </TagList>
                            </OperatorLabel>
                        ),
                    };
                };
                let treeNodeInfo = {
                    hasCaret: true,
                    id: matchingRuleType.id,
                    isExpanded: treeExpansionMap.get(matchingRuleType.id) ?? true,
                    label: <strong>{(matchingRuleType as EvaluatedComplexRule)?.mappingTarget?.uri ?? "URI"}</strong>,
                    childNodes: [],
                } as TreeNodeInfo;

                const generateTree = (
                    rule: EvaluatedURIRule["operator"],
                    entityValue: EvaluatedEntityOperator,
                    tree: TreeNodeInfo
                ) => {
                    if (!rule.inputs?.length) {
                        tree.childNodes = [...(tree?.childNodes ?? []), newNode(rule, entityValue.values)];
                        return tree;
                    }
                    const currentNode = newNode(rule, entityValue.values);
                    tree.childNodes = [...(tree?.childNodes ?? []), currentNode];

                    for (let i = 0; i < rule.inputs.length; i++) {
                        generateTree(rule.inputs[i], entityValue.children[i], currentNode);
                    }
                };

                generateTree(matchingRuleType.operator, e, treeNodeInfo);
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
        </React.Fragment>
    );
};

export default TransformEvaluationTabRow;
