import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class ComplianceBars extends StatelessWidget {
  final double distanceScore;
  final double paceScore;
  final double hrScore;
  final double overallScore;

  const ComplianceBars({super.key, required this.distanceScore, required this.paceScore, required this.hrScore, required this.overallScore});

  @override
  Widget build(BuildContext context) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      _buildOverall(),
      const SizedBox(height: AppTheme.spacingMd),
      _buildBar('Distance', distanceScore),
      const SizedBox(height: AppTheme.spacingSm),
      _buildBar('Pace', paceScore),
      const SizedBox(height: AppTheme.spacingSm),
      _buildBar('HR Zone', hrScore),
    ]);
  }

  Widget _buildOverall() {
    final pct = '${(overallScore * 100).round()}%';
    return Row(children: [
      const Text('COMPLIANCE', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, letterSpacing: 1, color: AppTheme.textSecondary)),
      const SizedBox(width: AppTheme.spacingSm),
      Text(pct, style: TextStyle(fontSize: 24, fontWeight: FontWeight.w700, color: _scoreColor(overallScore))),
    ]);
  }

  Widget _buildBar(String label, double score) {
    final pct = '${(score * 100).round()}%';
    return Row(children: [
      SizedBox(width: 70, child: Text(label, style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary))),
      Expanded(child: ClipRRect(
        borderRadius: BorderRadius.circular(4),
        child: LinearProgressIndicator(value: score, minHeight: 8, backgroundColor: AppTheme.divider, valueColor: AlwaysStoppedAnimation<Color>(_scoreColor(score))),
      )),
      const SizedBox(width: AppTheme.spacingSm),
      SizedBox(width: 35, child: Text(pct, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary))),
    ]);
  }

  Color _scoreColor(double score) {
    if (score >= 0.8) return AppTheme.success;
    if (score >= 0.6) return AppTheme.warning;
    return AppTheme.error;
  }
}
