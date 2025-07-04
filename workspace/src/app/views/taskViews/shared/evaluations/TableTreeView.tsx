import { Divider, IconButton, Table, TableBody, TableCell, TableRow, Tree, TreeNodeInfo } from "@eccenca/gui-elements";
import React from "react";

interface TableTreeProps {
    toggleTableExpansion: () => void;
    nodes: Array<TreeNodeInfo>;
    treeIsExpanded: boolean;
    columnWidths?: string[];
}

const TableTree: React.FC<TableTreeProps> = React.memo(
    ({ nodes = [], toggleTableExpansion, treeIsExpanded, columnWidths }: TableTreeProps) => {
        const [rowIsExpanded, setRowIsExpanded] = React.useState<boolean>(treeIsExpanded);
        React.useEffect(() => {
            setRowIsExpanded(treeIsExpanded);
        }, [treeIsExpanded]);

        const handleTreeToggle = React.useCallback(() => {
            setRowIsExpanded((t) => !t);
            toggleTableExpansion();
        }, []);

        return (
            <Table size="small" columnWidths={columnWidths} hasDivider={false} colorless>
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
                        <TableCell colSpan={columnWidths ? columnWidths.length - 1 : undefined}>
                            <Divider width="half" alignment="center" />
                            <Tree contents={nodes} />
                        </TableCell>
                    </TableRow>
                </TableBody>
            </Table>
        );
    }
);

export default TableTree;
