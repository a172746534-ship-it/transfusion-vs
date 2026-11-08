# BASELINE: the reference, before the desktop port

Taken from tag `v0.6.0` (commit 300b2c8, checked by the run itself) by `.github/workflows/desktop-reference.yml`,
pushed in commit 945b8de on branch `desktop` (docs/DESKTOP_APP.md §7, step 2). Nothing of the desktop app existed yet.

- **Run: 42461704759** (job `reference`, ubuntu-latest, Temurin 21, Gradle 9.6.1), 2026-11-08.
- Artifact `reference-baseline` (90-day retention): `app/build/test-results/`, `app/build/audit/`, `ktlint-report/`, `summary/`.
- **The audit set is the visual reference** for the desktop screenshot tests and the design reviewer. It stays that run's artifact, outside git.

## Summary

- **JVM tests (`testDebugUnitTest`): 241 in 22 classes: 241 PASSED.** 0 failures, 0 errors, 0 skipped.
- **ktlint 1.8.0:** fails. 6442 errors in 89 files over `app/src/**/*.kt` (3375 in `app/src/main`). Per file below: the desktop port compares a class (a) or (b) file against its own count here.
- **Audit set:** 262 PNGs: 65 screens × 4 variants (`w360_f10`, `w360_f20`, `w412_f10`, `w412_f10_night`) + 2 Arabic-phone shots (`w360_f10_ar`).

## UI checks, tap budget and robustness: the root SCORECARD.md's last run

These are emulator results; desktop-reference.yml has no emulator. They are copied from the root files, which stay read-only.

- **Last run:** custom timer full run 1, lab **42231693326**, commit 272e1e7 (root SCORECARD.md, "Custom timer, full run 1").
  - UI checks: **TRUNCATED 0; MIDWORD 1 at font 2.0; SPACING 8–9.** The other categories (TOUCH, NOLABEL, SMALLTEXT, PRIMARY, ATF) are 0, as in root BASELINE.md.
  - Per variant (root BASELINE.md, lab 42186380405): w360 f1.3 SPACING 8–9; w360 f2.0 SPACING 8–9 and MIDWORD 1; w412 f1.0 SPACING 9.
  - Tap budget: tasks 1–12 within budget on every API that ran (10: 3, 11: 1, 12: 3), also at 360 dp.
  - Monkey: 0 crashes, 0 ANR. Leaks: 0.
- **What fails in the reference itself** (docs/DESKTOP_APP.md trap 16): ktlint (above), SPACING 8–9 and MIDWORD 1 at font 2.0. On desktop these block only if the result is worse than here (§8).

## Every test class, method and result (run 42461704759)

### app.transfusionvs.alarm.CustomEngineTest (17: 17 PASSED)

| Method | Result |
|---|---|
| `collision_undoOnEachOne_restoresOnlyItsOwnRow` | PASSED |
| `startingMakesTheFirstAlarmOneIntervalLater_andArmsIt` | PASSED |
| `stopLeavesHomeAndCancelsAlarms_undoPutsItBackExactly` | PASSED |
| `collision_cancelTouchesOnlyItsOwnAddress` | PASSED |
| `theSameName_caseAndSpacesIgnored_opensTheExistingTimerAndChangesNothing` | PASSED |
| `doneThenUndo_isTheExactRow` | PASSED |
| `undoingAStartRemovesTheTimer` | PASSED |
| `collision_snoozingACheckpoint_leavesThePendingCustomAlarm` | PASSED |
| `collision_doneOnEachOne_leavesTheOtherTwoExactly` | PASSED |
| `aStoppedTimerIsGoneADayLater_withItsMrnAndName` | PASSED |
| `collision_preAlertsGoToTheirOwnType` | PASSED |
| `masksCountCustomMrns_soTheSuffixStaysUnique` | PASSED |
| `aNewInterval_reschedulesFromNow_neverInThePast` | PASSED |
| `theSameNameOnAnotherPatient_andSeveralTimersForOnePatient_areAllowed` | PASSED |
| `collision_snoozeWritesOnlyWhatIsSounding` | PASSED |
| `aRenameOrAnotherMrn_keepsTheSchedule` | PASSED |
| `collision_stopAndEditOfTheCustomTimer_leaveTheOtherTwo` | PASSED |

