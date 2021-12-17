import React, { useEffect, useState } from "react";
import { RouteProps } from "react-router";
import { ConnectedRouter } from "connected-react-router";
import { useDispatch } from "react-redux";

import { commonOp } from "@ducks/common";
import { ApplicationContainer, ApplicationContent } from "gui-elements";

import Header from "./views/layout/Header";
import RouterOutlet from "./RouterOutlet";
import { getHistory } from "./store/configureStore";
import { RecentlyViewedModal } from "./views/shared/modals/RecentlyViewedModal";

interface IProps {
    routes: RouteProps[];
    externalRoutes: any;
}

export default function App({ externalRoutes, routes }: IProps) {
    const dispatch = useDispatch();
    useEffect(() => {
        dispatch(commonOp.fetchCommonSettingsAsync());
        dispatch(commonOp.fetchExportTypesAsync());
    }, [commonOp]);
    const [sideNavExpanded, setsideNavExpanded] = useState(false);

    return (
        <ConnectedRouter history={getHistory()}>
            <ApplicationContainer>
                <Header
                    isApplicationSidebarExpanded={sideNavExpanded}
                    onClickApplicationSidebarExpand={() => {
                        setsideNavExpanded(!sideNavExpanded);
                    }}
                />
                <ApplicationContent
                    isApplicationSidebarExpanded={sideNavExpanded}
                    isApplicationSidebarRail={!sideNavExpanded}
                >
                    <RouterOutlet routes={routes} />
                </ApplicationContent>
            </ApplicationContainer>
            <RecentlyViewedModal />
        </ConnectedRouter>
    );
}
