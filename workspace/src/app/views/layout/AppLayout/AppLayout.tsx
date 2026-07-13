import React from "react";
import { ApplicationContainer, shadcn } from "@eccenca/gui-elements";
import { Header } from "../Header/Header";
import { AppSidebar } from "../AppSidebar/AppSidebar";
import { RecentlyViewedModal } from "../../shared/modals/RecentlyViewedModal";
import { CreateArtefactModal } from "../../shared/modals/CreateArtefactModal/CreateArtefactModal";
import { KeyboardShortcutsModal } from "../Header/KeyboardShortcutsModal";

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
                <shadcn.SidebarProvider>
                    <AppSidebar />
                    <shadcn.SidebarInset>
                        <Header />
                        <div className="diapp__insetcontent min-w-0 flex-1 p-3.5">{children}</div>
                    </shadcn.SidebarInset>
                </shadcn.SidebarProvider>
            </ApplicationContainer>
            <CreateArtefactModal />
            <RecentlyViewedModal />
            <KeyboardShortcutsModal />
        </>
    );
}
