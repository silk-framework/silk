import React from "react";
import { DataTable as CarbonDataTable } from "carbon-components-react";

function TableContainer({ children, className = "", ...otherProps }: any) {
    if (typeof otherProps.title !== "undefined") {
        otherProps.title = "";
    }
    if (typeof otherProps.description !== "undefined") {
        otherProps.description = "";
    }

    return (
        <CarbonDataTable.TableContainer {...otherProps} className={"ecc-simpletable__container " + className}>
            {children}
        </CarbonDataTable.TableContainer>
    );
}

export default TableContainer;
