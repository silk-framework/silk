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

export type IRouteProps = RouteProps & {
    /** Path of the route. */
    path: string;
    /** If true then only the component is shown without header etc. */
    componentOnly?: boolean;
};

const appRoutes: IRouteProps[] = [
    {
        path: "/",
        element: DashboardPage,
    },
    {
        path: "/projects/:projectId",
        element: ProjectPage,
    },
    {
        path: "/projects/:projectId/dataset/:taskId/:tab?",
        element: DatasetPage,
    },
    {
        path: "/projects/:projectId/workflow/:taskId/:tab?",
        element: WorkflowPage,
    },
    {
        path: "/projects/:projectId/transform/:taskId/:tab?",
        element: TransformPage,
    },
    {
        path: "/projects/:projectId/linking/:taskId/:tab?",
        element: LinkingPage,
    },
    {
        path: "/projects/:projectId/task/:taskId/:tab?",
        element: TaskPage,
    },
    {
        path: "/projects/:projectId/activities",
        element: Activities,
    },
    {
        path: "/activities",
        element: Activities,
    },
    {
        path: "/projects/:projectId/item/:pluginId/:taskId/view/:viewId",
        element: TaskPluginView,
        componentOnly: true,
    },
    {
        path: "*",
        element: NotFoundPage,
    },
];

export default appRoutes;
