import React from "react";
import { useUpdateNodeInternals } from "react-flow-renderer";

interface InitialNodeInternalsSynchronizationProps {
    reactFlowWrapper: React.MutableRefObject<HTMLElement | null> | null;
    nodeIds: string[];
    nodeGeometryKey: string;
    initializationGeneration: number;
}

/**
 * Synchronizes React Flow v9's cached node geometry once the initial rule editor layout is measurable.
 *
 * This is a workaround for React Flow v9 only. React Flow v12 updates node internals through its own ResizeObserver.
 */
export const useInitialNodeInternalsSynchronization = ({
    reactFlowWrapper,
    nodeIds,
    nodeGeometryKey,
    initializationGeneration,
}: InitialNodeInternalsSynchronizationProps) => {
    const updateNodeInternals = useUpdateNodeInternals();
    const currentNodeIds = React.useRef<string[]>([]);
    const initialNodeInternalsSynchronized = React.useRef(false);
    const lastInitializationGeneration = React.useRef<number | undefined>(undefined);
    currentNodeIds.current = nodeIds;

    React.useEffect(() => {
        if (lastInitializationGeneration.current !== initializationGeneration) {
            lastInitializationGeneration.current = initializationGeneration;
            initialNodeInternalsSynchronized.current = false;
        }
        const canvas = reactFlowWrapper?.current;
        const initialNodeIds = currentNodeIds.current;
        if (initialNodeIds.length === 0) {
            initialNodeInternalsSynchronized.current = false;
            return;
        }
        if (!canvas || initialNodeInternalsSynchronized.current) {
            return;
        }

        let layoutFrame: number | undefined;
        let refreshFrame: number | undefined;
        let resizeObserver: ResizeObserver | undefined;

        const refreshNodeInternals = () => {
            if (
                initialNodeInternalsSynchronized.current ||
                document.visibilityState !== "visible" ||
                layoutFrame !== undefined
            ) {
                return;
            }

            layoutFrame = window.requestAnimationFrame(() => {
                refreshFrame = window.requestAnimationFrame(() => {
                    layoutFrame = undefined;
                    refreshFrame = undefined;

                    const canvasBounds = canvas.getBoundingClientRect();
                    const nodeElements = Array.from(canvas.querySelectorAll<HTMLElement>(".react-flow__node")).filter(
                        (nodeElement) => initialNodeIds.includes(nodeElement.dataset.id ?? ""),
                    );
                    if (
                        canvasBounds.width === 0 ||
                        canvasBounds.height === 0 ||
                        nodeElements.length !== initialNodeIds.length ||
                        nodeElements.some((nodeElement) => {
                            const bounds = nodeElement.getBoundingClientRect();
                            return bounds.width === 0 || bounds.height === 0;
                        })
                    ) {
                        return;
                    }

                    initialNodeIds.forEach(updateNodeInternals);
                    initialNodeInternalsSynchronized.current = true;
                    resizeObserver?.disconnect();
                });
            });
        };

        resizeObserver = typeof ResizeObserver === "undefined" ? undefined : new ResizeObserver(refreshNodeInternals);
        resizeObserver?.observe(canvas);
        Array.from(canvas.querySelectorAll<HTMLElement>(".react-flow__node")).forEach((nodeElement) => {
            resizeObserver?.observe(nodeElement);
        });
        document.addEventListener("visibilitychange", refreshNodeInternals);
        refreshNodeInternals();

        return () => {
            document.removeEventListener("visibilitychange", refreshNodeInternals);
            resizeObserver?.disconnect();
            if (layoutFrame !== undefined) {
                window.cancelAnimationFrame(layoutFrame);
            }
            if (refreshFrame !== undefined) {
                window.cancelAnimationFrame(refreshFrame);
            }
        };
    }, [initializationGeneration, nodeGeometryKey, reactFlowWrapper, updateNodeInternals]);
};
