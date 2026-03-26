import 'package:flutter/material.dart';
import '../models/planned_workout.dart';
import '../theme/app_theme.dart';

class DayStatus {
  final String dayLabel;
  final WorkoutType workoutType;
  final bool isCompleted;
  final bool isMissed;
  final bool isToday;

  const DayStatus({required this.dayLabel, required this.workoutType, this.isCompleted = false, this.isMissed = false, this.isToday = false});
}

class WeekDayIndicators extends StatelessWidget {
  final List<DayStatus> days;
  const WeekDayIndicators({super.key, required this.days});

  @override
  Widget build(BuildContext context) {
    return Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: days.map(_buildDay).toList());
  }

  Widget _buildDay(DayStatus day) {
    return Column(children: [
      Text(day.dayLabel, style: TextStyle(fontSize: 11, fontWeight: day.isToday ? FontWeight.w700 : FontWeight.normal, color: day.isToday ? AppTheme.primary : AppTheme.textSecondary)),
      const SizedBox(height: 4),
      _buildIcon(day),
      const SizedBox(height: 2),
      Text(day.workoutType.abbreviation, style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
    ]);
  }

  Widget _buildIcon(DayStatus day) {
    if (day.isCompleted) return const Icon(Icons.check_circle, color: AppTheme.success, size: 18);
    if (day.isMissed) return const Icon(Icons.cancel, color: AppTheme.error, size: 18);
    if (day.isToday) return const Icon(Icons.circle, color: AppTheme.primary, size: 18);
    return const Icon(Icons.circle_outlined, color: AppTheme.textSecondary, size: 18);
  }
}