### app.transfusionvs.alarm.UndoEngineTest (11: 11 PASSED)

| Method | Result |
|---|---|
| `cantTurnThenUndoRestoresTheSlot` | PASSED |
| `stoppingAPlanThenUndoKeepsItRunning` | PASSED |
| `undoingAStartRemovesTheBed` | PASSED |
| `anExpiredEntryChangesNothing` | PASSED |
| `turnedThenUndoRestoresSlotAndPosition` | PASSED |
| `allTurnedMovesEveryBedAndUndoesAsOne` | PASSED |
| `aRepeatedAcknowledgeChangesNothingMoreAndAddsNoEntry` | PASSED |
| `acknowledgeThenUndoIsTheExactRow` | PASSED |
| `endThenUndoRestoresTheBedAndItsSchedule` | PASSED |
| `stopForReactionThenUndoRestoresWhatWasRinging` | PASSED |
| `oneBedOfARoundCanGoBackAlone` | PASSED |

### app.transfusionvs.data.MigrationTest (5: 5 PASSED)

| Method | Result |
|---|---|
| `updatingFromV2KeepsEverythingAndAddsBedsAndUndo` | PASSED |
| `oldAlarmAddressesStillParse_andTurnAlarmsHaveTheirOwn` | PASSED |
| `updatingFromV1KeepsRunningTransfusionsAndPendingCheckpoints` | PASSED |
| `updatingFromV3KeepsEverythingAndAddsCustomTimers` | PASSED |
| `customAlarmsHaveTheirOwnAddress` | PASSED |

### app.transfusionvs.schedule.BedMaskTest (5: 5 PASSED)

| Method | Result |
|---|---|
| `theSuffixGrowsOnlyAsFarAsItMustToStayUnique` | PASSED |
| `realBedLabelsAreLeftAlone` | PASSED |
| `theWholeNumberNeverShows` | PASSED |
| `itWorksInsideASentenceAndInArabicDigits` | PASSED |
| `aLongNumberShowsItsLastFourDigits` | PASSED |

### app.transfusionvs.schedule.CustomRulesTest (19: 19 PASSED)

| Method | Result |
|---|---|
| `aNameIsOneLine_upTo30Characters_countedFromTheFirstVisibleOne` | PASSED |
| `aSnoozedAlarmIsQuietUntilItsTime` | PASSED |
| `nothingAtOrAfterTheStopTime` | PASSED |
| `everyRequestIsACustomOne_andThePreAlertOnlyWhenSet` | PASSED |
| `aLateDoneDoesNotMoveTheSeries` | PASSED |
| `anIntervalShorterThanTheMissedTimeout_closesAtTheNextAlarm` | PASSED |
| `noAlarmBeforeTheSeriesStart` | PASSED |
| `dueRings_timeoutSilences_nextAlarmCloses` | PASSED |
| `theSameName_ignoresCaseSpacesAndComposition` | PASSED |
| `statusWordsMatchTheOtherTypes` | PASSED |
| `materializeCreatesUpToTheFirstFutureAlarm_andNothingWhileItIsThere` | PASSED |
| `aNewIntervalStartsFromTheLastDone_andNeverRingsInThePast` | PASSED |
| `alarmsThatPassedWhileThePhoneWasOff_comeBackMissed_exceptTheOneStillOpen` | PASSED |
| `aStoppedTimerMakesNothing` | PASSED |
| `emptyAndInvisibleNamesAreRejected` | PASSED |
| `theSameInterval_isTheSameTimer` | PASSED |
| `snoozeIsOfferedOnlyIfItEndsBeforeTheNextAlarm_andWithinTheLimit` | PASSED |
| `theFirstAlarmIsOneIntervalAfterTheStart_neverAtTheStart` | PASSED |
| `autoStopOnlyAfterTheStopTime_withNothingOpen` | PASSED |

### app.transfusionvs.schedule.CustomSchedulePropertyTest (7: 7 PASSED)

