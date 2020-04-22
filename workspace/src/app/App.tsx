import React, { useEffect } from "react";

import Header from "./views/layout/Header";
import RouterOutlet from "./RouterOutlet";
import { RouteProps } from "react-router";
import { getHistory } from "./store/configureStore";
import { ConnectedRouter } from "connected-react-router";

import { commonOp } from "@ducks/common";
import { useDispatch } from "react-redux";

import { ApplicationContainer, ApplicationContent, } from "@wrappers/index";

interface IProps {
    routes: RouteProps[];
    externalRoutes: any;
}

export default function App({externalRoutes, routes}: IProps) {
    const dispatch = useDispatch();
    useEffect(() => {
        dispatch(commonOp.fetchCommonSettingsAsync());
    }, []);

    return (
        <ConnectedRouter history={getHistory()}>
            <ApplicationContainer
                render={({isApplicationSidebarExpanded, onClickApplicationSidebarExpand}) => (
                    <>
                        <Header
                            externalRoutes={externalRoutes}
                            isApplicationSidebarExpanded={isApplicationSidebarExpanded}
                            onClickApplicationSidebarExpand={onClickApplicationSidebarExpand}/>
                        <ApplicationContent>
                            <RouterOutlet routes={routes}/>
                        </ApplicationContent>
                    </>
                )}
            />
        </ConnectedRouter>
    );
}
