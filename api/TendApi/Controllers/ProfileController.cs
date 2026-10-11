using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using TendApi.Data;
using TendApi.Models;

namespace TendApi.Controllers;

[ApiController]
[Authorize]
[Route("api/profile")]
public class ProfileController : ControllerBase
{
    private static readonly string[] WeekStarts = { "Monday", "Sunday" };
    private static readonly string[] Colours = { "terracotta", "green", "blue" };
    private const int MaxPhotoBytes = 2 * 1024 * 1024; // 2 MB

    private readonly TendDbContext _db;

    public ProfileController(TendDbContext db)
    {
        _db = db;
    }

    private int UserId => int.Parse(User.FindFirstValue("uid")!);

    public static ProfileDto ToDto(User u) =>
        new(u.Email, u.DisplayName, u.PhotoData != null, u.WeekStartsOn, u.Colour, u.DailyReminder);

    [HttpGet]
    public async Task<IActionResult> Get()
    {
        var user = await _db.Users.FindAsync(UserId);
        if (user == null) return Unauthorized(new { message = "Account not found." });
        return Ok(ToDto(user));
    }

    [HttpPut]
    public async Task<IActionResult> UpdateProfile([FromBody] UpdateProfileRequest request)
    {
        var name = (request.DisplayName ?? "").Trim();
        if (name.Length == 0 || name.Length > 60)
            return BadRequest(new { message = "Please enter a name (up to 60 characters)." });

        var user = await _db.Users.FindAsync(UserId);
        if (user == null) return Unauthorized(new { message = "Account not found." });

        user.DisplayName = name;
        await _db.SaveChangesAsync();
        return Ok(ToDto(user));
    }

    [HttpPut("settings")]
    public async Task<IActionResult> UpdateSettings([FromBody] UpdateSettingsRequest request)
    {
        if (request.WeekStartsOn == null || !WeekStarts.Contains(request.WeekStartsOn))
            return BadRequest(new { message = "Week must start on Monday or Sunday." });
        if (request.Colour == null || !Colours.Contains(request.Colour))
            return BadRequest(new { message = "Unknown colour." });

        var user = await _db.Users.FindAsync(UserId);
        if (user == null) return Unauthorized(new { message = "Account not found." });

        user.WeekStartsOn = request.WeekStartsOn;
        user.Colour = request.Colour;
        user.DailyReminder = request.DailyReminder;
        await _db.SaveChangesAsync();
        return Ok(ToDto(user));
    }

    [HttpPost("password")]
    public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordRequest request)
    {
        var current = request.CurrentPassword ?? "";
        var next = request.NewPassword ?? "";

        if (next.Length < 6)
            return BadRequest(new { message = "New password must be at least 6 characters." });

        var user = await _db.Users.FindAsync(UserId);
        if (user == null) return Unauthorized(new { message = "Account not found." });

        if (!BCrypt.Net.BCrypt.Verify(current, user.PasswordHash))
            return BadRequest(new { message = "Your current password is incorrect." });

        user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(next);
        await _db.SaveChangesAsync();
        return NoContent();
    }

    [HttpPost("photo")]
    public async Task<IActionResult> UploadPhoto(IFormFile file)
    {
        if (file == null || file.Length == 0)
            return BadRequest(new { message = "No photo was sent." });
        if (file.Length > MaxPhotoBytes)
            return BadRequest(new { message = "That photo is too large (2 MB max)." });
        if (file.ContentType != "image/jpeg" && file.ContentType != "image/png")
            return BadRequest(new { message = "Photos must be JPEG or PNG." });

        var user = await _db.Users.FindAsync(UserId);
        if (user == null) return Unauthorized(new { message = "Account not found." });

        using var ms = new MemoryStream();
        await file.CopyToAsync(ms);
        user.PhotoData = ms.ToArray();
        user.PhotoContentType = file.ContentType;
        await _db.SaveChangesAsync();
        return Ok(ToDto(user));
    }

    [HttpGet("photo")]
    public async Task<IActionResult> GetPhoto()
    {
        var user = await _db.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == UserId);
        if (user?.PhotoData == null) return NotFound();
        return File(user.PhotoData, user.PhotoContentType ?? "image/jpeg");
    }
}
