import { commonOp } from "@ducks/common";
import { ConnectedRouter } from "connected-react-router";
import React, { useEffect } from "react";
import { useDispatch } from "react-redux";

import { IRouteProps } from "./appRoutes";
import RouterOutlet from "./RouterOutlet";
import { getHistory } from "./store/configureStore";

interface IProps {
    routes: IRouteProps[];
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
            <RouterOutlet routes={routes} />
        </ConnectedRouter>
    );
}
