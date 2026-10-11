namespace TendApi.Models;

// ---------- Requests ----------
public record RegisterRequest(string? Email, string? Password, string? DisplayName);
public record LoginRequest(string? Email, string? Password);
public record UpdateProfileRequest(string? DisplayName);
public record UpdateSettingsRequest(string? WeekStartsOn, string? Colour, bool DailyReminder);
public record ChangePasswordRequest(string? CurrentPassword, string? NewPassword);
public record CreateHabitRequest(string? Name, string? Emoji);
public record ToggleRequest(string? Date);

// ---------- Responses ----------
public record ProfileDto(
    string Email,
    string DisplayName,
    bool HasPhoto,
    string WeekStartsOn,
    string Colour,
    bool DailyReminder);

public record AuthResponse(string Token, ProfileDto Profile);

public record HabitDto(
    int Id,
    string Name,
    string Emoji,
    int Streak,
    List<string> DoneDates);

public record ToggleResult(bool Done);
