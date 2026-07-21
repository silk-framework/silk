import React from "react";
import { Button, cn, ContextMenu, MenuItem } from "@eccenca/gui-elements";

export interface FilterMenuOption {
    id: string;
    label: string;
    /** Optional number of matching items, shown next to the option label. */
    count?: number;
    /** Optional leading icon-name stack, e.g. the item-type artefact icon (as `createIconNameStack`). */
    icon?: string[];
}

interface FilterMenuProps {
    /** Label shown on the dropdown toggler (e.g. the filter/facet name). */
    label: string;
    options: FilterMenuOption[];
    /** Ids of the currently applied options. */
    selectedIds: string[];
    onToggle(id: string): void;
    /** Whether toggling an option closes the dropdown. Defaults to true (single-select behavior);
     *  multi-select menus should pass false so several options can be toggled in one go. */
    closeOnSelect?: boolean;
    "data-test-id"?: string;
}

/**
 * A single filter rendered as a dropdown "option menu". Used both for the (single-select) item type
 * filter and for the (multi-select) server facets. Renders nothing when there are no options, so
 * a facet only appears once the server returns values for it.
 */
export default function FilterMenu({
    label,
    options,
    selectedIds,
    onToggle,
    closeOnSelect = true,
    "data-test-id": dataTestId,
}: FilterMenuProps) {
    if (!options.length) {
        return null;
    }
    const selectedCount = selectedIds.length;

    const toggler = (
        <Button
            data-test-id={dataTestId}
            variant="outline"
            rightIcon="toggler-caretdown"
            text={selectedCount ? `${label} (${selectedCount})` : label}
            className={cn(
                selectedCount && "border-brand/40 bg-brand/12 text-foreground hover:bg-brand/20 hover:text-foreground",
            )}
        />
    );

    return (
        <ContextMenu togglerElement={toggler} togglerText={label}>
            {options.map((opt) => {
                const active = selectedIds.includes(opt.id);
                return (
                    <MenuItem
                        key={opt.id}
                        active={active}
                        // Options with their own icon (e.g. item types) keep it and rely on the active
                        // highlight for selection; option lists without icons show a checkmark instead.
                        icon={opt.icon ? opt.icon : active ? "state-checkedsimple" : undefined}
                        text={opt.count != null ? `${opt.label} (${opt.count})` : opt.label}
                        onClick={() => onToggle(opt.id)}
                        shouldDismissPopover={closeOnSelect}
                    />
                );
            })}
        </ContextMenu>
    );
}