| Method | Result |
|---|---|
| `snoozeNeverPassesTheNextAlarm` | PASSED |
| `dstNeverShiftsSkipsOrDoublesAnAlarm` | PASSED |
| `missedExactlyWhenTheNextAlarmFires` | PASSED |
| `editingTheIntervalNeverRingsInThePast_andARenameNeverReschedules` | PASSED |
| `theFirstAlarmIsOneIntervalAfterTheStart` | PASSED |
| `aShiftWithGapsLateDonesAndSnoozes` | PASSED |
| `aNextAlarmIsAlwaysArmed_forEveryInterval` | PASSED |

### app.transfusionvs.schedule.CustomViewsTest (6: 6 PASSED)

| Method | Result |
|---|---|
| `upcoming_soon_due_overdue_andStopped_inTheSameWordsAsTheOtherCards` | PASSED |
| `withNoCustomAlarm_theRingIsAsBefore` | PASSED |
| `severalCustomAlarmsInTheSameMinute_earliestStartFirst` | PASSED |
| `customOnly_titlesTheScreen_andMaskingCoversTheName` | PASSED |
| `alarmsOfDifferentMinutes_theMostOverdueFirst` | PASSED |
| `atAnEqualRankAndTime_customComesAfterTheOthers_whoseOrderDoesNotChange` | PASSED |

### app.transfusionvs.schedule.ScheduleCalculatorTest (17: 17 PASSED)

| Method | Result |
|---|---|
| `snoozedCheckpointIsSilentUntilSnoozeEnds` | PASSED |
| `startIsLoggedWithoutAnAlarm` | PASSED |
| `clockJumpsForward_passedCheckpointsRingAsOneImmediateAlarm` | PASSED |
| `t0IsTruncatedToTheWholeMinute` | PASSED |
| `customDurationGetsItsOwnEndCheck` | PASSED |
| `checkpointsInTheSameMinuteAcrossBedsRingTogether` | PASSED |
| `limitWarningDefaultsTo15MinBefore4h_andIsConfigurable` | PASSED |
| `editingStartTime_keepsDoneRows_andMovesPendingOnes` | PASSED |
| `preAlertIsScheduledBeforeEachFutureCheckpoint` | PASSED |
| `plannedEndReplacesTheHourlyCheckOnTheSameMinute` | PASSED |
| `runningPastThePlannedEnd_hourlyChecksContinueAtTheSameMinute` | PASSED |
| `timezoneChangeKeepsTheSameInstants_onlyTheDisplayedClockChanges` | PASSED |
| `example1020_matchesTheCard` | PASSED |
| `editingDuration_movesTheEndCheck_withoutDoubleAlarms` | PASSED |
| `missedTimerCountsFromTheFirstRing_snoozeDoesNotExtendIt` | PASSED |
| `daylightSavingShiftKeepsHourlyChecksOnTheSameMinute_60RealMinutesApart` | PASSED |
| `pastStartTime_passedCheckpointsAreOverdue_futureOnesScheduled` | PASSED |

### app.transfusionvs.schedule.SchedulePropertyTest (9: 9 PASSED)

| Method | Result |
|---|---|
| `t0IsAWholeMinute` | PASSED |
| `transfusionPlanFollowsTheCard` | PASSED |
| `endOfShiftIsTheNextShiftChange` | PASSED |
| `longestGapIsBounded` | PASSED |
| `aLateTurnNeverMovesTheGrid` | PASSED |
| `anExcludedPositionIsNeverSuggested` | PASSED |
| `noAlarmAtOrAfterTheStopTime` | PASSED |
| `slotsSitOnTheWardClockAndAreAlwaysAhead` | PASSED |
| `firstSlotKeepsThe30MinuteRule` | PASSED |

### app.transfusionvs.schedule.TimelineTest (8: 8 PASSED)

