import { SERVE_PATH } from "../constants/path";

export const getFullRoutePath = (path: string) => `${SERVE_PATH}${path}`;
