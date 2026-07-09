// Score gradient expressed via design tokens/palette custom properties instead of raw hex, so it
// follows the active color scheme. `strongEquality`/`weakEquality` are the closest same-hue (~204°
// brand blue) tints available (exact hex parity isn't possible since this 2-step gradient was
// hand-picked, not sourced from a single palette ramp); `noEquality`/`unknownEquality` map to exact
// token/palette matches (#e5e5e5 === --border, #fff5d5 ≈ layout-yellow-100 #fff6d5).
export const scoreColorConfig = {
    strongEquality: {
        breakingPoint: 0.5,
        backgroundColor: "var(--primary)",
    },
    weakEquality: {
        breakingPoint: 0.0,
        backgroundColor: "var(--eccgui-color-palette-identity-accent-300)",
    },
    noEquality: {
        breakingPoint: -1.0,
        backgroundColor: "var(--border)",
    },
    unknownEquality: {
        breakingPoint: undefined,
        backgroundColor: "var(--eccgui-color-palette-layout-yellow-100)",
    },
};

export const scoreColorRepresentation = (score: number | undefined) => {
    let color: string | undefined = undefined;
    if (typeof score !== "undefined") {
        switch (true) {
            case score >= scoreColorConfig.strongEquality.breakingPoint:
                color = scoreColorConfig.strongEquality.backgroundColor;
                break;
            case score >= scoreColorConfig.weakEquality.breakingPoint:
                color = scoreColorConfig.weakEquality.backgroundColor;
                break;
            default:
                color = scoreColorConfig.noEquality.backgroundColor;
        }
    } else {
        color = scoreColorConfig.unknownEquality.backgroundColor;
    }

    return color;
};
