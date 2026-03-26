import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/user.dart';
import '../../providers/user_provider.dart';
import '../../theme/app_theme.dart';

class ProfileSetupScreen extends StatefulWidget {
  final VoidCallback onComplete;
  const ProfileSetupScreen({super.key, required this.onComplete});

  @override
  State<ProfileSetupScreen> createState() => _ProfileSetupScreenState();
}

class _ProfileSetupScreenState extends State<ProfileSetupScreen> {
  final _nameController = TextEditingController();
  final _maxHrController = TextEditingController();
  DateTime? _dateOfBirth;
  Units _units = Units.metric;
  bool _loading = false;

  @override
  void dispose() {
    _nameController.dispose();
    _maxHrController.dispose();
    super.dispose();
  }

  Future<void> _pickDateOfBirth() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: DateTime(1990, 1, 1),
      firstDate: DateTime(1940),
      lastDate: DateTime.now(),
    );
    if (picked != null) setState(() => _dateOfBirth = picked);
  }

  Future<void> _submit() async {
    setState(() => _loading = true);
    try {
      final maxHr =
          _maxHrController.text.isNotEmpty ? int.tryParse(_maxHrController.text) : null;
      await context.read<UserProvider>().updateProfile(
            UpdateProfileRequest(
              name: _nameController.text.trim().isNotEmpty
                  ? _nameController.text.trim()
                  : null,
              dateOfBirth: _dateOfBirth,
              maxHr: maxHr,
              preferredUnits: _units,
            ),
          );
      widget.onComplete();
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Set Up Profile')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppTheme.spacingLg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Tell us about yourself',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: AppTheme.spacingLg),
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(labelText: 'Name'),
                textCapitalization: TextCapitalization.words,
              ),
              const SizedBox(height: AppTheme.spacingMd),
              InkWell(
                onTap: _pickDateOfBirth,
                child: InputDecorator(
                  decoration: const InputDecoration(labelText: 'Date of Birth'),
                  child: Text(
                    _dateOfBirth != null
                        ? '${_dateOfBirth!.month}/${_dateOfBirth!.day}/${_dateOfBirth!.year}'
                        : 'Tap to select',
                    style: TextStyle(
                      color: _dateOfBirth != null
                          ? AppTheme.textPrimary
                          : AppTheme.textSecondary,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: AppTheme.spacingMd),
              TextFormField(
                controller: _maxHrController,
                decoration: const InputDecoration(
                  labelText: 'Max Heart Rate (optional)',
                  hintText: 'e.g. 185',
                ),
                keyboardType: TextInputType.number,
              ),
              const SizedBox(height: AppTheme.spacingMd),
              const Text(
                'Preferred Units',
                style: TextStyle(color: AppTheme.textSecondary),
              ),
              const SizedBox(height: AppTheme.spacingSm),
              SegmentedButton<Units>(
                segments: const [
                  ButtonSegment(value: Units.metric, label: Text('Kilometers')),
                  ButtonSegment(value: Units.imperial, label: Text('Miles')),
                ],
                selected: {_units},
                onSelectionChanged: (v) => setState(() => _units = v.first),
              ),
              const SizedBox(height: AppTheme.spacingXl),
              ElevatedButton(
                onPressed: _loading ? null : _submit,
                child: _loading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Continue'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
