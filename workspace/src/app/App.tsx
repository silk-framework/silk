import React from "react";

import Header from "./views/layout/header/Header";
import RouterOutlet from "./RouterOutlet";
import LanguageContainer from "./LanguageContainer";
import { RouteProps } from "react-router";
import { getHistory } from "./state/configureStore";
import { ConnectedRouter } from "connected-react-router";

import "normalize.css";
import "@wrappers/index.scss";

interface IProps {
    routes: RouteProps[];
    externalRoutes: any;
}

export default function App({ externalRoutes, routes }: IProps) {
    return (
        <LanguageContainer>
            <ConnectedRouter history={getHistory()}>
                <Header externalRoutes={externalRoutes}/>
                <RouterOutlet routes={routes}/>
            </ConnectedRouter>
        </LanguageContainer>
    );
}
