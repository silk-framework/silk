import React from "react";

import Header from "./views/layout/header/Header";
import RouterOutlet from "./RouterOutlet";
import LanguageContainer from "./LanguageContainer";
import { RouteProps } from "react-router";
import { ConnectedRouter } from "connected-react-router";
import { getHistory } from "./state/configureStore";

import "normalize.css";
import "@blueprintjs/core/lib/css/blueprint.css";

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
