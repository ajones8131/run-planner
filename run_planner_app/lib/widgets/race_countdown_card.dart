import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../theme/app_theme.dart';

class RaceCountdownCard extends StatelessWidget {
  final String raceName;
  final String raceDate;
  final double progressPercent;

  const RaceCountdownCard({super.key, required this.raceName, required this.raceDate, required this.progressPercent});

  @override
  Widget build(BuildContext context) {
    final date = DateTime.parse(raceDate);
    final daysAway = date.difference(DateTime.now()).inDays;
    final formattedDate = DateFormat('MMM d').format(date);
    final percentText = '${(progressPercent * 100).round()}%';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppTheme.spacingMd),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(raceName, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: AppTheme.textPrimary)),
            const SizedBox(height: AppTheme.spacingXs),
            Text('$daysAway days away \u00b7 $formattedDate', style: const TextStyle(fontSize: 14, color: AppTheme.textSecondary)),
            const SizedBox(height: AppTheme.spacingSm),
            Row(children: [
              Expanded(child: ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(value: progressPercent, minHeight: 8, backgroundColor: AppTheme.divider, valueColor: const AlwaysStoppedAnimation<Color>(AppTheme.primary)),
              )),
              const SizedBox(width: AppTheme.spacingSm),
              Text('$percentText through', style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
            ]),
          ],
        ),
      ),
    );
  }
}