| Method | Result |
|---|---|
| `statusOnTimeLateMissed` | PASSED |
| `liveRowShowsDoneOverdueAndUpcoming` | PASSED |
| `liveRowShowsNextHourlyOncePastThePlannedEnd` | PASSED |
| `countdownWording` | PASSED |
| `durationWording` | PASSED |
| `pickedStartTimeIsNeverInTheFuture` | PASSED |
| `previewMatchesTheCardRow` | PASSED |
| `endVsPromptNeverRings` | PASSED |

### app.transfusionvs.schedule.TurnGridTest (13: 13 PASSED)

| Method | Result |
|---|---|
| `endOfMyShiftIsTheNext08Or20` | PASSED |
| `noSlotAtOrAfterTheStopTime` | PASSED |
| `dstSpringForward_theMissingSlotIsSkipped` | PASSED |
| `dstFallBack_theRepeatedHourRingsOnce` | PASSED |
| `aMissedSlotClosesWhenTheNextSlotFires` | PASSED |
| `rotationCyclesAndNeverSuggestsAnExcludedPosition` | PASSED |
| `aLateAckDoesNotShiftTheSchedule` | PASSED |
| `remindIn30OnlyIfItLandsBeforeTheNextSlot` | PASSED |
| `startingMidSlot_firstTurnIsTheNextSlot_unlessUnder30MinAway` | PASSED |
| `evenHourGridAcrossMidnight` | PASSED |
| `longestGapIsComputedFromActualTurnTimes` | PASSED |
| `oneAndFourHourGridsKeep08And20` | PASSED |
| `timezoneChange_theGridFollowsThePhonesLocalClock` | PASSED |

### app.transfusionvs.schedule.TurnRulesTest (10: 10 PASSED)

| Method | Result |
|---|---|
| `remindIn30RingsAgainWithAFreshTimer` | PASSED |
| `theOverdueTimerStaysArmedAfterStartRound` | PASSED |
| `theMissedTimerStopsTheSound_butTheSlotStaysOpen` | PASSED |
| `afterTheAppWasOff_closedSlotsAreMissed_andOnlyTheCurrentOneRings` | PASSED |
| `aPlanRunningOverADayDoesNotRecreateCleanedUpSlotsAsMissed` | PASSED |
| `slotsInTheSameMinuteAcrossBedsRingTogether` | PASSED |
| `noSlotAtOrAfterTheStopTime_andThePlanStopsItself` | PASSED |
| `aLateAckDoesNotShift_theNextAlarmIsStillTheNextGridSlot` | PASSED |
| `anUnacknowledgedSlotBecomesMissedWhenTheNextSlotFires_andTheNewSlotRings` | PASSED |
| `aTimezoneChangeMovesUpcomingSlotsOntoTheNewLocalGrid` | PASSED |

### app.transfusionvs.schedule.UrgencyTest (14: 14 PASSED)

| Method | Result |
|---|---|
| `alarmRailPointsAtThisCheck` | PASSED |
| `nightFollowsTheClockAcrossMidnight` | PASSED |
| `theSummaryTellsTheTruthOfTheWard` | PASSED |
| `aTimezoneChangeMovesAnUnfiredFirstSlotOntoTheNewGrid` | PASSED |
| `calmSaysWhenInWords` | PASSED |
| `turnPlanOverdueOpenStopped` | PASSED |
| `theTemperatureFollowsTheTimeLeft` | PASSED |
| `theMrnIsDigitsOnly` | PASSED |
| `railFillsTowardTheNextDot` | PASSED |
| `pocketGuardIgnoresTheFirstSecondAndACoveredPhone` | PASSED |
| `missedCheckIsOverdueWithItsLateness` | PASSED |
| `stoppedForReactionIsAboveEverything` | PASSED |
| `undoStepsSurviveTheirTextForm` | PASSED |
| `mostUrgentFirstThenSoonest` | PASSED |

### app.transfusionvs.screenshots.ArabicPhoneTest (4: 4 PASSED)

| Method | Result |
|---|---|
| `theAlarmStaysLeftToRightWithItsWordsInOrder` | PASSED |
| `alarmAsSeenOnAnArabicPhone` | PASSED |
| `theManifestDoesNotAskForMirroring` | PASSED |
| `homeAsSeenOnAnArabicPhone` | PASSED |

