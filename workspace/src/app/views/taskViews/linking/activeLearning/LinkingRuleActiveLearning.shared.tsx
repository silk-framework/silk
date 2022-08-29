import { CSSProperties } from "react";
import "./LinkingRuleActiveLeraningConfig.scss";

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
    backgroundColor: "rgba(17, 20, 24, 0.0915)",
    fontWeight: "bolder",
    //color: "white",
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
        backgroundColor: "#003" // "#c2e6ea",
    },
    weakEquality: {
        breakingPoint: 0.0,
        backgroundColor: "#00c" // "#ddd7e2",
    },
    noEquality: {
        breakingPoint: -1.0,
        backgroundColor: "#0000f6" // "#fac9d9"
    },
    unknownEquality: {
        breakingPoint: undefined,
        backgroundColor: "#fff5d5"
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
