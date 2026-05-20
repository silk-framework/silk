import React from "react";

export interface ExternalSidebarContextProps {
    /** Reload tokens keyed by sidebar tab ID. A changed token triggers reloading for that tab. */
    reloadTokensByTabId?: Record<string, number>;
}

/** Optional external sidebar context consumed only by the rule editor sidebar. */
export const ExternalSidebarContext = React.createContext<ExternalSidebarContextProps>({});
