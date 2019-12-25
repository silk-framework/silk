import { lazy } from "react";
import { RouteProps } from "react-router";

const DashboardPage = lazy(() => import('./views/pages/dashboard/Dashboard'));
const ProjectPage = lazy(() => import('./views/pages/project/Project'));

const appRoutes: RouteProps[] = [
    {
        path: "/",
        exact: true,
        component: DashboardPage,
    },
    {
        path: "/project/:projectId",
        component: ProjectPage,
    },
];

export default appRoutes;
