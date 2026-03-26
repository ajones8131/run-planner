import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../providers/plan_provider.dart';
import '../../providers/user_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/vdot_chart.dart';

class ProfileScreen extends StatefulWidget {
  final VoidCallback? onEditProfile;
  final VoidCallback? onNewGoalRace;
  final VoidCallback? onLogout;
  const ProfileScreen({
    super.key,
    this.onEditProfile,
    this.onNewGoalRace,
    this.onLogout,
  });

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  @override
  void initState() {
    super.initState();
    context.read<UserProvider>().loadVdotHistory();
    context.read<PlanProvider>().loadGoalRaces();
  }

  @override
  Widget build(BuildContext context) {
    final userProvider = context.watch<UserProvider>();
    final planProvider = context.watch<PlanProvider>();
    final user = userProvider.user;

    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: ListView(
        children: [
          Card(
            child: InkWell(
              onTap: widget.onEditProfile,
              child: Padding(
                padding: const EdgeInsets.all(AppTheme.spacingMd),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            user?.name ?? user?.email ?? '',
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w600,
                              color: AppTheme.textPrimary,
                            ),
                          ),
                          const SizedBox(height: AppTheme.spacingXs),
                          Text(
                            [
                              if (userProvider.currentVdot != null)
                                'VDOT: ${userProvider.currentVdot!.toStringAsFixed(1)}',
                              'Units: ${user?.preferredUnits.name ?? 'metric'}',
                              if (user?.maxHr != null)
                                'HR max: ${user!.maxHr}',
                            ].join(' \u00b7 '),
                            style: const TextStyle(
                              fontSize: 14,
                              color: AppTheme.textSecondary,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const Icon(Icons.chevron_right, color: AppTheme.textSecondary),
                  ],
                ),
              ),
            ),
          ),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppTheme.spacingMd),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Fitness Over Time',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                  const SizedBox(height: AppTheme.spacingSm),
                  if (userProvider.currentVdot != null)
                    Text(
                      'Current: ${userProvider.currentVdot!.toStringAsFixed(1)}',
                      style: const TextStyle(
                        fontSize: 14,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  const SizedBox(height: AppTheme.spacingMd),
                  VdotChart(history: userProvider.vdotHistory),
                ],
              ),
            ),
          ),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppTheme.spacingMd),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Goal Races',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: AppTheme.textPrimary,
                        ),
                      ),
                      TextButton(
                        onPressed: widget.onNewGoalRace,
                        child: const Text('New Race'),
                      ),
                    ],
                  ),
                  ...planProvider.goalRaces.map(
                    (race) => Padding(
                      padding:
                          const EdgeInsets.only(bottom: AppTheme.spacingSm),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(race.distanceLabel),
                          Text(
                            race.status.name.toUpperCase(),
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppTheme.textSecondary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          Card(
            child: ListTile(
              title: const Text('Log out'),
              leading: const Icon(Icons.logout, color: AppTheme.error),
              onTap: () async {
                await context.read<AuthProvider>().logout();
                widget.onLogout?.call();
              },
            ),
          ),
          const SizedBox(height: AppTheme.spacingXl),
        ],
      ),
    );
  }
}
