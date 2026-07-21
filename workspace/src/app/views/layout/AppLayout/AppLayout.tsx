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
 * AppLayout includes all pages-components and provide
 * the data which based on projectId and taskId
 * @param children
 */
export function AppLayout({ children }: IProps) {
    return (
        <>
            <ApplicationContainer monitorDropzonesFor={["application/reactflow", "Files"]}>
                <GridBoardResetProvider>
                    <shadcn.SidebarProvider>
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
