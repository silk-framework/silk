import { TreeNodeInfo } from "@blueprintjs/core";
import { IconButton, Table, TableBody, TableCell, TableRow, Tree } from "@eccenca/gui-elements";
import React from "react";

interface TableTreeProps {
    toggleTableExpansion: () => void;
    nodes: Array<TreeNodeInfo>;
    treeIsExpanded: boolean;
}

const TableTree: React.FC<TableTreeProps> = React.memo(
    ({ nodes = [], toggleTableExpansion, treeIsExpanded }: TableTreeProps) => {
        const [rowIsExpanded, setRowIsExpanded] = React.useState<boolean>(treeIsExpanded);
        React.useEffect(() => {
            setRowIsExpanded(treeIsExpanded);
        }, [treeIsExpanded]);

        const handleTreeToggle = React.useCallback(() => {
            setRowIsExpanded((t) => !t);
            toggleTableExpansion();
        }, []);

        return (
            <Table size="small" columnWidths={["30px", "40%", "40%", "7rem", "9rem"]} hasDivider={false} colorless>
                <TableBody>
                    <TableRow>
                        <TableCell style={{ paddingLeft: "0", paddingRight: "0" }}>
                            <IconButton
                                data-test-id="tree-expand-item-btn"
                                id={`tree-btn-${rowIsExpanded ? "expanded" : "collapsed"}`}
                                onClick={handleTreeToggle}
                                name={!rowIsExpanded ? "toggler-caretright" : "toggler-caretdown"}
                            />
                        </TableCell>
                        <TableCell>
                            <Tree contents={nodes} />
                        </TableCell>
                    </TableRow>
                </TableBody>
            </Table>
        );
    }
);

export default TableTree;
