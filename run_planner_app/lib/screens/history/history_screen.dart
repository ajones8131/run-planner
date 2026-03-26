import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../models/workout.dart';
import '../../providers/workout_provider.dart';
import '../../theme/app_theme.dart';

class HistoryScreen extends StatefulWidget {
  final void Function(WorkoutResponse workout)? onWorkoutTap;
  const HistoryScreen({super.key, this.onWorkoutTap});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  @override
  void initState() {
    super.initState();
    context.read<WorkoutProvider>().loadWorkouts();
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<WorkoutProvider>();
    return Scaffold(
      appBar: AppBar(title: const Text('History')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(AppTheme.spacingSm),
            child: SegmentedButton<WorkoutFilter>(
              segments: const [
                ButtonSegment(
                  value: WorkoutFilter.all,
                  label: Text('All'),
                ),
                ButtonSegment(
                  value: WorkoutFilter.matched,
                  label: Text('Matched'),
                ),
                ButtonSegment(
                  value: WorkoutFilter.flagged,
                  label: Text('Flagged'),
                ),
              ],
              selected: {provider.filter},
              onSelectionChanged: (v) => provider.setFilter(v.first),
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: provider.loading
                ? const Center(child: CircularProgressIndicator())
                : provider.workouts.isEmpty
                    ? const Center(
                        child: Text(
                          'No workouts yet',
                          style: TextStyle(color: AppTheme.textSecondary),
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: () => provider.loadWorkouts(),
                        child: _buildWorkoutList(provider.workouts),
                      ),
          ),
        ],
      ),
    );
  }

  Widget _buildWorkoutList(List<WorkoutResponse> workouts) {
    final grouped = <String, List<WorkoutResponse>>{};
    for (final w in workouts) {
      grouped
          .putIfAbsent(
            DateFormat('yyyy-MM-dd').format(w.startedAt),
            () => [],
          )
          .add(w);
    }
    final sortedKeys = grouped.keys.toList()..sort((a, b) => b.compareTo(a));

    return ListView.builder(
      itemCount: sortedKeys.length,
      itemBuilder: (context, index) {
        final dateKey = sortedKeys[index];
        final dayWorkouts = grouped[dateKey]!;
        final date = DateTime.parse(dateKey);
        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);
        final yesterday = today.subtract(const Duration(days: 1));
        String dateLabel;
        if (date.isAtSameMomentAs(today)) {
          dateLabel = 'Today';
        } else if (date.isAtSameMomentAs(yesterday)) {
          dateLabel = 'Yesterday';
        } else {
          dateLabel = DateFormat('MMM d').format(date);
        }

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(
                left: AppTheme.spacingMd,
                top: AppTheme.spacingMd,
                bottom: AppTheme.spacingXs,
              ),
              child: Text(
                dateLabel,
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textSecondary,
                ),
              ),
            ),
            ...dayWorkouts.map((w) => _buildWorkoutRow(w)),
          ],
        );
      },
    );
  }

  Widget _buildWorkoutRow(WorkoutResponse workout) {
    final km = (workout.distanceMeters / 1000).toStringAsFixed(2);
    final pace = _formatPace(workout.paceMinPerKm);
    return InkWell(
      onTap: () => widget.onWorkoutTap?.call(workout),
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppTheme.spacingMd,
          vertical: AppTheme.spacingSm,
        ),
        decoration: const BoxDecoration(
          border: Border(
            bottom: BorderSide(color: AppTheme.divider, width: 0.5),
          ),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '$km km \u00b7 $pace/km',
                    style: const TextStyle(
                      fontSize: 16,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                  if (workout.avgHr != null)
                    Text(
                      '${workout.avgHr} bpm avg',
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                ],
              ),
            ),
            const Icon(
              Icons.chevron_right,
              color: AppTheme.textSecondary,
              size: 20,
            ),
          ],
        ),
      ),
    );
  }

  String _formatPace(double minPerKm) {
    final minutes = minPerKm.floor();
    final seconds = ((minPerKm - minutes) * 60).round();
    return '$minutes:${seconds.toString().padLeft(2, '0')}';
  }
}
