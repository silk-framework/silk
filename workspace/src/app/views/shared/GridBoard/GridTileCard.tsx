import React from "react";
import { GridTileTitleIcon } from "./GridTileIcon";

interface GridTileCardProps {
    /** Card title shown in the header bar. Omit for a header-less card. */
    title?: React.ReactNode;
    /** Optional actions rendered on the right of the header bar. */
    actions?: React.ReactNode;
    /** Drop the default content padding (e.g. for an embedded editor / iframe). */
    noPadding?: boolean;
    className?: string;
    children?: React.ReactNode;
    "data-test-id"?: string;
}

/**
 * Lightweight token-styled card used to give a consistent surface to grid tiles whose
 * content does not carry its own {@link Card} chrome (the "Summary" block, the editor).
 * Fills the height of its {@link GridBoard} tile and scrolls its body when needed.
 */
export function GridTileCard({ title, actions, noPadding, className, children, ...rest }: GridTileCardProps) {
    return (
        <div
            data-test-id={rest["data-test-id"]}
            className={
                "flex min-h-0 w-full flex-1 flex-col overflow-hidden rounded-lg border border-border bg-card shadow-xs " +
                (className ?? "")
            }
        >
            {title != null && (
                <div className="flex min-h-14 items-center justify-between gap-2 border-b border-border px-4 py-3">
                    {/* Match the gui-elements `CardTitle` typography (16px semibold, tight tracking) so
                        tile headers are visually identical to the self-carded widgets' `CardHeader`. */}
                    <div className="flex min-w-0 items-center">
                        <GridTileTitleIcon />
                        <h2 className="truncate text-base font-semibold tracking-tight text-foreground">{title}</h2>
                    </div>
                    {actions}
                </div>
            )}
            <div
                className={
                    "min-h-0 flex-1 overflow-auto " + (noPadding ? "" : title != null ? "px-4 pt-2 pb-4" : "p-4")
                }
            >
                {children}
            </div>
        </div>
    );
}

export default GridTileCard;
