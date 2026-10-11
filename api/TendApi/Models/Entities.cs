namespace TendApi.Models;

public class User
{
    public int Id { get; set; }
    public string Email { get; set; } = string.Empty;
    public string PasswordHash { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;

    // Settings
    public string WeekStartsOn { get; set; } = "Monday";   // "Monday" or "Sunday"
    public string Colour { get; set; } = "terracotta";     // "terracotta", "green" or "blue"
    public bool DailyReminder { get; set; } = false;

    // Profile photo (stored in the database)
    public byte[]? PhotoData { get; set; }
    public string? PhotoContentType { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public List<Habit> Habits { get; set; } = new();
}

public class Habit
{
    public int Id { get; set; }
    public int UserId { get; set; }
    public User? User { get; set; }

    public string Name { get; set; } = string.Empty;
    public string Emoji { get; set; } = "🌱";
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public List<CheckIn> CheckIns { get; set; } = new();
}

public class CheckIn
{
    public int Id { get; set; }
    public int HabitId { get; set; }
    public Habit? Habit { get; set; }

    public DateOnly Date { get; set; }
}
