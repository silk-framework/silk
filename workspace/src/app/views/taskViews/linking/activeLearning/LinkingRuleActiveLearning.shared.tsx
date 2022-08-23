import React, { CSSProperties } from "react";
import "./LinkingRuleActiveLeraningConfig.scss";

/** Arrow going to the right. */
export const ArrowRight = () => <div className={"arrow-right"} />;

/** Arrow giond to the left. */
export const ArrowLeft = () => <div className={"arrow-left"} />;

/** A dashed line. */
export const DashedLine = () => {
    return (
        <div style={{
            height: "0px",
            width: "auto",
            borderTop: "2px dashed lightgray"
        }}/>
    );
};

/** TODO: Temp styles, replace with proper CSS styles and classes. */
const mainColumnStyle: CSSProperties = {
    width: "40%",
    textAlign: "center",
    padding: "5px",
    borderWidth: "thin",
    borderStyle: "solid",
    borderColor: "lightgray",
};
const headerColumnStyle: CSSProperties = {
    ...mainColumnStyle,
    backgroundColor: "blue",
    color: "white",
};
const centerColumnStyle: CSSProperties = {
    width: "20%",
    maxWidth: "20%",
    padding: "5px",
};

export const columnStyles = {
    mainColumnStyle,
    headerColumnStyle,
    centerColumnStyle,
};
