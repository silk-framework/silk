import React from "react";
// import PropTypes from 'prop-types';
import GridColumn from "./../Grid/GridColumn";

function WorkspaceSide({ children, className = '', ...restProps }: any) {
    return (
        <GridColumn
            {...restProps}
            className={'ecc-workspace__side '+className}
            sm={4} md={8} lg={5} xlg={5}
        >
            { children }
        </GridColumn>
    )
}

export default WorkspaceSide;
