import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/goal_race.dart';
import '../../providers/plan_provider.dart';
import '../../providers/user_provider.dart';
import '../../theme/app_theme.dart';

class _RaceOption {
  final String label;
  final int distanceMeters;
  const _RaceOption(this.label, this.distanceMeters);
}

class GoalRaceWizardScreen extends StatefulWidget {
  final VoidCallback onComplete;
  const GoalRaceWizardScreen({super.key, required this.onComplete});

  @override
  State<GoalRaceWizardScreen> createState() => _GoalRaceWizardScreenState();
}

class _GoalRaceWizardScreenState extends State<GoalRaceWizardScreen> {
  int _step = 0;
  _RaceOption? _selectedDistance;
  DateTime? _raceDate;
  bool _loading = false;
  final _hoursController = TextEditingController();
  final _minutesController = TextEditingController();

  static const _distances = [
    _RaceOption('5K', 5000),
    _RaceOption('10K', 10000),
    _RaceOption('Half Marathon', 21097),
    _RaceOption('Marathon', 42195),
  ];

  @override
  void dispose() {
    _hoursController.dispose();
    _minutesController.dispose();
    super.dispose();
  }

  String? _error;

  Future<void> _generatePlan() async {
    if (_selectedDistance == null || _raceDate == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final planProvider = context.read<PlanProvider>();
      int? goalFinishSeconds;
      final hours = int.tryParse(_hoursController.text) ?? 0;
      final minutes = int.tryParse(_minutesController.text) ?? 0;
      if (hours > 0 || minutes > 0) goalFinishSeconds = (hours * 3600) + (minutes * 60);

      final race = await planProvider.createGoalRace(
        CreateGoalRaceRequest(
          distanceMeters: _selectedDistance!.distanceMeters,
          distanceLabel: _selectedDistance!.label,
          raceDate: _raceDate!,
          goalFinishSeconds: goalFinishSeconds,
        ),
      );
      await planProvider.createPlan(race.id);
    } catch (e) {
      debugPrint('PLAN GENERATION ERROR: $e');
      if (mounted) {
        setState(() {
          _error = 'Failed to generate plan. Make sure you have workout history so we can calculate your fitness level.';
        });
      }
      return;
    } finally {
      if (mounted) setState(() => _loading = false);
    }
    if (mounted) widget.onComplete();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Step ${_step + 1} of 4')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(AppTheme.spacingLg),
          child: switch (_step) {
            0 => _buildDistanceStep(),
            1 => _buildDateStep(),
            2 => _buildGoalTimeStep(),
            3 => _buildConfirmationStep(),
            _ => const SizedBox.shrink(),
          },
        ),
      ),
    );
  }

  Widget _buildDistanceStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'What are you training for?',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: AppTheme.textPrimary,
          ),
        ),
        const SizedBox(height: AppTheme.spacingLg),
        ..._distances.map(
          (d) => Padding(
            padding: const EdgeInsets.only(bottom: AppTheme.spacingSm),
            child: OutlinedButton(
              onPressed: () => setState(() {
                _selectedDistance = d;
                _step = 1;
              }),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.all(AppTheme.spacingMd),
                alignment: Alignment.centerLeft,
              ),
              child: Text(d.label, style: const TextStyle(fontSize: 18)),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildDateStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'When is your race?',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: AppTheme.textPrimary,
          ),
        ),
        const SizedBox(height: AppTheme.spacingLg),
        CalendarDatePicker(
          initialDate: DateTime.now().add(const Duration(days: 90)),
          firstDate: DateTime.now().add(const Duration(days: 14)),
          lastDate: DateTime.now().add(const Duration(days: 365)),
          onDateChanged: (date) => setState(() {
            _raceDate = date;
            _step = 2;
          }),
        ),
      ],
    );
  }

  Widget _buildGoalTimeStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          "What's your goal finish time?",
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: AppTheme.textPrimary,
          ),
        ),
        const SizedBox(height: AppTheme.spacingSm),
        const Text(
          "Optional — leave blank if you're not targeting a specific time",
          style: TextStyle(color: AppTheme.textSecondary),
        ),
        const SizedBox(height: AppTheme.spacingLg),
        Row(
          children: [
            Expanded(
              child: TextFormField(
                controller: _hoursController,
                decoration: const InputDecoration(labelText: 'Hours', hintText: '3'),
                keyboardType: TextInputType.number,
              ),
            ),
            const SizedBox(width: AppTheme.spacingMd),
            Expanded(
              child: TextFormField(
                controller: _minutesController,
                decoration: const InputDecoration(labelText: 'Minutes', hintText: '15'),
                keyboardType: TextInputType.number,
              ),
            ),
          ],
        ),
        const SizedBox(height: AppTheme.spacingXl),
        ElevatedButton(
          onPressed: () => setState(() => _step = 3),
          child: const Text('Continue'),
        ),
      ],
    );
  }

  Widget _buildConfirmationStep() {
    final userProvider = context.watch<UserProvider>();
    final vdot = userProvider.currentVdot;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Ready to generate your plan',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: AppTheme.textPrimary,
          ),
        ),
        const SizedBox(height: AppTheme.spacingLg),
        _buildSummaryRow('Race', _selectedDistance!.label),
        _buildSummaryRow(
          'Date',
          '${_raceDate!.month}/${_raceDate!.day}/${_raceDate!.year}',
        ),
        if (_hoursController.text.isNotEmpty || _minutesController.text.isNotEmpty)
          _buildSummaryRow('Goal', '${_hoursController.text}h ${_minutesController.text}m'),
        if (vdot != null) _buildSummaryRow('Current VDOT', vdot.toStringAsFixed(1)),
        if (_error != null) ...[
          const SizedBox(height: AppTheme.spacingMd),
          Text(_error!, style: const TextStyle(color: AppTheme.error), textAlign: TextAlign.center),
        ],
        const Spacer(),
        ElevatedButton(
          onPressed: _loading ? null : _generatePlan,
          child: _loading
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Generate Plan'),
        ),
      ],
    );
  }

  Widget _buildSummaryRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontSize: 16, color: AppTheme.textSecondary)),
          Text(
            value,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: AppTheme.textPrimary,
            ),
          ),
        ],
      ),
    );
  }
}