### app.transfusionvs.screenshots.AuditScreenshots (46: 46 PASSED)

| Method | Result |
|---|---|
| `alarm_done_undo` | PASSED |
| `start_bed_picked` | PASSED |
| `start_edit` | PASSED |
| `turn_detail_on_track` | PASSED |
| `start_empty` | PASSED |
| `detail_upcoming` | PASSED |
| `detail_on_time_and_late` | PASSED |
| `detail_ringing` | PASSED |
| `widget` | PASSED |
| `home_due_ringing` | PASSED |
| `alarm_two_beds` | PASSED |
| `home_end_vs_pending` | PASSED |
| `round_alarm` | PASSED |
| `round_seven` | PASSED |
| `turn_edit` | PASSED |
| `alarm_snoozed` | PASSED |
| `setup` | PASSED |
| `undo_bar_on_home` | PASSED |
| `home_one_bed_upcoming` | PASSED |
| `turn_start_options` | PASSED |
| `detail_stopped` | PASSED |
| `turn_start` | PASSED |
| `home_setup_warning` | PASSED |
| `turn_start_bed_picked` | PASSED |
| `reaction_20_beds` | PASSED |
| `reaction_one_bed` | PASSED |
| `round_checklist` | PASSED |
| `alarm_one_bed` | PASSED |
| `home_six_beds` | PASSED |
| `home_only_turning` | PASSED |
| `home_overdue_missed` | PASSED |
| `turn_detail_skipped` | PASSED |
| `arabic_phone` | PASSED |
| `start_options` | PASSED |
| `home_empty` | PASSED |
| `home_stopped_reaction` | PASSED |
| `turn_detail_stopped` | PASSED |
| `alarm_combined` | PASSED |
| `settings` | PASSED |
| `detail_overdue_missed` | PASSED |
| `alarm_no_more_snooze` | PASSED |
| `home_20_beds` | PASSED |
| `home_both_features` | PASSED |
| `reaction_no_bed` | PASSED |
| `detail_ended_prompt` | PASSED |
| `turn_detail_overdue` | PASSED |

### app.transfusionvs.screenshots.ClinicalContentTest (3: 3 PASSED)

| Method | Result |
|---|---|
| `alarmCarriesTheFrontOfTheCard` | PASSED |
| `reactionGuideMatchesTheBackOfTheCard` | PASSED |
| `timesKeepLatinDigitsOnAnArabicPhone` | PASSED |

### app.transfusionvs.screenshots.CustomNameFitTest (4: 4 PASSED)

| Method | Result |
|---|---|
| `theWidestName_onTheCard_isNeverClipped` | PASSED |
| `aTypical30CharacterName_onTheAlarm_takesAtMostTwoLines` | PASSED |
| `aTypical30CharacterName_onTheCard_takesAtMostTwoLines` | PASSED |
| `theWidestName_onTheAlarm_takesAtMostThreeLines_neverClipped` | PASSED |

### app.transfusionvs.screenshots.CustomScreenshots (20: 20 PASSED)

| Method | Result |
|---|---|
| `custom_alarm_long_name` | PASSED |
| `custom_edit` | PASSED |
| `custom_detail_stopped` | PASSED |
| `custom_detail_overdue` | PASSED |
| `home_with_custom` | PASSED |
| `custom_alarm_with_round` | PASSED |
| `custom_start_empty` | PASSED |
| `custom_start_other` | PASSED |
| `custom_start_valid` | PASSED |
| `custom_detail_upcoming` | PASSED |
| `custom_start_long_name` | PASSED |
| `custom_alarm_one` | PASSED |
| `custom_detail_stop_set` | PASSED |
| `custom_alarm_with_transfusion_and_round` | PASSED |
| `home_only_custom` | PASSED |
| `custom_alarm_several` | PASSED |
| `custom_widget` | PASSED |
| `custom_detail_ringing` | PASSED |
| `custom_alarm_wide_name` | PASSED |
| `custom_alarm_with_transfusion` | PASSED |

### app.transfusionvs.screenshots.RepositioningScreenshots (9: 9 PASSED)

