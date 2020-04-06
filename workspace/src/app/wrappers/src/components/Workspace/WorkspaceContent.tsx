import React from "react";
// import PropTypes from 'prop-types';
import Grid from "./../Grid/Grid";
import GridRow from "./../Grid/GridRow";

function WorkspaceContent({ children, className = '', ...restProps }: any) {
    return (
        <Grid
            {...restProps}
            as={'article'}
            className={'ecc-workspace__content '+className}
            fullWidth={true}
        >
            <GridRow>
                { children }
            </GridRow>
        </Grid>
    )
}

export default WorkspaceContent;
