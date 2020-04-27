import React from "react";
// import PropTypes from 'prop-types';
import GridColumn from "./../Grid/GridColumn";

function WorkspaceMain({ children, className = "", ...restProps }: any) {
    return (
        <GridColumn {...restProps} className={"ecc-workspace__main " + className}>
            {children}
        </GridColumn>
    );
}

export default WorkspaceMain;