| Method | Result |
|---|---|
| `homeOnlyRepositioning` | PASSED |
| `detail` | PASSED |
| `roundAlarm` | PASSED |
| `edit` | PASSED |
| `home` | PASSED |
| `start` | PASSED |
| `roundSevenBeds` | PASSED |
| `roundWithTransfusion` | PASSED |
| `roundChecklist` | PASSED |

### app.transfusionvs.screenshots.TransfusionScreenshots (10: 10 PASSED)

| Method | Result |
|---|---|
| `detail` | PASSED |
| `alarmTwoBeds` | PASSED |
| `reaction` | PASSED |
| `alarmOneBed` | PASSED |
| `edit` | PASSED |
| `home` | PASSED |
| `setup` | PASSED |
| `start` | PASSED |
| `settings` | PASSED |
| `homeEmpty` | PASSED |

### app.transfusionvs.ui.CustomDetailGuardTest (1: 1 PASSED)

| Method | Result |
|---|---|
| `stopWorksAtOnce_butNotRightAfterDone` | PASSED |

### app.transfusionvs.ui.WardColorsTest (3: 3 PASSED)

| Method | Result |
|---|---|
| `tokensMatchTheVerifiedPalette` | PASSED |
| `nightMeetsTheThresholds` | PASSED |
| `dayMeetsTheThresholds` | PASSED |

## ktlint 1.8.0 errors per file (run 42461704759)

