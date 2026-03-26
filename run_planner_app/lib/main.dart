import 'package:flutter/material.dart';

void main() {
  runApp(const RunPlannerApp());
}

class RunPlannerApp extends StatelessWidget {
  const RunPlannerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Run Planner',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const Scaffold(
        body: Center(
          child: Text('Run Planner'),
        ),
      ),
    );
  }
}
