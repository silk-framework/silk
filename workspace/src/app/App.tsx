import React, { useEffect } from "react";
import { ConnectedRouter } from "connected-react-router";
import { useDispatch, ReactReduxContext } from "react-redux";

import { commonOp } from "@ducks/common";
import RouterOutlet from "./RouterOutlet";
import { AppDispatch, getHistory } from "./store/configureStore";
import { IRouteProps } from "./appRoutes";

interface IProps {
    routes: IRouteProps[];
    externalRoutes: any;
}

export default function App({ externalRoutes, routes }: IProps) {
    const dispatch = useDispatch<AppDispatch>();

    useEffect(() => {
        dispatch(commonOp.fetchCommonSettingsAsync());
        dispatch(commonOp.fetchExportTypesAsync());
    }, [commonOp]);
    return (
        <ConnectedRouter history={getHistory()} context={ReactReduxContext as any}>
            <RouterOutlet routes={routes} />
        </ConnectedRouter>
    );
}
