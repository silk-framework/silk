import { CSSProperties } from "react";
import "./LinkingRuleActiveLeraningConfig.scss";

/** TODO: Temp styles, replace with proper CSS styles and classes. */
const mainColumnStyle: CSSProperties = {
    width: "40%",
    textAlign: "inherit",
    padding: "5px",
    borderWidth: "thin",
    borderStyle: "solid",
    borderColor: "lightgray",
};
const headerColumnStyle: CSSProperties = {
    ...mainColumnStyle,
    textAlign: "center",
    backgroundColor: "rgba(17, 20, 24, 0.0915)",
    fontWeight: "bolder",
    borderStyle: "none",
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

export const scoreColorConfig = {
    strongEquality: {
        breakingPoint: 0.5,
        backgroundColor: "#054b7a",
    },
    weakEquality: {
        breakingPoint: 0.0,
        backgroundColor: "#88ccf7",
    },
    noEquality: {
        breakingPoint: -1.0,
        backgroundColor: "#ceeafc",
    },
    unknownEquality: {
        breakingPoint: undefined,
        backgroundColor: "#fff5d5",
    }
}

export const scoreColorRepresentation = (score: number | undefined) => {
    let color: string | undefined = undefined;
    if (typeof score !== "undefined") {
        switch (true) {
            case (score >= scoreColorConfig.strongEquality.breakingPoint):
                color = scoreColorConfig.strongEquality.backgroundColor;
                break;
            case (score >= scoreColorConfig.weakEquality.breakingPoint):
                color = scoreColorConfig.weakEquality.backgroundColor;
                break;
            default:
                color = scoreColorConfig.noEquality.backgroundColor;
        }
    } else {
        color = scoreColorConfig.unknownEquality.backgroundColor;
    }

    return color;
}
