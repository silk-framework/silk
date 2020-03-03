import React, { useEffect } from "react";

import Header from "./views/layout/Header";
import RouterOutlet from "./RouterOutlet";
import LanguageContainer from "./LanguageContainer";
import { RouteProps } from "react-router";
import { getHistory } from "./store/configureStore";
import { ConnectedRouter } from "connected-react-router";

import { globalOp } from "@ducks/global";
import { useDispatch } from "react-redux";

import {
    ApplicationContainer,
    ApplicationContent,
} from "@wrappers/index";

interface IProps {
    routes: RouteProps[];
    externalRoutes: any;
}

export default function App({externalRoutes, routes}: IProps) {
    const dispatch = useDispatch();
    useEffect(() => {
        dispatch(globalOp.addBreadcrumb({
            href: '',
            text: 'Home'
        }));
        dispatch(globalOp.addBreadcrumb({
            href: '',
            text: 'Data Integration'
        }));
    }, []);

    return (
        <LanguageContainer>
            <ConnectedRouter history={getHistory()}>
                <ApplicationContainer
                    render = {({ isApplicationSidebarExpanded, onClickApplicationSidebarExpand }) => (
                        <>
                            <Header externalRoutes={externalRoutes}/>
                            <ApplicationContent>
                                <RouterOutlet routes={routes}/>
                            </ApplicationContent>
                        </>
                    )}
                />
            </ConnectedRouter>
        </LanguageContainer>
    );
}
