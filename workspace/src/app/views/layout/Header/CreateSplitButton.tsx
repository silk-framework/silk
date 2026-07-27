import React from "react";
import { useDispatch } from "react-redux";
import { useTranslation } from "react-i18next";
import { cn, Icon, shadcn } from "@eccenca/gui-elements";
import { commonOp } from "@ducks/common";
import { AppDispatch } from "store/configureStore";
import { uppercaseFirstChar } from "../../../utils/transformers";
import { artefactTypes } from "./artefactTypes";
import { headerMenuElevation } from "./headerChrome";

const { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } = shadcn;

/**
 * Split "Create" action (2A design): the brand-orange primary opens the create dialog for all
 * artefact types; the caret opens a dropdown that pre-selects a category. Both dispatch
 * `setSelectedArtefactDType`, which sets the category and opens the dialog in one reducer. The
 * category list, icons and labels come from the shared {@link artefactTypes} registry so the menu
 * reads at a glance and stays in sync with the keyboard shortcuts.
 */
export function CreateSplitButton() {
    const dispatch = useDispatch<AppDispatch>();
    const [t] = useTranslation();

    const openCreateDialog = React.useCallback(
        (dtype: string) => {
            dispatch(commonOp.setSelectedArtefactDType(dtype));
        },
        [dispatch],
    );

    // The caret is only ~32px wide, so a menu anchored to it (align="end") would be much wider than
    // its anchor and spill left past the Create button, reading as detached. Measuring the whole
    // split-button on open and using it as the menu's min-width makes the menu match the button's
    // footprint: right edges stay flush (align="end") and the left edges line up too.
    //
    // Anchoring gotcha: Radix copies the *content's* computed z-index onto its fixed popper wrapper.
    // The shadcn content ships `z-50`, but the header is `z-[var(--z-app-header)]` (elevated over
    // modals), so the top slice of any header menu — the part that overlaps the header band before it
    // clears the bottom edge — was painted *behind* the header, making the menu read as detached from
    // the button and "stuck to the header". `headerMenuElevation` lifts header menus above the chrome.
    const createSplitButtonRef = React.useRef<HTMLDivElement>(null);
    const [createMenuMinWidth, setCreateMenuMinWidth] = React.useState<number | undefined>(undefined);

    return (
        <div ref={createSplitButtonRef} className="flex items-stretch">
            <button
                type="button"
                data-test-id="create-item-btn"
                onClick={() => openCreateDialog("all")}
                className={cn(
                    "flex h-9 cursor-pointer items-center gap-1.5 rounded-l-lg bg-brand pl-3 pr-3.5 text-sm font-semibold",
                    "text-brand-foreground transition-[filter] hover:brightness-95 focus-visible:outline-none",
                    "focus-visible:ring-2 focus-visible:ring-brand/50",
                )}
            >
                <Icon name="item-add-artefact" small />
                <span>{t("common.action.create", "Create")}</span>
            </button>
            <DropdownMenu
                onOpenChange={(open) => {
                    if (open && createSplitButtonRef.current) {
                        setCreateMenuMinWidth(createSplitButtonRef.current.offsetWidth);
                    }
                }}
            >
                <DropdownMenuTrigger asChild>
                    <button
                        type="button"
                        aria-label={t("common.action.create", "Create") + " …"}
                        className={cn(
                            "flex h-9 w-8 cursor-pointer items-center justify-center rounded-r-lg border-l border-brand-foreground/25",
                            "bg-brand text-brand-foreground transition-[filter] hover:brightness-95",
                            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/50",
                        )}
                    >
                        <Icon name="toggler-caretdown" small />
                    </button>
                </DropdownMenuTrigger>
                {/* Brand-orange menu so the caret dropdown reads as one control with the Create button.
                    On the orange surface the text/icons are the dark `brand-foreground`; the highlighted
                    (hover/keyboard) row inverts to a solid `brand-foreground` fill with the label + icons
                    flipped back to the orange `brand` — a high-contrast active state. The label is a text
                    node so it just inherits the row's `focus:text-brand`, but the icon is an `<svg>`
                    element, and shadcn's base recolours descendant svgs on focus via
                    `not-data-[variant=destructive]:focus:**:text-accent-foreground`. We must reuse that
                    EXACT variant prefix (`not-data-[variant=destructive]:focus:**:`) so tailwind-merge
                    replaces shadcn's class outright — a plain `focus:**:` is a different variant set, stays
                    alongside it, and loses on specificity. */}
                <DropdownMenuContent
                    align="end"
                    sideOffset={4}
                    style={createMenuMinWidth ? { minWidth: createMenuMinWidth } : undefined}
                    className={cn(
                        "min-w-44 bg-brand text-brand-foreground ring-brand-foreground/15",
                        headerMenuElevation,
                    )}
                >
                    {artefactTypes.map((type) => (
                        <DropdownMenuItem
                            key={type.dtype}
                            onClick={() => openCreateDialog(type.dtype)}
                            className="text-brand-foreground focus:bg-brand-foreground focus:text-brand not-data-[variant=destructive]:focus:**:text-brand"
                        >
                            <Icon name={[type.icon]} small className="text-brand-foreground" />
                            {uppercaseFirstChar(t(type.labelKey))}
                        </DropdownMenuItem>
                    ))}
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
}
