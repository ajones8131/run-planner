import 'package:flutter/material.dart';
import '../models/planned_workout.dart';
import '../theme/app_theme.dart';

class WorkoutCard extends StatelessWidget {
  final PlannedWorkoutResponse workout;
  final double? complianceScore;
  final bool isToday;
  final bool isMissed;
  final VoidCallback? onTap;

  const WorkoutCard({super.key, required this.workout, this.complianceScore, this.isToday = false, this.isMissed = false, this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: AppTheme.spacingMd, vertical: AppTheme.spacingSm),
        decoration: BoxDecoration(
          color: isToday ? AppTheme.primaryLight : null,
          border: const Border(bottom: BorderSide(color: AppTheme.divider, width: 0.5)),
        ),
        child: Row(children: [
          _buildStatusIcon(),
          const SizedBox(width: AppTheme.spacingSm),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(workout.workoutType.displayName, style: TextStyle(fontSize: 16, fontWeight: isToday ? FontWeight.w600 : FontWeight.normal, color: AppTheme.textPrimary)),
            if (workout.workoutType != WorkoutType.rest)
              Text(_subtitle(), style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary)),
          ])),
          if (complianceScore != null) _buildComplianceBadge(),
        ]),
      ),
    );
  }

  Widget _buildStatusIcon() {
    if (complianceScore != null) return Icon(Icons.check_circle, color: _complianceColor(), size: 20);
    if (isMissed) return const Icon(Icons.cancel, color: AppTheme.error, size: 20);
    if (isToday) return const Icon(Icons.circle, color: AppTheme.primary, size: 20);
    return const Icon(Icons.circle_outlined, color: AppTheme.textSecondary, size: 20);
  }

  Widget _buildComplianceBadge() {
    final pct = '${(complianceScore! * 100).round()}%';
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(color: _complianceColor().withValues(alpha: 0.1), borderRadius: BorderRadius.circular(12)),
      child: Text(pct, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: _complianceColor())),
    );
  }

  Color _complianceColor() {
    if (complianceScore == null) return AppTheme.textSecondary;
    if (complianceScore! >= 0.8) return AppTheme.success;
    if (complianceScore! >= 0.6) return AppTheme.warning;
    return AppTheme.error;
  }

  String _subtitle() {
    final km = (workout.targetDistanceMeters / 1000).toStringAsFixed(1);
    if (workout.targetPaceMinPerKm != null && workout.targetPaceMaxPerKm != null) {
      return '$km km \u00b7 ${_formatPace(workout.targetPaceMinPerKm!)}-${_formatPace(workout.targetPaceMaxPerKm!)}/km';
    }
    return '$km km';
  }

  String _formatPace(double minPerKm) {
    final minutes = minPerKm.floor();
    final seconds = ((minPerKm - minutes) * 60).round();
    return '$minutes:${seconds.toString().padLeft(2, '0')}';
  }
}
