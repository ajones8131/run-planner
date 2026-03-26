import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/widgets/race_countdown_card.dart';

void main() {
  group('RaceCountdownCard', () {
    testWidgets('displays race name and date', (tester) async {
      await tester.pumpWidget(const MaterialApp(home: Scaffold(body: RaceCountdownCard(raceName: 'Chicago Marathon', raceDate: '2026-10-12', progressPercent: 0.71))));
      expect(find.text('Chicago Marathon'), findsOneWidget);
      expect(find.textContaining('Oct 12'), findsOneWidget);
    });

    testWidgets('shows progress percentage', (tester) async {
      await tester.pumpWidget(const MaterialApp(home: Scaffold(body: RaceCountdownCard(raceName: 'Test Race', raceDate: '2026-12-01', progressPercent: 0.50))));
      expect(find.textContaining('50%'), findsOneWidget);
    });
  });
}
