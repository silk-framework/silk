import React from "react";
import { cn, IconButton } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { WorkbenchViewMode } from "../../../../hooks/useStoreGlobalTableSettings";

interface IProps {
    mode: WorkbenchViewMode;
    onChange(mode: WorkbenchViewMode): void;
}

const options: { mode: WorkbenchViewMode; icon: string; labelKey: string; fallback: string; testId: string }[] = [
    { mode: "table", icon: "toggler-table", labelKey: "widget.ViewModeToggle.table", fallback: "Table view", testId: "view-mode-table" },
    { mode: "grid", icon: "module-dashboard", labelKey: "widget.ViewModeToggle.grid", fallback: "Grid view", testId: "view-mode-grid" },
];

/** Segmented control that toggles the workbench result list between the table and grid presentations. */
export default function ViewModeToggle({ mode, onChange }: IProps) {
    const [t] = useTranslation();
    return (
        <div
            className="inline-flex items-center gap-0.5 rounded-lg bg-muted p-0.5"
            data-test-id="view-mode-toggle"
            role="group"
        >
            {options.map((opt) => {
                const active = opt.mode === mode;
                return (
                    <IconButton
                        key={opt.mode}
                        small
                        data-test-id={opt.testId}
                        name={[opt.icon]}
                        text={t(opt.labelKey, opt.fallback)}
                        onClick={() => onChange(opt.mode)}
                        aria-pressed={active}
                        className={cn(
                            "rounded-md",
                            active ? "bg-card text-foreground shadow-sm" : "text-muted-foreground",
                        )}
                    />
                );
            })}
        </div>
    );
}
