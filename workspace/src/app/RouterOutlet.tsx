import React, { Component, Suspense } from "react";
import { Redirect, Route, Switch } from "react-router-dom";
import Loading from "./views/components/loading/Loading";

class RouterOutlet extends Component<any, any> {
    render() {
        return (
            <Suspense fallback={<Loading/>}>
                <Switch>
                    {
                        this.props.routes.map(route => {
                            return (
                                <Route
                                    key={route.path as string}
                                    path={route.path}
                                    exact={route.exact}
                                    component={route.component}
                                />
                            )
                        })
                    }
                    <Redirect to={'/'} from={'*'}/>
                </Switch>
            </Suspense>
        )
    }
}

export default RouterOutlet;
