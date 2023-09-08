import React, { useState } from "react";
import { Header } from "../Header/Header";
import { RecentlyViewedModal } from "../../shared/modals/RecentlyViewedModal";
import { ApplicationContainer, ApplicationContent } from "@eccenca/gui-elements";
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
    const [sideNavExpanded, setSideNavExpanded] = useState(false);

    return (
        <>
            <ApplicationContainer>
                <Header
                    isApplicationSidebarExpanded={sideNavExpanded}
                    onClickApplicationSidebarExpand={() => {
                        setSideNavExpanded(!sideNavExpanded);
                    }}
                />
                <ApplicationContent
                    isApplicationSidebarExpanded={sideNavExpanded}
                    isApplicationSidebarRail={!sideNavExpanded}
                >
                    {children}
                </ApplicationContent>
            </ApplicationContainer>
            <RecentlyViewedModal />
            <KeyboardShortcutsModal />
        </>
    );
}