| File | Errors |
|---|---|
| `app/src/androidTest/java/app/transfusionvs/uicheck/Robustness.kt` | 94 |
| `app/src/androidTest/java/app/transfusionvs/uicheck/TapBudget.kt` | 144 |
| `app/src/androidTest/java/app/transfusionvs/uicheck/UiChecks.kt` | 464 |
| `app/src/debug/java/app/transfusionvs/lab/LabReceiver.kt` | 179 |
| `app/src/main/java/app/transfusionvs/App.kt` | 2 |
| `app/src/main/java/app/transfusionvs/alarm/AlarmActivity.kt` | 330 |
| `app/src/main/java/app/transfusionvs/alarm/AlarmPlayer.kt` | 30 |
| `app/src/main/java/app/transfusionvs/alarm/AlarmScheduler.kt` | 30 |
| `app/src/main/java/app/transfusionvs/alarm/AlarmService.kt` | 17 |
| `app/src/main/java/app/transfusionvs/alarm/CustomSource.kt` | 22 |
| `app/src/main/java/app/transfusionvs/alarm/Engine.kt` | 270 |
| `app/src/main/java/app/transfusionvs/alarm/Haptics.kt` | 1 |
| `app/src/main/java/app/transfusionvs/alarm/Notifications.kt` | 92 |
| `app/src/main/java/app/transfusionvs/alarm/PocketGuard.kt` | 5 |
| `app/src/main/java/app/transfusionvs/alarm/Receivers.kt` | 21 |
| `app/src/main/java/app/transfusionvs/alarm/RingSpec.kt` | 27 |
| `app/src/main/java/app/transfusionvs/alarm/TurnSource.kt` | 52 |
| `app/src/main/java/app/transfusionvs/alarm/Undo.kt` | 130 |
| `app/src/main/java/app/transfusionvs/data/AppDatabase.kt` | 4 |
| `app/src/main/java/app/transfusionvs/data/Custom.kt` | 4 |
| `app/src/main/java/app/transfusionvs/data/Dao.kt` | 2 |
| `app/src/main/java/app/transfusionvs/data/Entities.kt` | 7 |
| `app/src/main/java/app/transfusionvs/data/Settings.kt` | 8 |
| `app/src/main/java/app/transfusionvs/data/TurnDao.kt` | 1 |
| `app/src/main/java/app/transfusionvs/data/Turns.kt` | 5 |
| `app/src/main/java/app/transfusionvs/data/Ward.kt` | 8 |
| `app/src/main/java/app/transfusionvs/schedule/ArmPlanner.kt` | 49 |
| `app/src/main/java/app/transfusionvs/schedule/BedMask.kt` | 3 |
| `app/src/main/java/app/transfusionvs/schedule/CustomRules.kt` | 83 |
| `app/src/main/java/app/transfusionvs/schedule/Labels.kt` | 40 |
| `app/src/main/java/app/transfusionvs/schedule/ScheduleCalculator.kt` | 23 |
| `app/src/main/java/app/transfusionvs/schedule/Timeline.kt` | 58 |
| `app/src/main/java/app/transfusionvs/schedule/TurnGrid.kt` | 45 |
| `app/src/main/java/app/transfusionvs/schedule/TurnRules.kt` | 55 |
| `app/src/main/java/app/transfusionvs/schedule/TurnViews.kt` | 34 |
| `app/src/main/java/app/transfusionvs/schedule/Urgency.kt` | 131 |
| `app/src/main/java/app/transfusionvs/setup/OemGuide.kt` | 19 |
| `app/src/main/java/app/transfusionvs/setup/SetupChecks.kt` | 21 |
| `app/src/main/java/app/transfusionvs/ui/MainActivity.kt` | 95 |
| `app/src/main/java/app/transfusionvs/ui/MainViewModel.kt` | 24 |
| `app/src/main/java/app/transfusionvs/ui/components/Blocks.kt` | 11 |
| `app/src/main/java/app/transfusionvs/ui/components/Cards.kt` | 123 |
| `app/src/main/java/app/transfusionvs/ui/components/Common.kt` | 163 |
| `app/src/main/java/app/transfusionvs/ui/components/Night.kt` | 5 |
| `app/src/main/java/app/transfusionvs/ui/components/PatientField.kt` | 12 |
| `app/src/main/java/app/transfusionvs/ui/components/Positions.kt` | 11 |
| `app/src/main/java/app/transfusionvs/ui/components/Rail.kt` | 16 |
| `app/src/main/java/app/transfusionvs/ui/components/TaskName.kt` | 27 |
| `app/src/main/java/app/transfusionvs/ui/components/Timeline.kt` | 48 |
| `app/src/main/java/app/transfusionvs/ui/components/UndoBar.kt` | 12 |
| `app/src/main/java/app/transfusionvs/ui/screens/CustomDetailScreen.kt` | 67 |
| `app/src/main/java/app/transfusionvs/ui/screens/CustomStartScreen.kt` | 167 |
| `app/src/main/java/app/transfusionvs/ui/screens/DetailScreen.kt` | 142 |
| `app/src/main/java/app/transfusionvs/ui/screens/HomeScreen.kt` | 66 |
| `app/src/main/java/app/transfusionvs/ui/screens/ReactionScreen.kt` | 61 |
| `app/src/main/java/app/transfusionvs/ui/screens/SettingsScreen.kt` | 121 |
| `app/src/main/java/app/transfusionvs/ui/screens/SetupScreen.kt` | 73 |
| `app/src/main/java/app/transfusionvs/ui/screens/StartScreen.kt` | 66 |
| `app/src/main/java/app/transfusionvs/ui/screens/TurnDetailScreen.kt` | 57 |
| `app/src/main/java/app/transfusionvs/ui/screens/TurnStartScreen.kt` | 153 |
| `app/src/main/java/app/transfusionvs/ui/theme/Theme.kt` | 89 |
| `app/src/main/java/app/transfusionvs/ui/theme/Tokens.kt` | 6 |
| `app/src/main/java/app/transfusionvs/ui/theme/Type.kt` | 84 |
| `app/src/main/java/app/transfusionvs/widget/BedsWidget.kt` | 47 |
| `app/src/sharedTest/java/app/transfusionvs/screenshots/CustomFakes.kt` | 53 |
| `app/src/sharedTest/java/app/transfusionvs/screenshots/Fakes.kt` | 103 |
| `app/src/sharedTest/java/app/transfusionvs/screenshots/TurnFakes.kt` | 82 |
| `app/src/test/java/app/transfusionvs/alarm/CustomEngineTest.kt` | 158 |
| `app/src/test/java/app/transfusionvs/alarm/UndoEngineTest.kt` | 108 |
| `app/src/test/java/app/transfusionvs/data/MigrationTest.kt` | 43 |
| `app/src/test/java/app/transfusionvs/schedule/BedMaskTest.kt` | 1 |
| `app/src/test/java/app/transfusionvs/schedule/CustomRulesTest.kt` | 24 |
| `app/src/test/java/app/transfusionvs/schedule/CustomSchedulePropertyTest.kt` | 102 |
| `app/src/test/java/app/transfusionvs/schedule/CustomViewsTest.kt` | 91 |
| `app/src/test/java/app/transfusionvs/schedule/ScheduleCalculatorTest.kt` | 40 |
| `app/src/test/java/app/transfusionvs/schedule/SchedulePropertyTest.kt` | 60 |
| `app/src/test/java/app/transfusionvs/schedule/TimelineTest.kt` | 75 |
| `app/src/test/java/app/transfusionvs/schedule/TurnGridTest.kt` | 19 |
| `app/src/test/java/app/transfusionvs/schedule/TurnRulesTest.kt` | 75 |
| `app/src/test/java/app/transfusionvs/schedule/UrgencyTest.kt` | 46 |
| `app/src/test/java/app/transfusionvs/screenshots/ArabicPhoneTest.kt` | 42 |
| `app/src/test/java/app/transfusionvs/screenshots/AuditScreenshots.kt` | 434 |
| `app/src/test/java/app/transfusionvs/screenshots/ClinicalContentTest.kt` | 16 |
| `app/src/test/java/app/transfusionvs/screenshots/CustomNameFitTest.kt` | 50 |
| `app/src/test/java/app/transfusionvs/screenshots/CustomScreenshots.kt` | 235 |
| `app/src/test/java/app/transfusionvs/screenshots/RepositioningScreenshots.kt` | 109 |
| `app/src/test/java/app/transfusionvs/screenshots/TransfusionScreenshots.kt` | 156 |
| `app/src/test/java/app/transfusionvs/ui/CustomDetailGuardTest.kt` | 19 |
| `app/src/test/java/app/transfusionvs/ui/WardColorsTest.kt` | 45 |

