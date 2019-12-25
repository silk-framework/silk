import Toaster from "@wrappers/toaster";
import { Position } from "@wrappers/constants";

/** Singleton toaster instance. Create separate instances for different options. */
export const AppToaster = Toaster.create({
    className: "toaster-app",
    position: Position.TOP,
});
