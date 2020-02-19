import Toaster from "@wrappers/blueprint/toaster";
import { Position } from "@wrappers/blueprint/constants";

/** Singleton toaster instance. Create separate instances for different options. */
export const AppToaster = Toaster.create({
    className: "toaster-app",
    position: Position.TOP,
});
