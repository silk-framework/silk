import React, { useEffect } from "react";
import { RouteProps } from "react-router";
import { ConnectedRouter } from "connected-react-router";
import { useDispatch } from "react-redux";

import { commonOp } from "@ducks/common";
import { ApplicationContainer, ApplicationContent } from "@gui-elements/index";

import Header from "./views/layout/Header";
import RouterOutlet from "./RouterOutlet";
import { getHistory } from "./store/configureStore";

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

    return (
        <ConnectedRouter history={getHistory()}>
            <ApplicationContainer
                isSideNavExpanded={false}
                render={({
                    isSideNavExpanded,
                    onClickSideNavExpand
                }:any) => (
                    <>
                        <Header
                            isApplicationSidebarExpanded={isSideNavExpanded}
                            onClickApplicationSidebarExpand={onClickSideNavExpand}
                        />
                        <ApplicationContent isApplicationSidebarExpanded={isSideNavExpanded}>
                            <RouterOutlet routes={routes} />
                        </ApplicationContent>
                    </>
                )}
            />
        </ConnectedRouter>
    );
}
