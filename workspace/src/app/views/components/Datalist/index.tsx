import React from "react";
import Spinner from "@wrappers/blueprint/spinner";
import StructuredListBody from "@wrappers/carbon/structured-list/StructuredListBody";
import StructuredListHead from "@wrappers/carbon/structured-list/StructuredListHead";
import StructuredListCell from "@wrappers/carbon/structured-list/StructuredListCell";
import StructuredListRow from "@wrappers/carbon/structured-list/StructuredListRow";
import { StructuredListWrapper } from "carbon-components-react";

function _Row({children}) {
    return (
        <StructuredListRow className={'data_row'}>
            {children}
        </StructuredListRow>
    )
}

function _Cell({children, ...restProps}) {
    return (
        <StructuredListCell className={'data_cell'} {...restProps}>
            {children}
        </StructuredListCell>
    )
}

function _Header({children}) {
    return (
        <StructuredListHead className={'header'}>
            <_Row>
                <_Cell>
                    {children}
                </_Cell>
            </_Row>
        </StructuredListHead>
    )
}

function _Body({children, ...restProps}) {
    return (
        <StructuredListBody {...restProps}>
            {children}
        </StructuredListBody>
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
            <StructuredListWrapper>
                {children}
            </StructuredListWrapper>
    )
}

DataList.Header = _Header;
DataList.Body = _Body;
DataList.Footer = _Footer;
DataList.Row = _Row;
DataList.Cell = _Cell;

export default DataList;
