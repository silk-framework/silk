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
