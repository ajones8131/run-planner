import 'package:flutter/foundation.dart';
import '../models/user.dart';
import '../models/vdot.dart';
import '../services/user_service.dart';
import '../services/vdot_service.dart';

class UserProvider extends ChangeNotifier {
  final UserService _userService;
  final VdotService _vdotService;
  UserResponse? _user;
  List<VdotHistoryResponse> _vdotHistory = [];
  bool _loading = false;
  String? _error;

  UserProvider({required UserService userService, required VdotService vdotService})
      : _userService = userService,
        _vdotService = vdotService;

  UserResponse? get user => _user;
  List<VdotHistoryResponse> get vdotHistory => _vdotHistory;
  bool get loading => _loading;
  String? get error => _error;

  double? get currentVdot {
    final accepted = _vdotHistory.where((e) => e.accepted).toList();
    if (accepted.isEmpty) return null;
    accepted.sort((a, b) => a.calculatedAt.compareTo(b.calculatedAt));
    return accepted.last.newVdot;
  }

  List<VdotHistoryResponse> get flaggedEntries =>
      _vdotHistory.where((e) => e.flagged && !e.accepted).toList();

  Future<void> loadProfile() async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _user = await _userService.getProfile();
    } catch (e) {
      _error = 'Failed to load profile';
    }
    _loading = false;
    notifyListeners();
  }

  Future<void> updateProfile(UpdateProfileRequest request) async {
    try {
      _user = await _userService.updateProfile(request);
      notifyListeners();
    } catch (e) {
      _error = 'Failed to update profile';
      notifyListeners();
    }
  }

  Future<void> loadVdotHistory() async {
    try {
      _vdotHistory = await _vdotService.getHistory();
      notifyListeners();
    } catch (e) {
      _error = 'Failed to load VDOT history';
      notifyListeners();
    }
  }

  Future<void> acceptVdot(String id) async {
    await _vdotService.accept(id);
    await loadVdotHistory();
  }

  Future<void> dismissVdot(String id) async {
    await _vdotService.dismiss(id);
    await loadVdotHistory();
  }
}
