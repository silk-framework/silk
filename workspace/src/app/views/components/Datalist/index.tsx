import React from "react";
import Spinner from "@wrappers/blueprint/spinner";
import StructuredListBody from "@wrappers/carbon/structured-list/StructuredListBody";
import StructuredListHead from "@wrappers/carbon/structured-list/StructuredListHead";
import StructuredListCell from "@wrappers/carbon/structured-list/StructuredListCell";
import StructuredListRow from "@wrappers/carbon/structured-list/StructuredListRow";
import { StructuredListWrapper } from "carbon-components-react";

function _ListRow({children, ...restProps}) {
    return (
        <StructuredListRow {...restProps}>
            {children}
        </StructuredListRow>
    )
}

function _Cell({children, ...restProps}) {
    return (
        <StructuredListCell {...restProps}>
            {children}
        </StructuredListCell>
    )
}

function _Header({children}) {
    return (
        <StructuredListHead className={'header'}>
            <_ListRow head>
                {children}
            </_ListRow>
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
        <StructuredListBody className={'footer'}>
            {children}
        </StructuredListBody>
    )
}

const _loadingIndicator = () => <Spinner/>;

const _emptyContent = () => <p>No resources found</p>;

function DataList({children, isLoading = false, data}) {
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
DataList.ListRow = _ListRow;
DataList.Cell = _Cell;

export default DataList;
