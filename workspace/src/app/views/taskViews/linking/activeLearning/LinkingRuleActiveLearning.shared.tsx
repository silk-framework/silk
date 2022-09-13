import "./LinkingRuleActiveLeraningConfig.scss";

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
        backgroundColor: "#e5e5e5",
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
