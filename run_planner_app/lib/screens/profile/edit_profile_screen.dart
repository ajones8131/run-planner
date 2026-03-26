import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/user.dart';
import '../../providers/user_provider.dart';
import '../../theme/app_theme.dart';

class EditProfileScreen extends StatefulWidget {
  final VoidCallback? onSaved;
  const EditProfileScreen({super.key, this.onSaved});

  @override
  State<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen> {
  late TextEditingController _nameController;
  late TextEditingController _maxHrController;
  late Units _units;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    final user = context.read<UserProvider>().user;
    _nameController = TextEditingController(text: user?.name ?? '');
    _maxHrController =
        TextEditingController(text: user?.maxHr?.toString() ?? '');
    _units = user?.preferredUnits ?? Units.metric;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _maxHrController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    setState(() => _loading = true);
    try {
      await context.read<UserProvider>().updateProfile(
            UpdateProfileRequest(
              name: _nameController.text.trim(),
              maxHr: int.tryParse(_maxHrController.text),
              preferredUnits: _units,
            ),
          );
      widget.onSaved?.call();
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Edit Profile'),
        actions: [
          TextButton(
            onPressed: _loading ? null : _save,
            child: _loading
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Save'),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppTheme.spacingLg),
        child: Column(
          children: [
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(labelText: 'Name'),
            ),
            const SizedBox(height: AppTheme.spacingMd),
            TextFormField(
              controller: _maxHrController,
              decoration: const InputDecoration(labelText: 'Max Heart Rate'),
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
                ButtonSegment(
                  value: Units.metric,
                  label: Text('Kilometers'),
                ),
                ButtonSegment(
                  value: Units.imperial,
                  label: Text('Miles'),
                ),
              ],
              selected: {_units},
              onSelectionChanged: (v) => setState(() => _units = v.first),
            ),
          ],
        ),
      ),
    );
  }
}
