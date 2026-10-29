import React from "react";

export interface ExternalSidebarContextProps {
    /** Sidebar-wide reload token. A changed token triggers reloading for the currently active tab. */
    reloadToken?: number;
}

/** Optional external sidebar context consumed only by the rule editor sidebar. */
export const ExternalSidebarContext = React.createContext<ExternalSidebarContextProps>({});
