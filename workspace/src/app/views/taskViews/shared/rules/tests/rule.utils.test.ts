import {
    IRuleOperatorNode,
    RuleEditorValidationNode,
    RuleOperatorPluginType,
} from "../../../../shared/RuleEditor/RuleEditor.typings";
import utils from "../rule.utils";

describe("Rule utils", () => {
    const ruleNode = (nodeId: string, node: Partial<IRuleOperatorNode> = {}): IRuleOperatorNode => {
        return {
            nodeId: nodeId,
            inputs: [],
            label: "Rule node",
            parameters: {},
            pluginId: "TestPlugin",
            pluginType: "unknown",
            portSpecification: {
                minInputPorts: 0,
            },
            inputsCanBeSwitched: false,
            ...node,
        };
    };
    const rootNodes = (operatorNodes: IRuleOperatorNode[]) =>
        utils.convertToRuleOperatorNodeMap(operatorNodes, false)[1];
    const rootNodeIds = (operatorNodes: IRuleOperatorNode[]) => rootNodes(operatorNodes).map((op) => op.nodeId);
    it("should convert rule operators to node map and extract root nodes", () => {
        expect(rootNodes([])).toHaveLength(0);
        expect(
            rootNodeIds([
                ruleNode("A"),
                ruleNode("B", { inputs: ["A"] }),
                ruleNode("C", { inputs: ["B"] }),
                ruleNode("D"),
            ])
        ).toStrictEqual(["C", "D"]);
        const ruleNodes = [ruleNode("A"), ruleNode("B"), ruleNode("C", { inputs: ["A", "B"] })];
        expect(rootNodeIds(ruleNodes)).toStrictEqual(["C"]);
        const [nodeMap, roots] = utils.convertToRuleOperatorNodeMap(ruleNodes, true);
        expect(nodeMap.size).toBe(3);
        expect(nodeMap.get("A")).toBe(ruleNodes[0]);
        expect(roots[0]).toBe(ruleNodes[2]);
    });

    const validateRuleTreeFactory = (ruleTree: Map<string, RuleEditorValidationNode>) => {
        const validateTrue = (sourceNode: string, targetNode: string, targetPort: number = 0) => {
            if (!utils.validateConnection(ruleTree.get(sourceNode)!!, ruleTree.get(targetNode)!!, targetPort)) {
                throw Error(
                    `Connection from '${sourceNode}' to '${targetNode}' in input ${targetPort} is not a valid connection, but must be!`
                );
            }
        };
        const validateFalse = (sourceNode: string, targetNode: string, targetPort: number = 0) => {
            if (utils.validateConnection(ruleTree.get(sourceNode)!!, ruleTree.get(targetNode)!!, targetPort)) {
                throw Error(
                    `Connection from '${sourceNode}' to '${targetNode}' in input ${targetPort} is a valid connection, but must not be!`
                );
            }
        };
        return [validateTrue, validateFalse];
    };

    const ruleTree = (edges: TestEdge[] = [], additionalNodes: TestOperator[] = []) =>
        createTestGraph(
            [
                operator("sp1", "PathInputOperator", "sourcePathInput"),
                operator("sp2", "PathInputOperator", "sourcePathInput"),
                operator("sp3", "PathInputOperator", "sourcePathInput"),
                operator("tp1", "PathInputOperator", "targetPathInput"),
                operator("tp2", "PathInputOperator", "targetPathInput"),
                operator("tp3", "PathInputOperator", "targetPathInput"),
                operator("t1", "TransformOperator"),
                operator("t2", "TransformOperator"),
                operator("t3", "TransformOperator"),
                operator("t4", "TransformOperator"),
                operator("c1", "ComparisonOperator"),
                operator("c2", "ComparisonOperator"),
                operator("c3", "ComparisonOperator"),
                operator("a1", "AggregationOperator"),
                operator("a2", "AggregationOperator"),
                ...additionalNodes,
            ],
            edges
        );

    it("should validate connections from input paths to other operators", () => {
        const [validateTrue, validateFalse] = validateRuleTreeFactory(ruleTree());
        // Into comparisons
        validateTrue("sp1", "c1", 0);
        validateTrue("tp1", "c1", 1);
        validateFalse("sp1", "c1", 1);
        validateFalse("tp1", "c1", 0);
        // Into transform op
        validateTrue("sp1", "t1");
        validateTrue("tp1", "t1");
        // Into aggregation
        validateFalse("sp1", "a1");
        validateFalse("tp1", "a1");
        // Into paths
        validateFalse("sp1", "tp1");
    });

    it("should validate connections from transform operators to other operators", () => {
        const [validateTrue, validateFalse] = validateRuleTreeFactory(ruleTree());
        //Into paths
        validateFalse("t1", "sp1");
        validateFalse("t1", "tp1");
        // Into other transform ops
        validateTrue("t1", "t2");
        // Into comparisons
        validateTrue("t1", "c1");
        // Into aggregations
        validateFalse("t1", "a1");
    });

    it("should validate connections from comparison operators to other operators", () => {
        const [validateTrue, validateFalse] = validateRuleTreeFactory(ruleTree());
        //Into paths
        validateFalse("c1", "sp1");
        validateFalse("c1", "tp1");
        // Into transform ops
        validateFalse("c1", "t1");
        // Into comparisons
        validateFalse("c1", "c2");
        // Into aggregations
        validateTrue("c1", "a1");
    });

    it("should validate connections from aggregation operators to other operators", () => {
        const [validateTrue, validateFalse] = validateRuleTreeFactory(ruleTree());
        //Into paths
        validateFalse("a1", "sp1");
        validateFalse("a1", "tp1");
        // Into transform ops
        validateFalse("a1", "t1");
        // Into comparisons
        validateFalse("a1", "c2");
        // Into aggregations
        validateTrue("a1", "a1");
    });

    it("should validate simple value chains", () => {
        const [validateTrue, validateFalse] = validateRuleTreeFactory(
            ruleTree([edge("sp1", "t1"), edge("tp1", "t2"), edge("t3", "c1"), edge("t4", "c2", 1)])
        );
        /** Path to transform */
        validateTrue("sp2", "t3");
        validateFalse("sp2", "t4");
        validateTrue("sp2", "t1", 1);
        validateFalse("sp2", "t2", 1);
        validateTrue("sp2", "t1");
        validateTrue("sp2", "t2");
        validateFalse("tp2", "t3");
        validateTrue("tp2", "t4");
        validateFalse("tp2", "t1", 1);
        validateTrue("tp2", "t2", 1);
        validateTrue("tp2", "t1");
        validateTrue("tp2", "t2");

        /** Transform to transform */
        validateTrue("t1", "t3");
        validateFalse("t1", "t4");
        validateFalse("t1", "t2", 1);
        // Target path replaces source paths, so this is valid
        validateTrue("t1", "t2");
        validateTrue("t2", "t1");
        validateTrue("t2", "t1");
        validateTrue("t3", "t2");
        validateTrue("t3", "t2", 1);
        validateTrue("t4", "t1");
        validateTrue("t4", "t1", 1);

        /** Transform to comparison */
        validateTrue("t1", "c3");
        validateFalse("t1", "c3", 1);
        validateFalse("t2", "c3");
        validateTrue("t2", "c3", 1);
    });

    it("should validate complex value chain combinations", () => {
        const validateFns = validateRuleTreeFactory(ruleTree([edge("sp1", "t1"), edge("t1", "c1"), edge("tp1", "t2")]));
        const validateFalse = validateFns[1];
        validateFalse("tp1", "t1");
        validateFalse("tp1", "t1", 1);
        validateFalse("t2", "t1");
        validateFalse("t2", "t1", 1);
    });

    it("should validate connections to reversible comparators", () => {
        const [validateTrue, validateFalse] = validateRuleTreeFactory(
            ruleTree(
                [
                    // Setup some paths
                    edge("source1", "t1"),
                    edge("target1", "t2"),
                    edge("source2", "t3"),
                    edge("target2", "t4"),
                    edge("sp1", "reverseS1"),
                    edge("sp2", "reverseS2", 1),
                    edge("tp1", "reverseT1"),
                    edge("tp2", "reverseT2", 1),
                    edge("t1", "reverseS3", 1),
                    edge("t2", "reverseT3"),
                ],
                [
                    // additional path operators
                    operator("source1", "PathInputOperator", "sourcePathInput"),
                    operator("source2", "PathInputOperator", "sourcePathInput"),
                    operator("target1", "PathInputOperator", "targetPathInput"),
                    operator("target2", "PathInputOperator", "targetPathInput"),
                    operator("reverseNoInput", "ComparisonOperator", undefined, true),
                    operator("reverseS1", "ComparisonOperator", undefined, true),
                    operator("reverseS2", "ComparisonOperator", undefined, true),
                    operator("reverseS3", "ComparisonOperator", undefined, true),
                    operator("reverseT1", "ComparisonOperator", undefined, true),
                    operator("reverseT2", "ComparisonOperator", undefined, true),
                    operator("reverseT3", "ComparisonOperator", undefined, true),
                ]
            )
        );
        // reversible comparator without inputs takes any connection
        validateTrue("tp3", "reverseNoInput");
        validateTrue("tp3", "reverseNoInput", 1);
        validateTrue("t1", "reverseNoInput");
        validateTrue("t1", "reverseNoInput", 1);
        validateTrue("sp3", "reverseNoInput", 1);
        // reversible comparator with source input must not get another source input, but replace is OK
        validateTrue("sp3", "reverseS1");
        validateFalse("sp3", "reverseS1", 1);
        validateFalse("sp3", "reverseS2");
        validateTrue("sp3", "reverseS2", 1);
        validateTrue("t3", "reverseS1");
        validateFalse("t3", "reverseS1", 1);
        validateFalse("t3", "reverseS2");
        validateTrue("t3", "reverseS2", 1);
        validateFalse("t3", "reverseS3");
        validateTrue("t3", "reverseS3", 1);
        // same for target input
        validateTrue("tp3", "reverseT1");
        validateFalse("tp3", "reverseT1", 1);
        validateFalse("tp3", "reverseT2");
        validateTrue("tp3", "reverseT2", 1);
        validateTrue("t4", "reverseT1");
        validateFalse("t4", "reverseT1", 1);
        validateFalse("t4", "reverseT2");
        validateTrue("t4", "reverseT2", 1);
        validateFalse("t4", "reverseT3", 1);
        validateTrue("t4", "reverseT3");
    });
});

