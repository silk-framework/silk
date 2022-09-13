import React from "react";
import {
    Grid,
    GridRow,
    GridColumn,
} from "@eccenca/gui-elements";

interface ComparisionDataObjectProps {
    children?: React.ReactNode;
    className?: string;
}

export const ComparisionDataContainer = ({children, className=""}: ComparisionDataObjectProps) => {
    return (
        <Grid
            columns={3}
            fullWidth={true}
            className={`diapp-linking-learningdata__container ${className}`}
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

export const ComparisionDataRow = ({children, className=""}: ComparisionDataObjectProps) => {
    return (
        <GridRow className={`diapp-linking-learningdata__row ${className}`}>
            {children}
        </GridRow>
    )
}

export const ComparisionDataHeader = ({children, className=""}: ComparisionDataObjectProps) => {
    return (
        <GridColumn className={`diapp-linking-learningdata__header ${className}`}>
            {children}
        </GridColumn>
    )
}

export const ComparisionDataCell = ({children, className=""}: ComparisionDataObjectProps) => {
    return (
        <GridColumn className={`diapp-linking-learningdata__cell ${className}`}>
            {children}
        </GridColumn>
    )
}

export const ComparisionDataConnection = ({children, className=""}: ComparisionDataObjectProps) => {
    return (
        <GridColumn className={`diapp-linking-learningdata__connection ${className}`}>
            {children}
        </GridColumn>
    )
}
