import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/vdot.dart';
import '../theme/app_theme.dart';

class VdotChart extends StatelessWidget {
  final List<VdotHistoryResponse> history;
  final void Function(VdotHistoryResponse entry)? onPointTap;

  const VdotChart({super.key, required this.history, this.onPointTap});

  @override
  Widget build(BuildContext context) {
    if (history.isEmpty) {
      return const SizedBox(height: 200, child: Center(child: Text('No VDOT history yet', style: TextStyle(color: AppTheme.textSecondary))));
    }
    final accepted = history.where((e) => e.accepted).toList()..sort((a, b) => a.calculatedAt.compareTo(b.calculatedAt));
    if (accepted.isEmpty) {
      return const SizedBox(height: 200, child: Center(child: Text('No accepted VDOT data', style: TextStyle(color: AppTheme.textSecondary))));
    }

    final spots = accepted.asMap().entries.map((e) => FlSpot(e.key.toDouble(), e.value.newVdot)).toList();
    final minY = spots.map((s) => s.y).reduce((a, b) => a < b ? a : b) - 2;
    final maxY = spots.map((s) => s.y).reduce((a, b) => a > b ? a : b) + 2;

    return SizedBox(
      height: 200,
      child: LineChart(LineChartData(
        minY: minY, maxY: maxY,
        gridData: const FlGridData(show: false),
        titlesData: FlTitlesData(
          topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          bottomTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, getTitlesWidget: (value, meta) {
            final idx = value.toInt();
            if (idx < 0 || idx >= accepted.length) return const SizedBox.shrink();
            return Padding(padding: const EdgeInsets.only(top: 8), child: Text(DateFormat('MMM').format(accepted[idx].calculatedAt), style: const TextStyle(fontSize: 10, color: AppTheme.textSecondary)));
          })),
          leftTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 35, getTitlesWidget: (value, meta) {
            return Text(value.toInt().toString(), style: const TextStyle(fontSize: 10, color: AppTheme.textSecondary));
          })),
        ),
        borderData: FlBorderData(show: false),
        lineBarsData: [LineChartBarData(
          spots: spots, isCurved: true, color: AppTheme.primary, barWidth: 2,
          dotData: FlDotData(show: true, getDotPainter: (spot, _, __, ___) => FlDotCirclePainter(radius: 4, color: AppTheme.primary, strokeWidth: 2, strokeColor: Colors.white)),
          belowBarData: BarAreaData(show: true, color: AppTheme.primary.withValues(alpha: 0.1)),
        )],
        lineTouchData: LineTouchData(
          touchCallback: (event, response) {
            if (event is FlTapUpEvent && response?.lineBarSpots != null && response!.lineBarSpots!.isNotEmpty && onPointTap != null) {
              final idx = response.lineBarSpots!.first.spotIndex;
              if (idx < accepted.length) onPointTap!(accepted[idx]);
            }
          },
          touchTooltipData: LineTouchTooltipData(getTooltipItems: (spots) {
            return spots.map((spot) => LineTooltipItem('VDOT ${spot.y.toStringAsFixed(1)}', const TextStyle(color: Colors.white, fontWeight: FontWeight.w600))).toList();
          }),
        ),
      )),
    );
  }
}
