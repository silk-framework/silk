import React from "react";
import { useDispatch } from "react-redux";
import { cn, shadcn } from "@eccenca/gui-elements";
import { commonOp } from "@ducks/common";
import CreateButton from "../../shared/buttons/CreateButton";
import { NotificationsMenu } from "../../shared/ApplicationNotifications/NotificationsMenu";
import { APPLICATION_CORPORATION_NAME, APPLICATION_NAME, APPLICATION_SUITE_NAME } from "../../../constants/base";
import { APP_VIEWHEADER_ID } from "../../shared/PageHeader/PageHeader";
import { useKeyboardHeaderShortcuts } from "./useKeyBoardHeaderShortcuts";
import { AppDispatch } from "store/configureStore";

/**
 * Sticky header inside the `SidebarInset` (shadcn sidebar-07 pattern): sidebar trigger,
 * the page-header portal target (breadcrumbs + title, filled per page via `PageHeader`),
 * and the global actions (create, notifications) at the right.
 */
export function Header() {
    const dispatch = useDispatch<AppDispatch>();

    //general keyboard shortcuts
    useKeyboardHeaderShortcuts();

    const handleCreateDialog = React.useCallback(() => {
        dispatch(commonOp.setSelectedArtefactDType("all"));
    }, []);

    const brandingSuffix =
        APPLICATION_CORPORATION_NAME() || APPLICATION_SUITE_NAME()
            ? ` @ ${APPLICATION_CORPORATION_NAME()} ${APPLICATION_SUITE_NAME()}`
            : "";

    return (
        <header
            className={cn(
                // constant h-12 (the sidebar-07 compact height): the fixed notifications panel is
                // pinned at top-12, so the block's shrink-on-collapse is intentionally not adopted
                "sticky top-0 z-[8000] flex h-12 shrink-0 items-center gap-2 border-b border-border bg-background",
                // notifications-over-modals elevation, see `useApplicationHeaderOverModals`
                "[.eccgui-application--topheader_&]:z-[8002]",
            )}
            aria-label={`${APPLICATION_NAME()}${brandingSuffix}`}
        >
            <div className="flex w-full min-w-0 items-center gap-2 px-4">
                <shadcn.SidebarTrigger className="-ml-1" />
                <shadcn.Separator orientation="vertical" className="mr-2 data-[orientation=vertical]:h-4" />
                <div id={APP_VIEWHEADER_ID} className="flex min-w-0 grow flex-col justify-center" />
                <div className="ml-auto flex shrink-0 items-center gap-2">
                    <CreateButton onClick={handleCreateDialog} />
                    <NotificationsMenu />
                </div>
            </div>
        </header>
    );
}
