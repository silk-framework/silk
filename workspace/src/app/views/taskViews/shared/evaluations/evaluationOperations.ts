import { IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";

/**
 * Gets all the nodes that are connected to the root node.
 */
const getSubTreeNodes = (ruleOperatorNodes: IRuleOperatorNode[], rootNodeId: string): IRuleOperatorNode[] => {
    const ruleNodes: Map<string, IRuleOperatorNode> = new Map();
    ruleOperatorNodes.forEach((node) => ruleNodes.set(node.nodeId, node));

    const subTreeNodes: IRuleOperatorNode[] = [];
    const addChildNodes = (ruleNode: IRuleOperatorNode | undefined) => {
        if (ruleNode) {
            subTreeNodes.push(ruleNode);
            ruleNode.inputs.forEach((input) => input && addChildNodes(ruleNodes.get(input)));
        }
    };
    addChildNodes(ruleNodes.get(rootNodeId));
    return subTreeNodes;
};

const evaluationUtils = {
    getSubTreeNodes,
};

export default evaluationUtils;
