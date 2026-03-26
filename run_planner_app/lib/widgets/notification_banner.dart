import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class NotificationBanner extends StatelessWidget {
  final String message;
  final String? actionLabel;
  final VoidCallback? onTap;
  final VoidCallback? onDismiss;

  const NotificationBanner({super.key, required this.message, this.actionLabel, this.onTap, this.onDismiss});

  @override
  Widget build(BuildContext context) {
    return Card(
      color: AppTheme.warning.withValues(alpha: 0.1),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppTheme.radiusMd),
        child: Padding(
          padding: const EdgeInsets.all(AppTheme.spacingSm),
          child: Row(children: [
            const Icon(Icons.warning_amber_rounded, color: AppTheme.warning, size: 20),
            const SizedBox(width: AppTheme.spacingSm),
            Expanded(child: Text(message, style: const TextStyle(fontSize: 14, color: AppTheme.textPrimary))),
            if (actionLabel != null) Text(actionLabel!, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: AppTheme.primary)),
            if (onDismiss != null) IconButton(icon: const Icon(Icons.close, size: 18), onPressed: onDismiss, padding: EdgeInsets.zero, constraints: const BoxConstraints()),
          ]),
        ),
      ),
    );
  }
}
