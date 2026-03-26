import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../models/planned_workout.dart';
import '../../models/workout.dart';
import '../../theme/app_theme.dart';
import '../../widgets/compliance_bars.dart';

class WorkoutDetailScreen extends StatelessWidget {
  final PlannedWorkoutResponse? plannedWorkout;
  final WorkoutResponse? actualWorkout;
  final double? complianceScore;
  final double? distanceScore;
  final double? paceScore;
  final double? hrScore;

  const WorkoutDetailScreen({
    super.key,
    this.plannedWorkout,
    this.actualWorkout,
    this.complianceScore,
    this.distanceScore,
    this.paceScore,
    this.hrScore,
  });

  @override
  Widget build(BuildContext context) {
    final title = plannedWorkout?.workoutType.displayName ?? 'Workout';
    final date = plannedWorkout?.scheduledDate ?? actualWorkout?.startedAt;

    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppTheme.spacingMd),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (date != null)
              Text(
                DateFormat('EEEE, MMM d').format(date),
                style: const TextStyle(
                  fontSize: 16,
                  color: AppTheme.textSecondary,
                ),
              ),
            const SizedBox(height: AppTheme.spacingLg),
            if (plannedWorkout != null) ...[
              _buildSectionHeader('PLANNED'),
              const SizedBox(height: AppTheme.spacingSm),
              _buildRow(
                'Distance',
                '${(plannedWorkout!.targetDistanceMeters / 1000).toStringAsFixed(1)} km',
              ),
              if (plannedWorkout!.targetPaceMinPerKm != null)
                _buildRow(
                  'Pace',
                  '${_formatPace(plannedWorkout!.targetPaceMinPerKm!)} - ${_formatPace(plannedWorkout!.targetPaceMaxPerKm!)}/km',
                ),
              if (plannedWorkout!.targetHrZone != null)
                _buildRow('HR Zone', '${plannedWorkout!.targetHrZone} bpm'),
              const SizedBox(height: AppTheme.spacingLg),
            ],
            if (actualWorkout != null) ...[
              _buildSectionHeader('ACTUAL'),
              const SizedBox(height: AppTheme.spacingSm),
              _buildRow(
                'Distance',
                '${(actualWorkout!.distanceMeters / 1000).toStringAsFixed(2)} km',
              ),
              _buildRow(
                'Pace',
                '${_formatPace(actualWorkout!.paceMinPerKm)}/km',
              ),
              if (actualWorkout!.avgHr != null)
                _buildRow('Avg HR', '${actualWorkout!.avgHr} bpm'),
              if (actualWorkout!.maxHr != null)
                _buildRow('Max HR', '${actualWorkout!.maxHr} bpm'),
              const SizedBox(height: AppTheme.spacingLg),
            ],
            if (complianceScore != null)
              ComplianceBars(
                overallScore: complianceScore!,
                distanceScore: distanceScore ?? 0,
                paceScore: paceScore ?? 0,
                hrScore: hrScore ?? 0,
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String text) => Text(
        text,
        style: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          letterSpacing: 1,
          color: AppTheme.textSecondary,
        ),
      );

  Widget _buildRow(String label, String value) => Padding(
        padding: const EdgeInsets.only(bottom: AppTheme.spacingSm),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              label,
              style: const TextStyle(
                fontSize: 15,
                color: AppTheme.textSecondary,
              ),
            ),
            Text(
              value,
              style: const TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w500,
                color: AppTheme.textPrimary,
              ),
            ),
          ],
        ),
      );

  String _formatPace(double minPerKm) {
    final minutes = minPerKm.floor();
    final seconds = ((minPerKm - minutes) * 60).round();
    return '$minutes:${seconds.toString().padLeft(2, '0')}';
  }
}
