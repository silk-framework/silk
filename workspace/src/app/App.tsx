import React, { useEffect } from "react";

import Header from "./views/layout/header/Header";
import RouterOutlet from "./RouterOutlet";
import LanguageContainer from "./LanguageContainer";
import { RouteProps } from "react-router";
import { getHistory } from "./state/configureStore";
import { ConnectedRouter } from "connected-react-router";

import { globalOp } from "@ducks/global";
import { useDispatch } from "react-redux";

interface IProps {
    routes: RouteProps[];
    externalRoutes: any;
}

export default function App({ externalRoutes, routes }: IProps) {
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
                <Header externalRoutes={externalRoutes}/>
                <RouterOutlet routes={routes}/>
            </ConnectedRouter>
        </LanguageContainer>
    );
}
