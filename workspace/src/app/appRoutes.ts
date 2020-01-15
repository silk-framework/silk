import { lazy } from "react";
import { RouteProps } from "react-router";

const DashboardPage = lazy(() => import('./views/pages/dashboard/Dashboard'));

const appRoutes: RouteProps[] = [
    {
        path: "/",
        exact: true,
        component: DashboardPage,
    },
    {
        path: "/project/:projectId",
        component: DashboardPage,
        exact: true
    },
];

export default appRoutes;
