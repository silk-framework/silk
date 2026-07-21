import React from "react";
import { Icon } from "@eccenca/gui-elements";
import { ValidIconName } from "@eccenca/gui-elements/src/components/atoms/Icon/canonicalIconNames";

/**
 * The icon associated with the grid tile currently being rendered. Provided by {@link GridBoard}
 * for every tile; the same icon is what the tile shows once it is minimized into the right rail.
 */
const GridTileIconContext = React.createContext<ValidIconName | undefined>(undefined);

/** Makes the current tile's icon available to its subtree (e.g. a self-carding widget's header). */
export const GridTileIconProvider = GridTileIconContext.Provider;

interface GridTileTitleIconProps {
    className?: string;
}

/**
 * Leading icon for a grid tile's title, mirroring the icon shown for the same tile in the minimized
 * rail so the two are easy to connect at a glance. Reads the tile's icon from context and renders
 * nothing when there is none — so it is safe to drop into any widget header, including ones that are
 * also rendered outside a {@link GridBoard}.
 */
export function GridTileTitleIcon({ className }: GridTileTitleIconProps) {
    const icon = React.useContext(GridTileIconContext);
    if (!icon) {
        return null;
    }
    // `mr-2` gives the gap to the title: most consumers place this straight inside a gui-elements
    // `CardTitle` (an `OverviewItemLine` flex row with no column gap), next to the heading element.
    return <Icon small name={icon} className={"mr-2 shrink-0 text-muted-foreground " + (className ?? "")} />;
}

export default GridTileTitleIcon;
