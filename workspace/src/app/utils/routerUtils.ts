import { SERVE_PATH } from "../constants";

export const getFullRoutePath = (path: string) => `${SERVE_PATH}${path}`;
