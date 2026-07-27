import React, { useState } from "react";
import NotFound from "../../pages/NotFound";
import { ProjectForbiddenNotification } from "../ProjectForbiddenNotification";

/**
 * Shared "task not found / access forbidden" guard for the task-shaped detail pages
 * (Transform, Linking, Task, Dataset, Workflow, RuleBlock). Each of those pages otherwise
 * repeats the same `notFound`/`forbidden` state pair plus the early-return branch.
 *
 * Usage:
 * ```tsx
 * const { guardElement, notFoundCallback, forbiddenCallback } = useTaskPageGuards();
 * // wire the callbacks into the actions tile …
 * if (guardElement) return guardElement;
 * ```
 *
 * `forbidden` takes precedence over `notFound`, matching the original per-page branches.
 */
export function useTaskPageGuards(): {
    guardElement: React.ReactElement | null;
    notFoundCallback: (notFound: boolean) => void;
    forbiddenCallback: (forbidden: boolean) => void;
} {
    const [notFound, setNotFound] = useState(false);
    const [forbidden, setForbidden] = useState(false);

    // Built with `createElement` (not JSX) so this stays a `.ts` module.
    const guardElement = forbidden
        ? React.createElement(ProjectForbiddenNotification)
        : notFound
          ? React.createElement(NotFound)
          : null;

    return { guardElement, notFoundCallback: setNotFound, forbiddenCallback: setForbidden };
}
