import { HTMLTable } from "@blueprintjs/core";
import React from "react";

function _Row({children}) {
    return (
        <tr>
            {children}
        </tr>
    )
}

function _Cell({children, ...restProps}) {
    return (
        <td {...restProps}>
            {children}
        </td>
    )
}

function _Header({ children }) {
    return (
        <thead>
            <_Row>
                <_Cell>
                    {children}
                </_Cell>
            </_Row>
        </thead>
    )
}

function _Body({children}) {
    return (
        <tbody>
        {children}
        </tbody>
    )
}

function _Footer({children}) {
    return (
        <tfoot>
        <_Row>
            <_Cell>
                {children}
            </_Cell>
        </_Row>
        </tfoot>
    )
}

function DataList({ children }) {
    return (
        <HTMLTable bordered={true} interactive={true} striped={true}>
            {children}
        </HTMLTable>
    )
}

DataList.Header = _Header;
DataList.Body = _Body;
DataList.Footer = _Footer;
DataList.Row = _Row;
DataList.Cell = _Cell;

export default DataList;
