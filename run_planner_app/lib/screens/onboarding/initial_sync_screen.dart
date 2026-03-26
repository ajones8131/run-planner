import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/sync_provider.dart';
import '../../theme/app_theme.dart';

class InitialSyncScreen extends StatefulWidget {
  final VoidCallback onComplete;
  const InitialSyncScreen({super.key, required this.onComplete});

  @override
  State<InitialSyncScreen> createState() => _InitialSyncScreenState();
}

class _InitialSyncScreenState extends State<InitialSyncScreen> {
  @override
  void initState() {
    super.initState();
    _runSync();
  }

  Future<void> _runSync() async {
    final syncProvider = context.read<SyncProvider>();
    await syncProvider.sync(since: DateTime.now().subtract(const Duration(days: 90)));
    if (mounted) widget.onComplete();
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: EdgeInsets.all(AppTheme.spacingLg),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                CircularProgressIndicator(),
                SizedBox(height: AppTheme.spacingLg),
                Text(
                  'Pulling your recent workouts...',
                  style: TextStyle(fontSize: 18, color: AppTheme.textPrimary),
                ),
                SizedBox(height: AppTheme.spacingSm),
                Text(
                  'This may take a moment',
                  style: TextStyle(fontSize: 14, color: AppTheme.textSecondary),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
