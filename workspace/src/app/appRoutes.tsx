import { RouteProps } from "react-router";
import { lazy } from "react";

const DashboardPage = lazy(() => import("./views/pages/Workspace"));
const ProjectPage = lazy(() => import("./views/pages/Project"));
const DatasetPage = lazy(() => import("./views/pages/Dataset"));
const WorkflowPage = lazy(() => import("./views/pages/Workflow"));
const TransformPage = lazy(() => import("./views/pages/Transform"));
const LinkingPage = lazy(() => import("./views/pages/Linking"));
const TaskPage = lazy(() => import("./views/pages/Task"));
const TaskPluginView = lazy(() => import("./views/pages/TaskPluginView/TaskPluginView"));
const NotFoundPage = lazy(() => import("./views/pages/NotFound"));
const Activities = lazy(() => import("./views/pages/Activities"));

export interface IRouteProps extends RouteProps {
    /** Path of the route. */
    path: string;
    /** If true then only the component is shown without header etc. */
    componentOnly?: boolean;
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
        path: "/projects/:projectId/dataset/:taskId/:tab?",
        component: DatasetPage,
        exact: true,
    },
    {
        path: "/projects/:projectId/workflow/:taskId/:tab?",
        component: WorkflowPage,
        exact: true,
    },
    {
        path: "/projects/:projectId/transform/:taskId/:tab?",
        component: TransformPage,
        exact: true,
    },
    {
        path: "/projects/:projectId/linking/:taskId/:tab?",
        component: LinkingPage,
        exact: true,
    },
    {
        path: "/projects/:projectId/task/:taskId/:tab?",
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
        path: "/projects/:projectId/item/:pluginId/:taskId/view/:viewId",
        component: TaskPluginView,
        exact: true,
        componentOnly: true,
    },
    {
        path: "*",
        component: NotFoundPage,
        exact: false,
    },
];

export default appRoutes;
