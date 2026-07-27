import React from "react";
import { ApplicationContainer, shadcn } from "@eccenca/gui-elements";
import { Header } from "../Header/Header";
import { AppSidebar } from "../AppSidebar/AppSidebar";
import { QuickSearchDialog } from "../../shared/QuickSearch";
import { CreateArtefactModal } from "../../shared/modals/CreateArtefactModal/CreateArtefactModal";
import { KeyboardShortcutsModal } from "../Header/KeyboardShortcutsModal";
import { GridBoardResetProvider } from "../../shared/GridBoard";

interface IProps {
    children: React.ReactNode;
}

/**
 * The gui-elements `SidebarProvider` writes the `sidebar_state` cookie on toggle but never reads
 * it back on load (that restore happens server-side in the Next.js shadcn template, which we
 * don't have). So read it here and feed it as `defaultOpen`. Fallback = closed: the sidebar
 * stays collapsed unless the user has explicitly opened it before.
 */
export const readStoredSidebarOpen = (): boolean => {
    if (typeof document === "undefined") return false;
    const match = document.cookie.match(/(?:^|;\s*)sidebar_state=(true|false)/);
    return match ? match[1] === "true" : false;
};

/**
 * AppLayout includes all pages-components and provide
 * the data which based on projectId and taskId
 * @param children
 */
export function AppLayout({ children }: IProps) {
    // Read once on mount; the provider owns the state and rewrites the cookie on every toggle.
    const [defaultSidebarOpen] = React.useState(readStoredSidebarOpen);
    return (
        <>
            <ApplicationContainer monitorDropzonesFor={["application/reactflow", "Files"]}>
                <GridBoardResetProvider>
                    <shadcn.SidebarProvider defaultOpen={defaultSidebarOpen}>
                        <AppSidebar />
                        <shadcn.SidebarInset>
                            <Header />
                            <div className="diapp__insetcontent min-w-0 flex-1 p-4">{children}</div>
                        </shadcn.SidebarInset>
                    </shadcn.SidebarProvider>
                </GridBoardResetProvider>
            </ApplicationContainer>
            <CreateArtefactModal />
            <QuickSearchDialog />
            <KeyboardShortcutsModal />
        </>
    );
}
