import { RouteProps } from "react-router";
import { lazy } from "react";

const DashboardPage = lazy(() => import("./views/pages/Workspace"));
const ProjectPage = lazy(() => import("./views/pages/Project"));
const DatasetPage = lazy(() => import("./views/pages/Dataset"));
const WorkflowPage = lazy(() => import("./views/pages/Workflow"));
const TransformPage = lazy(() => import("./views/pages/Transform"));
const LinkingPage = lazy(() => import("./views/pages/Linking"));
const TaskPage = lazy(() => import("./views/pages/Task"));
const NotFoundPage = lazy(() => import("./views/pages/NotFound"));
const Activities = lazy(() => import("./views/pages/Activities"));

interface IRouteProps extends RouteProps {
    path: string;
}

const appRoutes: IRouteProps[] = [
    {
        path: "/",
        component: DashboardPage,
        exact: true,
    },
    {
        path: "/projects/:projectId",
        component: ProjectPage,
        exact: true,
    },
    {
        path: "/projects/:projectId/dataset/:taskId",
        component: DatasetPage,
        exact: true,
    },
    {
        path: "/projects/:projectId/workflow/:taskId",
        component: WorkflowPage,
        exact: true,
    },
    {
        path: "/projects/:projectId/transform/:taskId",
        component: TransformPage,
        exact: true,
    },
    {
        path: "/projects/:projectId/linking/:taskId",
        component: LinkingPage,
        exact: true,
    },
    {
        path: "/projects/:projectId/task/:taskId",
        component: TaskPage,
        exact: true,
    },
    {
        path: "/projects/:projectId/activities",
        component: Activities,
        exact: true,
    },
    {
        path: "/activities",
        component: Activities,
        exact: true,
    },
    {
        path: "*",
        component: NotFoundPage,
        exact: false,
    },
];

export default appRoutes;
