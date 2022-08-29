import React from "react";
import {
    Grid,
    GridRow,
    GridColumn,
} from "@eccenca/gui-elements";
import { columnStyles } from "./../LinkingRuleActiveLearning.shared";

export const ComparisionDataContainer = ({children}) => {
    return (
        <Grid
            columns={3}
            fullWidth={true}
            className="diapp-linking-learningdata__container"
        >
            {children}
        </Grid>
    )
}

export const ComparisionDataHead = ({children}) => {
    return (
        <>
            {children}
        </>
    )
}

export const ComparisionDataBody = ({children}) => {
    return (
        <>
            {children}
        </>
    )
}

export const ComparisionDataRow = ({children}) => {
    return (
        <GridRow
            style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}
            className="diapp-linking-learningdata__row"
        >
            {children}
        </GridRow>
    )
}

export const ComparisionDataHeader = ({children}) => {
    return (
        <GridColumn style={columnStyles.headerColumnStyle} className="diapp-linking-learningdata__header">
            {children}
        </GridColumn>
    )
}

export const ComparisionDataCell = ({children}) => {
    return (
        <GridColumn style={{...columnStyles.mainColumnStyle}} className="diapp-linking-learningdata__cell">
            {children}
        </GridColumn>
    )
}

export const ComparisionDataConnection = ({children}) => {
    return (
        <GridColumn style={columnStyles.centerColumnStyle} className="diapp-linking-learningdata__connection">
            {children}
        </GridColumn>
    )
}
