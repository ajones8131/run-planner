import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../models/planned_workout.dart';
import '../../providers/plan_provider.dart';
import '../../providers/sync_provider.dart';
import '../../providers/user_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/notification_banner.dart';
import '../../widgets/race_countdown_card.dart';
import '../../widgets/sync_indicator.dart';
import '../../widgets/week_day_indicators.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final planProvider = context.watch<PlanProvider>();
    final syncProvider = context.watch<SyncProvider>();
    final userProvider = context.watch<UserProvider>();
    final plan = planProvider.activePlan;
    final goalRace = planProvider.activeGoalRace;
    final todayWorkout = planProvider.todayWorkout;
    final flagged = userProvider.flaggedEntries;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Run Planner'),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: AppTheme.spacingMd),
            child: SyncIndicator(
              lastSyncedAt: syncProvider.lastSyncedAt,
              syncing: syncProvider.syncing,
              error: syncProvider.error,
            ),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          await Future.wait([
            planProvider.loadActivePlan(),
            syncProvider.sync(),
            userProvider.loadVdotHistory(),
          ]);
        },
        child: ListView(
          padding: const EdgeInsets.symmetric(vertical: AppTheme.spacingSm),
          children: [
            if (flagged.isNotEmpty)
              NotificationBanner(
                message:
                    'New fitness estimate: VDOT ${flagged.first.previousVdot.toStringAsFixed(1)} \u2192 ${flagged.first.newVdot.toStringAsFixed(1)}',
                actionLabel: 'Review',
              ),
            if (goalRace != null && plan != null)
              RaceCountdownCard(
                raceName: goalRace.distanceLabel,
                raceDate: goalRace.raceDate.toIso8601String().split('T')[0],
                progressPercent: planProvider.progressPercent,
              ),
            _buildTodayCard(todayWorkout),
            if (plan != null) _buildWeekSummary(planProvider),
          ],
        ),
      ),
    );
  }

  Widget _buildTodayCard(PlannedWorkoutResponse? workout) {
    final dayName = DateFormat('EEEE').format(DateTime.now());
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppTheme.spacingMd),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'TODAY \u00b7 $dayName',
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                letterSpacing: 1,
                color: AppTheme.textSecondary,
              ),
            ),
            const SizedBox(height: AppTheme.spacingSm),
            if (workout == null)
              const Text(
                'No workout scheduled',
                style: TextStyle(fontSize: 16, color: AppTheme.textSecondary),
              )
            else if (workout.workoutType == WorkoutType.rest)
              const Text(
                'Rest Day',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textPrimary,
                ),
              )
            else ...[
              Text(
                workout.workoutType.displayName,
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: AppTheme.spacingXs),
              Text(
                _workoutSubtitle(workout),
                style: const TextStyle(
                  fontSize: 15,
                  color: AppTheme.textSecondary,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildWeekSummary(PlanProvider planProvider) {
    final plan = planProvider.activePlan!;
    final now = DateTime.now();
    final weekNumber = planProvider.currentWeek;
    final totalWeeks = planProvider.totalWeeks;
    final weekStart = now.subtract(Duration(days: now.weekday - 1));
    final dayLabels = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];

    final days = List.generate(7, (i) {
      final day = weekStart.add(Duration(days: i));
      final dayDate = DateTime(day.year, day.month, day.day);
      final isToday =
          dayDate.isAtSameMomentAs(DateTime(now.year, now.month, now.day));
      PlannedWorkoutResponse? pw;
      try {
        pw = plan.workouts.firstWhere(
          (w) => w.scheduledDate.isAtSameMomentAs(dayDate),
        );
      } catch (_) {}
      return DayStatus(
        dayLabel: dayLabels[i],
        workoutType: pw?.workoutType ?? WorkoutType.rest,
        isToday: isToday,
        isCompleted:
            day.isBefore(now) && !isToday && pw != null,
      );
    });

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppTheme.spacingMd),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'This Week \u00b7 Week $weekNumber of $totalWeeks',
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                letterSpacing: 1,
                color: AppTheme.textSecondary,
              ),
            ),
            const SizedBox(height: AppTheme.spacingMd),
            WeekDayIndicators(days: days),
          ],
        ),
      ),
    );
  }

  String _workoutSubtitle(PlannedWorkoutResponse workout) {
    final km = (workout.targetDistanceMeters / 1000).toStringAsFixed(1);
    final parts = <String>['$km km'];
    if (workout.targetPaceMinPerKm != null &&
        workout.targetPaceMaxPerKm != null) {
      parts.add(
        '${_formatPace(workout.targetPaceMinPerKm!)}-${_formatPace(workout.targetPaceMaxPerKm!)}/km',
      );
    }
    if (workout.targetHrZone != null) {
      parts.add('HR ${workout.targetHrZone} bpm');
    }
    return parts.join(' \u00b7 ');
  }

  String _formatPace(double minPerKm) {
    final minutes = minPerKm.floor();
    final seconds = ((minPerKm - minutes) * 60).round();
    return '$minutes:${seconds.toString().padLeft(2, '0')}';
  }
}
