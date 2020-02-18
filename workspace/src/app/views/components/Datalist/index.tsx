import React from "react";
import HTMLTable from "@wrappers/html-table";
import Card from "@wrappers/card";
import Spinner from "@wrappers/spinner";

function _Row({children}) {
    return (
        <div className={'data_row'}>
            {children}
        </div>
    )
}

function _Cell({children, ...restProps}) {
    return (
        <div className={'data_cell'} {...restProps}>
            {children}
        </div>
    )
}

function _Header({children}) {
    return (
        <div className={'header'}>
            <_Row>
                <_Cell>
                    {children}
                </_Cell>
            </_Row>
        </div>
    )
}

function _Body({children, ...restProps}) {
    return (
        <div {...restProps}>
        {children}
        </div>
    )
}

function _Footer({children}) {
    return (
        <div className={'footer'}>
            <_Row>
                <_Cell>
                    {children}
                </_Cell>
            </_Row>
        </div>
    )
}

const _loadingIndicator = () => <Spinner/>;

const _emptyContent = () => <p>No resources found</p>;

function DataList({children, isLoading, data}) {
    if (isLoading) {
        return _loadingIndicator();
    } else if (!data.length) {
        return _emptyContent();
    }
    return (
            <div>
                {children}
            </div>
    )
}

DataList.Header = _Header;
DataList.Body = _Body;
DataList.Footer = _Footer;
DataList.Row = _Row;
DataList.Cell = _Cell;

export default DataList;
