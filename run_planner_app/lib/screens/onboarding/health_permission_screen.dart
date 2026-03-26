import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/sync_provider.dart';
import '../../theme/app_theme.dart';

class HealthPermissionScreen extends StatefulWidget {
  final VoidCallback onComplete;
  const HealthPermissionScreen({super.key, required this.onComplete});

  @override
  State<HealthPermissionScreen> createState() => _HealthPermissionScreenState();
}

class _HealthPermissionScreenState extends State<HealthPermissionScreen> {
  bool _requesting = false;

  Future<void> _requestPermissions() async {
    setState(() => _requesting = true);
    try {
      await context.read<SyncProvider>().requestPermissions();
      widget.onComplete();
    } finally {
      if (mounted) setState(() => _requesting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Health Data')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(AppTheme.spacingLg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 40),
              const Icon(Icons.favorite, size: 64, color: AppTheme.error),
              const SizedBox(height: AppTheme.spacingLg),
              const Text(
                'Connect Apple Health',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: AppTheme.spacingMd),
              const Text(
                'Run Planner reads your workout history, heart rate, and VO2 max to calculate your fitness level and match your runs against your training plan.\n\nWe only read data — we never write to Apple Health.',
                style: TextStyle(
                  fontSize: 15,
                  color: AppTheme.textSecondary,
                  height: 1.5,
                ),
                textAlign: TextAlign.center,
              ),
              const Spacer(),
              ElevatedButton(
                onPressed: _requesting ? null : _requestPermissions,
                child: _requesting
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Allow Access'),
              ),
              const SizedBox(height: AppTheme.spacingSm),
              TextButton(
                onPressed: widget.onComplete,
                child: const Text('Skip for Now'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
