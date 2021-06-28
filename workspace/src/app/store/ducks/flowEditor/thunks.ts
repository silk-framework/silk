import { requestSearchList } from "@ducks/workspace/requests";
import { createAsyncThunk } from "@reduxjs/toolkit";
import { TASK_SEARCH_IDS } from "./typings";

export const requestSearchTask = createAsyncThunk(TASK_SEARCH_IDS.searchList, requestSearchList);
