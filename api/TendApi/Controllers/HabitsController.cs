using System.Globalization;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using TendApi.Data;
using TendApi.Models;

namespace TendApi.Controllers;

[ApiController]
[Authorize]
[Route("api/habits")]
public class HabitsController : ControllerBase
{
    private const string DateFormat = "yyyy-MM-dd";
    private readonly TendDbContext _db;

    public HabitsController(TendDbContext db)
    {
        _db = db;
    }

    private int UserId => int.Parse(User.FindFirstValue("uid")!);

    private static bool TryParseDate(string? text, out DateOnly date) =>
        DateOnly.TryParseExact(text, DateFormat, CultureInfo.InvariantCulture, DateTimeStyles.None, out date);

    private static string Format(DateOnly date) => date.ToString(DateFormat, CultureInfo.InvariantCulture);

    // GET /api/habits?from=2026-10-05&to=2026-10-11&today=2026-10-10
    // Returns every habit with the days done inside [from, to] and the current streak.
    [HttpGet]
    public async Task<IActionResult> GetAll([FromQuery] string? from, [FromQuery] string? to, [FromQuery] string? today)
    {
        if (!TryParseDate(from, out var fromDate) || !TryParseDate(to, out var toDate) || !TryParseDate(today, out var todayDate))
            return BadRequest(new { message = "from, to and today must be dates like 2026-10-10." });

        var habits = await _db.Habits
            .AsNoTracking()
            .Where(h => h.UserId == UserId)
            .Include(h => h.CheckIns)
            .OrderBy(h => h.Id)
            .ToListAsync();

        var result = habits.Select(h =>
        {
            var done = h.CheckIns.Select(c => c.Date).ToHashSet();

            var inRange = done
                .Where(d => d >= fromDate && d <= toDate)
                .OrderBy(d => d)
                .Select(Format)
                .ToList();

            // Streak = consecutive done days ending today.
            // If today isn't done yet, the streak survives until the day is over, so count from yesterday.
            var day = done.Contains(todayDate) ? todayDate : todayDate.AddDays(-1);
            var streak = 0;
            while (done.Contains(day))
            {
                streak++;
                day = day.AddDays(-1);
            }

            return new HabitDto(h.Id, h.Name, h.Emoji, streak, inRange);
        }).ToList();

        return Ok(result);
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreateHabitRequest request)
    {
        var name = (request.Name ?? "").Trim();
        var emoji = string.IsNullOrWhiteSpace(request.Emoji) ? "🌱" : request.Emoji.Trim();

        if (name.Length == 0 || name.Length > 60)
            return BadRequest(new { message = "Give your habit a name (up to 60 characters)." });
        if (emoji.Length > 16)
            return BadRequest(new { message = "That emoji is too long." });

        var habit = new Habit { UserId = UserId, Name = name, Emoji = emoji };
        _db.Habits.Add(habit);
        await _db.SaveChangesAsync();

        return Ok(new HabitDto(habit.Id, habit.Name, habit.Emoji, 0, new List<string>()));
    }

    [HttpDelete("{id:int}")]
    public async Task<IActionResult> Delete(int id)
    {
        var habit = await _db.Habits.FirstOrDefaultAsync(h => h.Id == id && h.UserId == UserId);
        if (habit == null) return NotFound(new { message = "Habit not found." });

        _db.Habits.Remove(habit);
        await _db.SaveChangesAsync();
        return NoContent();
    }

    // POST /api/habits/5/toggle  { "date": "2026-10-10" }
    [HttpPost("{id:int}/toggle")]
    public async Task<IActionResult> Toggle(int id, [FromBody] ToggleRequest request)
    {
        if (!TryParseDate(request.Date, out var date))
            return BadRequest(new { message = "date must look like 2026-10-10." });

        // Can't tick off the future (one day of slack for time zones ahead of UTC)
        var latestAllowed = DateOnly.FromDateTime(DateTime.UtcNow).AddDays(1);
        if (date > latestAllowed)
            return BadRequest(new { message = "You can't check off a day that hasn't happened yet." });

        var habit = await _db.Habits.FirstOrDefaultAsync(h => h.Id == id && h.UserId == UserId);
        if (habit == null) return NotFound(new { message = "Habit not found." });

        var existing = await _db.CheckIns.FirstOrDefaultAsync(c => c.HabitId == id && c.Date == date);
        bool done;
        if (existing != null)
        {
            _db.CheckIns.Remove(existing);
            done = false;
        }
        else
        {
            _db.CheckIns.Add(new CheckIn { HabitId = id, Date = date });
            done = true;
        }

        await _db.SaveChangesAsync();
        return Ok(new ToggleResult(done));
    }
}
