import React, { useState } from "react";
import { Header } from "../Header/Header";
import { RecentlyViewedModal } from "../../shared/modals/RecentlyViewedModal";
import { ApplicationContainer, ApplicationContent } from "@eccenca/gui-elements";
import { KeyboardShortcutsModal } from "../Header/KeyboardShortcutsModal";

interface IProps {
    children: React.ReactNode;
}

/**
 * Below the application header, above the content: where a toolbar panel sits.
 * Header height is `mini-units(8)` of gui-elements, the layer is carbon's `z('header')`.
 */
const PANEL_CONTAINER_STYLE = {
    position: "fixed",
    top: "4rem",
    right: 0,
    bottom: 0,
    zIndex: 8000,
} as const;

/**
 * The width of what a toolbar panel docked into the container.
 * The panel decides that width; the content makes room for it.
 */
const usePanelContainerWidth = (panelContainer: HTMLElement | null): number => {
    const [width, setWidth] = useState(0);

    React.useEffect(() => {
        if (!panelContainer) {
            return;
        }
        const observer = new ResizeObserver(([entry]) => {
            setWidth(entry?.borderBoxSize[0]?.inlineSize ?? 0);
        });
        observer.observe(panelContainer);
        return () => observer.disconnect();
    }, [panelContainer]);

    return width;
};

/**
 * AppLayout includes all pages-components and provide
 * the data which based on projectId and taskId
 * @param children
 */
export function AppLayout({ children }: IProps) {
    const [sideNavExpanded, setSideNavExpanded] = useState(false);
    const [panelContainer, setPanelContainer] = useState<HTMLElement | null>(null);
    const panelWidth = usePanelContainerWidth(panelContainer);

    return (
        <>
            <ApplicationContainer monitorDropzonesFor={["application/reactflow", "Files"]}>
                <Header
                    isApplicationSidebarExpanded={sideNavExpanded}
                    onClickApplicationSidebarExpand={() => {
                        setSideNavExpanded(!sideNavExpanded);
                    }}
                    panelContainer={panelContainer}
                />
                <ApplicationContent
                    isApplicationSidebarExpanded={sideNavExpanded}
                    isApplicationSidebarRail={!sideNavExpanded}
                    htmlMainProps={{ style: { marginRight: panelWidth || undefined } }}
                >
                    {children}
                </ApplicationContent>
                <div ref={setPanelContainer} data-test-id="toolbar-panel-container" style={PANEL_CONTAINER_STYLE} />
            </ApplicationContainer>
            <RecentlyViewedModal />
            <KeyboardShortcutsModal />
        </>
    );
}
