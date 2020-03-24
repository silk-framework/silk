import React from "react";
// import PropTypes from 'prop-types';
import { Column as CarbonColumn } from "carbon-components-react/lib/components/Grid";

const WorkspaceColumn = ({
    children,
    className = '',
    small = false,
    medium = false,
    full = true,
    ...restProps
}: any) => {
    let sizeConfig = {};
    if (small) sizeConfig = { md:2, lg:3 };
    if (medium) sizeConfig = { md:3, lg:5 };
    return (
        <CarbonColumn {...restProps} {...sizeConfig} className={'ecc-workspace__column '+className}>
            { children }
        </CarbonColumn>
    )
}

export default WorkspaceColumn;
