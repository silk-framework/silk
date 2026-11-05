import React, { Suspense } from "react";
import { Route, Switch } from "react-router-dom";
import Loading from "./views/shared/Loading";
import { getFullRoutePath } from "./utils/routerUtils";
import { AppLayout } from "./views/layout/AppLayout/AppLayout";
import { useTranslation } from "react-i18next";
import { IRouteProps } from "./appRoutes";
import { ApplicationContainer, ApplicationContent } from "@eccenca/gui-elements";

interface RouterOutletProps {
    routes: IRouteProps[];
}

export default function RouterOutlet({ routes }: RouterOutletProps) {
    const [t] = useTranslation();
    const componentOnlyRoutes = routes.filter((route) => route.componentOnly);
    const layoutRoutes = routes.filter((route) => !route.componentOnly);

    return (
        <Suspense fallback={<Loading posGlobal description={t("common.app.loading", "Loading page.")} />}>
            <Switch>
                {componentOnlyRoutes.map((route) => {
                    const Component = route.component as React.ComponentType | undefined;
                    return (
                        <Route key={route.path} path={getFullRoutePath(route.path)} exact={route.exact}>
                            {Component && (
                                <ApplicationContainer monitorDropzonesFor={["application/reactflow", "Files"]}>
                                    <ApplicationContent>
                                        <Component />
                                    </ApplicationContent>
                                </ApplicationContainer>
                            )}
                        </Route>
                    );
                })}
                <Route>
                    <AppLayout>
                        <Switch>
                            {layoutRoutes.map((route) => {
                                const Component = route.component as React.ComponentType | undefined;
                                return (
                                    <Route key={route.path} path={getFullRoutePath(route.path)} exact={route.exact}>
                                        {Component && <Component />}
                                    </Route>
                                );
                            })}
                        </Switch>
                    </AppLayout>
                </Route>
            </Switch>
        </Suspense>
    );
}