## The audit set (run 42461704759, `app/build/audit/`)

`alarm_1bed`, `alarm_2beds`, `alarm_done_undo`, `alarm_snooze_used_up`, `alarm_snoozed`, `alarm_transfusion_and_round`, `custom_alarm_1`, `custom_alarm_3`, `custom_alarm_long_name`, `custom_alarm_wide_name`, `custom_alarm_with_round`, `custom_alarm_with_transfusion`, `custom_alarm_with_transfusion_and_round`, `custom_detail_overdue`, `custom_detail_ringing`, `custom_detail_stop_set`, `custom_detail_stopped`, `custom_detail_upcoming`, `custom_edit`, `custom_start_empty`, `custom_start_long_name`, `custom_start_other`, `custom_start_valid`, `custom_widget`, `detail_ended_end_vs`, `detail_on_time_late`, `detail_overdue_missed`, `detail_ringing`, `detail_stopped_reaction`, `detail_upcoming`, `home_1bed_upcoming`, `home_20beds`, `home_6beds`, `home_both_features`, `home_empty`, `home_end_vs_pending`, `home_only_custom`, `home_only_turning`, `home_overdue_missed`, `home_ringing`, `home_setup_warning`, `home_stopped_reaction`, `home_with_custom`, `reaction_1bed`, `reaction_20beds`, `reaction_no_bed`, `round_7beds`, `round_alarm`, `round_checklist`, `settings`, `setup_check`, `start_bed_picked`, `start_edit`, `start_empty`, `start_options`, `turn_detail_on_track`, `turn_detail_overdue_missed`, `turn_detail_skipped`, `turn_detail_stopped`, `turn_edit`, `turn_start`, `turn_start_bed_picked`, `turn_start_options`, `undo_bar_on_home`, `widget`.

Each in `w360_f10`, `w360_f20`, `w412_f10` and `w412_f10_night`; `alarm_1bed` and `home_both_features` also in `w360_f10_ar`.
