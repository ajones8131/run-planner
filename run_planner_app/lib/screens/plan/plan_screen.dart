import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../models/planned_workout.dart';
import '../../providers/plan_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/workout_card.dart';

class PlanScreen extends StatefulWidget {
  final void Function(PlannedWorkoutResponse workout)? onWorkoutTap;
  const PlanScreen({super.key, this.onWorkoutTap});

  @override
  State<PlanScreen> createState() => _PlanScreenState();
}

class _PlanScreenState extends State<PlanScreen> {
  late PageController _pageController;
  int _currentWeekOffset = 0;

  @override
  void initState() {
    super.initState();
    final planProvider = context.read<PlanProvider>();
    _currentWeekOffset = planProvider.currentWeek - 1;
    _pageController = PageController(initialPage: _currentWeekOffset);
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final planProvider = context.watch<PlanProvider>();
    final plan = planProvider.activePlan;
    if (plan == null) {
      return const Scaffold(body: Center(child: Text('No active plan')));
    }
    final totalWeeks = planProvider.totalWeeks;

    return Scaffold(
      appBar: AppBar(title: const Text('Training Plan')),
      body: Column(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppTheme.spacingMd,
              vertical: AppTheme.spacingSm,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                IconButton(
                  icon: const Icon(Icons.chevron_left),
                  onPressed: _currentWeekOffset > 0
                      ? () {
                          _pageController.previousPage(
                            duration: const Duration(milliseconds: 300),
                            curve: Curves.easeInOut,
                          );
                        }
                      : null,
                ),
                Column(
                  children: [
                    Text(
                      'Week ${_currentWeekOffset + 1} of $totalWeeks',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    Text(
                      _weekDateRange(_currentWeekOffset, plan.startDate),
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  ],
                ),
                IconButton(
                  icon: const Icon(Icons.chevron_right),
                  onPressed: _currentWeekOffset < totalWeeks - 1
                      ? () {
                          _pageController.nextPage(
                            duration: const Duration(milliseconds: 300),
                            curve: Curves.easeInOut,
                          );
                        }
                      : null,
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: PageView.builder(
              controller: _pageController,
              itemCount: totalWeeks,
              onPageChanged: (index) =>
                  setState(() => _currentWeekOffset = index),
              itemBuilder: (context, weekIndex) =>
                  _buildWeekPage(weekIndex, plan),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildWeekPage(int weekIndex, dynamic plan) {
    final weekNumber = weekIndex + 1;
    final weekWorkouts =
        (plan.workouts as List<PlannedWorkoutResponse>)
            .where((w) => w.weekNumber == weekNumber)
            .toList()
          ..sort((a, b) => a.dayOfWeek.compareTo(b.dayOfWeek));
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final dayLabels = [
      'Monday',
      'Tuesday',
      'Wednesday',
      'Thursday',
      'Friday',
      'Saturday',
      'Sunday',
    ];

    return ListView.builder(
      itemCount: weekWorkouts.length,
      itemBuilder: (context, index) {
        final workout = weekWorkouts[index];
        final isToday = workout.scheduledDate.isAtSameMomentAs(today);
        final isMissed = workout.scheduledDate.isBefore(today) &&
            workout.workoutType != WorkoutType.rest;
        final dayName = dayLabels[(workout.dayOfWeek - 1) % 7];
        final dateStr = DateFormat('MMM d').format(workout.scheduledDate);

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(
                left: AppTheme.spacingMd,
                top: AppTheme.spacingSm,
                bottom: AppTheme.spacingXs,
              ),
              child: Text(
                '$dayName \u00b7 $dateStr',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: isToday ? AppTheme.primary : AppTheme.textSecondary,
                ),
              ),
            ),
            WorkoutCard(
              workout: workout,
              isToday: isToday,
              isMissed: isMissed,
              onTap: () => widget.onWorkoutTap?.call(workout),
            ),
          ],
        );
      },
    );
  }

  String _weekDateRange(int weekOffset, DateTime planStart) {
    final weekStart = planStart.add(Duration(days: weekOffset * 7));
    final weekEnd = weekStart.add(const Duration(days: 6));
    return '${DateFormat('MMM d').format(weekStart)} - ${DateFormat('MMM d').format(weekEnd)}';
  }
}
