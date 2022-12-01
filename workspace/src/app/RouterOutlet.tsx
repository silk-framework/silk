import { ApplicationContainer, ApplicationContent } from "@eccenca/gui-elements";
import React, { Suspense } from "react";
import { useTranslation } from "react-i18next";
import { Route, Switch } from "react-router-dom";

import { IRouteProps } from "./appRoutes";
import { getFullRoutePath } from "./utils/routerUtils";
import { AppLayout } from "./views/layout/AppLayout/AppLayout";
import Loading from "./views/shared/Loading";

interface RouterOutletProps {
    routes: IRouteProps[];
}

export default function RouterOutlet({ routes }: RouterOutletProps) {
    const [t] = useTranslation();
    return (
        <Suspense fallback={<Loading posGlobal description={t("common.app.loading", "Loading page.")} />}>
            <Switch>
                {routes.map((route) => {
                    const Component = route.component as any;
                    return (
                        <Route key={route.path} path={getFullRoutePath(route.path)} exact={route.exact}>
                            {route.componentOnly && Component ? (
                                <ApplicationContainer>
                                    <ApplicationContent>
                                        <Component />
                                    </ApplicationContent>
                                </ApplicationContainer>
                            ) : (
                                <AppLayout>{Component && <Component />}</AppLayout>
                            )}
                        </Route>
                    );
                })}
            </Switch>
        </Suspense>
    );
}
