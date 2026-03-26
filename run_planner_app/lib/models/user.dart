enum Units {
  metric,
  imperial;

  static Units fromJson(String value) {
    return switch (value) {
      'METRIC' => Units.metric,
      'IMPERIAL' => Units.imperial,
      _ => throw ArgumentError('Unknown Units value: $value'),
    };
  }

  String toJson() => name.toUpperCase();
}

class UserResponse {
  final String id;
  final String email;
  final String? name;
  final DateTime? dateOfBirth;
  final int? maxHr;
  final Units preferredUnits;

  const UserResponse({
    required this.id,
    required this.email,
    this.name,
    this.dateOfBirth,
    this.maxHr,
    required this.preferredUnits,
  });

  factory UserResponse.fromJson(Map<String, dynamic> json) {
    return UserResponse(
      id: json['id'] as String,
      email: json['email'] as String,
      name: json['name'] as String?,
      dateOfBirth: json['dateOfBirth'] != null
          ? DateTime.parse(json['dateOfBirth'] as String)
          : null,
      maxHr: json['maxHr'] as int?,
      preferredUnits: Units.fromJson(json['preferredUnits'] as String),
    );
  }
}

class UpdateProfileRequest {
  final String? name;
  final DateTime? dateOfBirth;
  final int? maxHr;
  final Units? preferredUnits;

  const UpdateProfileRequest({
    this.name,
    this.dateOfBirth,
    this.maxHr,
    this.preferredUnits,
  });

  Map<String, dynamic> toJson() {
    return {
      if (name != null) 'name': name,
      if (dateOfBirth != null)
        'dateOfBirth':
            '${dateOfBirth!.year}-${dateOfBirth!.month.toString().padLeft(2, '0')}-${dateOfBirth!.day.toString().padLeft(2, '0')}',
      if (maxHr != null) 'maxHr': maxHr,
      if (preferredUnits != null) 'preferredUnits': preferredUnits!.toJson(),
    };
  }
}
