import Toaster from "gui-elements/blueprint/toaster";
import { Position } from "gui-elements/blueprint/constants";

/** Singleton toaster instance. Create separate instances for different options. */
export const AppToaster = Toaster.create({
    position: Position.BOTTOM_RIGHT,
});
