import { MenuItemProps } from "@eccenca/gui-elements";

interface SelectOptionState {
    id?: string;
    handleFocus?: () => void;
    modifiers: { active: boolean };
}

/** Keep the active descendant on the same element that exposes the option role. */
export const selectOptionProps = ({
    id,
    handleFocus,
    modifiers,
}: SelectOptionState): Pick<
    MenuItemProps,
    "id" | "role" | "roleStructure" | "aria-selected" | "tabIndex" | "onFocus" | "active"
> => ({
    id,
    role: "option",
    roleStructure: "none",
    "aria-selected": modifiers.active,
    tabIndex: -1,
    onFocus: handleFocus,
    active: modifiers.active,
});
