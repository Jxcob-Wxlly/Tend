using System.Net.Mail;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using TendApi.Data;
using TendApi.Models;
using TendApi.Services;

namespace TendApi.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly TendDbContext _db;
    private readonly TokenService _tokens;

    public AuthController(TendDbContext db, TokenService tokens)
    {
        _db = db;
        _tokens = tokens;
    }

    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequest request)
    {
        var email = (request.Email ?? "").Trim().ToLowerInvariant();
        var name = (request.DisplayName ?? "").Trim();
        var password = request.Password ?? "";

        if (!IsValidEmail(email))
            return BadRequest(new { message = "Please enter a valid email address." });
        if (name.Length == 0 || name.Length > 60)
            return BadRequest(new { message = "Please enter a name (up to 60 characters)." });
        if (password.Length < 6)
            return BadRequest(new { message = "Password must be at least 6 characters." });

        if (await _db.Users.AnyAsync(u => u.Email == email))
            return Conflict(new { message = "An account with that email already exists." });

        var user = new User
        {
            Email = email,
            DisplayName = name,
            // BCrypt salts and hashes the password; the plain password is never stored
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(password)
        };

        _db.Users.Add(user);
        await _db.SaveChangesAsync();

        return Ok(new AuthResponse(_tokens.Create(user), ProfileController.ToDto(user)));
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest request)
    {
        var email = (request.Email ?? "").Trim().ToLowerInvariant();
        var password = request.Password ?? "";

        var user = await _db.Users.FirstOrDefaultAsync(u => u.Email == email);

        if (user == null || !BCrypt.Net.BCrypt.Verify(password, user.PasswordHash))
            return Unauthorized(new { message = "Incorrect email or password." });

        return Ok(new AuthResponse(_tokens.Create(user), ProfileController.ToDto(user)));
    }

    private static bool IsValidEmail(string email)
    {
        if (email.Length == 0 || email.Length > 256) return false;
        try
        {
            var parsed = new MailAddress(email);
            return parsed.Address == email && email.Contains('.');
        }
        catch
        {
            return false;
        }
    }
}
