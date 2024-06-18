import { IActivityStatus } from "@eccenca/gui-elements/src/cmem/ActivityControl/ActivityControlTypes";
import { ActivityAction } from "@eccenca/gui-elements/src/cmem/ActivityControl/SilkActivityControl";

/** Response object of the response array of an activity list request. */
export interface IActivityListEntry {
    // name / ID of the activity
    name: string;
    // Label of the activity
    label: string;
    // Non-singleton activities may have multiple parallel instances while singleton instances always have one instance.
    instances: { id: string }[];
    // Characteristics of an activity.
    activityCharacteristics: IActivityCharacteristics;
    // Meta data of an activity
    metaData?: {
        projectId?: string;
        taskId?: string;
    };
}

/** Characteristics of a specific activity. */
interface IActivityCharacteristics {
    // If this is a main (execution) activity, i.e. it should be preferably shown to the user.
    isMainActivity: boolean;
    // If this is a cached activity, i.e. that it stores cached values (that can become stale) and thus needs to support restarts.
    isCacheActivity: boolean;
}

/** Functions that are called inside an activity control and must be provided from the outside. */
export interface IActivityControlFunctions {
    // Register a callback, so the activity control receives updates
    registerForUpdates: (callback: (status: IActivityStatus) => any) => any;
    // Un-register from updates in case the activity control gets removed
    unregisterFromUpdates: () => any;
    // Executes different activity actions, e.g. start, cancel, restart. Returns false if the execution has failed.
    executeActivityAction: (action: ActivityAction) => boolean | Promise<boolean>;
}

/** Stores the overall status of all caches. */
export interface IActivityCachesOverallStatus {
    // Oldest start time
    oldestStartTime: string | undefined;
    // If there is currently at least one cache activity running
    currentlyExecuting: boolean;
    // The number of failed activities
    failedActivities: number;
}