type TestPluginType = "doesntMatter" | "sourcePathInput" | "targetPathInput";
interface TestOperator {
    nodeId: string;
    pluginType: RuleOperatorPluginType;
    pluginId: TestPluginType;
    inputsCanBeSwitched?: boolean;
}

interface TestEdge {
    sourceNode: string;
    targetNode: string;
    targetPort: number;
}

const operator = (
    nodeId: string,
    pluginType: RuleOperatorPluginType,
    pluginId: TestPluginType = "doesntMatter",
    inputsCanBeSwitched: boolean = false
): TestOperator => {
    return { nodeId, pluginType, pluginId, inputsCanBeSwitched };
};

const edge = (sourceNode: string, targetNode: string, targetPort: number = 0): TestEdge => {
    return { sourceNode, targetNode, targetPort };
};

const createTestGraph = (nodes: TestOperator[], edges: TestEdge[]): Map<string, RuleEditorValidationNode> => {
    const nodeMap = new Map<string, RuleEditorValidationNode>();
    const inputNodes = new Map<string, (string | undefined)[]>(nodes.map((n) => [n.nodeId, []]));
    const outputNode = new Map<string, string>();
    edges.forEach((e) => {
        const inputNode = inputNodes.get(e.targetNode);
        inputNode!![e.targetPort] = e.sourceNode;
        outputNode.set(e.sourceNode, e.targetNode);
    });
    nodes.forEach((node) => {
        nodeMap.set(node.nodeId, {
            // Most properties from IRuleOperatorNode are not important (not read)
            node: {
                ...node,
            } as IRuleOperatorNode,
            inputs: () =>
                inputNodes.get(node.nodeId)!!.map((inputNode) => (inputNode ? nodeMap.get(inputNode)!! : undefined)),
            output: () => (outputNode.get(node.nodeId) ? nodeMap.get(outputNode.get(node.nodeId)!!) : undefined),
        });
    });
    return nodeMap;
};
