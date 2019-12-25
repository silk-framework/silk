import React, { Suspense } from "react";
import { Redirect, Route, Switch } from "react-router-dom";
import Loading from "./views/components/loading/Loading";
import { useSelector } from "react-redux";
import { IStore } from "./state/typings/IStore";

export default function RouterOutlet({routes}) {
    const pathname = useSelector((state: IStore) => state.router.location.pathname);

    return (
        <Suspense fallback={<Loading/>}>
            <Switch>
                {
                    routes.map(route => {
                        return (
                            <Route
                                key={route.path as string}
                                path={`${pathname}${route.path}`}
                                exact={route.exact}
                                component={route.component}
                            />
                        )
                    })
                }
            </Switch>
        </Suspense>
    )
}
