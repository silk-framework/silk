import React, { CSSProperties } from "react";
import "./LinkingRuleActiveLeraningConfig.scss";

/** Arrow going to the right. */
export const ArrowRight = () => <div className={"arrow-right"} />;

/** Arrow giond to the left. */
export const ArrowLeft = () => <div className={"arrow-left"} />;

/** A dashed line. */
export const DashedLine = () => {
    return (
        <svg width="100%" height="10px" viewBox="0 0 100 10" preserveAspectRatio="none">
            <g fill="none" stroke="black">
                <line x1="0" y1="5" x2="100" y2="5" strokeDasharray={"2 2"} />
            </g>
        </svg>
    );
};

/** TODO: Temp styles, replace with proper CSS styles and classes. */
const mainColumnStyle: CSSProperties = {
    width: "40%",
    maxWidth: "40%",
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
    display: "flex",
    columnWidth: "20%",
    maxWidth: "20%",
    padding: "5px",
    alignItems: "center",
    justifyContent: "center",
};

export const columnStyles = {
    mainColumnStyle,
    headerColumnStyle,
    centerColumnStyle,
};
