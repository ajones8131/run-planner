import 'package:flutter_test/flutter_test.dart';

import 'package:run_planner_app/main.dart';

void main() {
  testWidgets('Run Planner app smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const RunPlannerApp());

    expect(find.text('Run Planner'), findsOneWidget);
  });
}
