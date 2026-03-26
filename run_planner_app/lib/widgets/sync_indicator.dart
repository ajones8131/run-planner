import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class SyncIndicator extends StatelessWidget {
  final DateTime? lastSyncedAt;
  final bool syncing;
  final String? error;

  const SyncIndicator({super.key, this.lastSyncedAt, this.syncing = false, this.error});

  @override
  Widget build(BuildContext context) {
    if (syncing) {
      return const Row(mainAxisSize: MainAxisSize.min, children: [
        SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 1.5)),
        SizedBox(width: 6),
        Text('Syncing...', style: TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
      ]);
    }
    if (error != null) return const Text('Sync failed', style: TextStyle(fontSize: 12, color: AppTheme.error));
    if (lastSyncedAt == null) return const Text('Not synced yet', style: TextStyle(fontSize: 12, color: AppTheme.textSecondary));
    final diff = DateTime.now().difference(lastSyncedAt!);
    return Text('Last synced: ${_formatDuration(diff)} ago', style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary));
  }

  String _formatDuration(Duration diff) {
    if (diff.inMinutes < 1) return 'just now';
    if (diff.inMinutes < 60) return '${diff.inMinutes}m';
    if (diff.inHours < 24) return '${diff.inHours}h';
    return '${diff.inDays}d';
  }
}
