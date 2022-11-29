import React from "react";
import {
    Grid,
    GridRow,
    GridColumn,
    TestableComponent
} from "@eccenca/gui-elements";

interface ComparisonDataObjectProps extends TestableComponent {
    children?: React.ReactNode;
    className?: string;
}

export const ComparisonDataContainer = ({children, className=""}: ComparisonDataObjectProps) => {
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

export const ComparisonDataHead = ({children}) => {
    return (
        <>
            {children}
        </>
    )
}

export const ComparisonDataBody = ({children}) => {
    return (
        <>
            {children}
        </>
    )
}

export const ComparisonDataRow = ({children, className="", ...otherProps}: ComparisonDataObjectProps) => {
    return (
        <GridRow className={`diapp-linking-learningdata__row ${className}`} data-test-id={otherProps["data-test-id"]}>
            {children}
        </GridRow>
    )
}

export const ComparisonDataHeader = ({children, className=""}: ComparisonDataObjectProps) => {
    return (
        <GridColumn className={`diapp-linking-learningdata__header ${className}`}>
            {children}
        </GridColumn>
    )
}

export const ComparisonDataCell = ({children, className=""}: ComparisonDataObjectProps) => {
    return (
        <GridColumn className={`diapp-linking-learningdata__cell ${className}`}>
            {children}
        </GridColumn>
    )
}

export const ComparisonDataConnection = ({children, className=""}: ComparisonDataObjectProps) => {
    return (
        <GridColumn className={`diapp-linking-learningdata__connection ${className}`}>
            {children}
        </GridColumn>
    )
}
