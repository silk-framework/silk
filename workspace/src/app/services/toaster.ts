import { Position } from "@eccenca/gui-elements/blueprint/constants";
import Toaster from "@eccenca/gui-elements/blueprint/toaster";

/** Singleton toaster instance. Create separate instances for different options. */
export const AppToaster = Toaster.create({
    position: Position.BOTTOM_RIGHT,
});
