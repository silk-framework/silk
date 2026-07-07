import React, { useState } from "react";
import { Header } from "../Header/Header";
import { RecentlyViewedModal } from "../../shared/modals/RecentlyViewedModal";
import { ApplicationContainer, ApplicationContent } from "@eccenca/gui-elements";
import { KeyboardShortcutsModal } from "../Header/KeyboardShortcutsModal";
import { fetchStoredThemeMode, setStoredThemeMode, ThemeMode } from "../../../../theme/theme";

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
    const [themeMode, setThemeMode] = useState<ThemeMode>(() => fetchStoredThemeMode());

    const handleThemeModeChange = (mode: ThemeMode) => {
        setThemeMode(mode);
        setStoredThemeMode(mode);
    };

    return (
        <>
            <ApplicationContainer monitorDropzonesFor={["application/reactflow", "Files"]} themeMode={themeMode}>
                <Header
                    isApplicationSidebarExpanded={sideNavExpanded}
                    onClickApplicationSidebarExpand={() => {
                        setSideNavExpanded(!sideNavExpanded);
                    }}
                    themeMode={themeMode}
                    onChangeThemeMode={handleThemeModeChange}
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
