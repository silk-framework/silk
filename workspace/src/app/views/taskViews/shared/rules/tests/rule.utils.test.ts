import utils from "../rule.utils";
import { IRuleOperatorNode } from "../../../../shared/RuleEditor/RuleEditor.typings";

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
        const [nodeMap, roots] = utils.convertToRuleOperatorNodeMap(ruleNodes);
        expect(nodeMap.size).toBe(3);
        expect(nodeMap.get("A")).toBe(ruleNodes[0]);
        expect(roots[0]).toBe(ruleNodes[2]);
    });
});
